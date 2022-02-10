package eu.europeana.fulltextwrite.web;

import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.isValidAnnotationId;
import static eu.europeana.fulltextwrite.web.WebConstants.MOTIVATION_SUBTITLING;
import static eu.europeana.fulltextwrite.web.WebConstants.REQUEST_VALUE_SOURCE;

import com.dotsub.converter.model.SubtitleItem;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.commons.web.exception.ApplicationAuthenticationException;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltextwrite.config.AppSettings;
import eu.europeana.fulltextwrite.exception.*;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.AnnotationPreview.Builder;
import eu.europeana.fulltextwrite.model.DeleteAnnoSyncResponse;
import eu.europeana.fulltextwrite.model.DeleteAnnoSyncResponse.Status;
import eu.europeana.fulltextwrite.model.SubtitleType;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.service.AnnotationsApiRestService;
import eu.europeana.fulltextwrite.service.FTWriteService;
import eu.europeana.fulltextwrite.service.SubtitleHandlerService;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import io.swagger.annotations.ApiOperation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@Validated
public class FulltextWriteController extends BaseRest {

  private final AppSettings appSettings;
  private final SubtitleHandlerService subtitleHandlerService;
  private final AnnotationsApiRestService annotationsApiRestService;

  private final FTWriteService ftWriteService;

  private final Predicate<String> annotationIdPattern;

  public FulltextWriteController(
      AppSettings appSettings,
      SubtitleHandlerService subtitleHandlerService,
      AnnotationsApiRestService annotationsApiRestService,
      FTWriteService ftWriteService) {
    this.appSettings = appSettings;
    this.subtitleHandlerService = subtitleHandlerService;
    this.annotationsApiRestService = annotationsApiRestService;
    this.ftWriteService = ftWriteService;
    annotationIdPattern =
        Pattern.compile(
                String.format(
                    FulltextWriteUtils.ANNOTATION_ID_REGEX,
                    appSettings.getAnnotationIdHostsPattern()))
            .asMatchPredicate();
  }

  @ApiOperation(value = "Propagate and synchronise with Annotations API")
  @PostMapping(
      value = "/fulltext/annosync",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> syncAnnotations(
      @RequestParam(value = REQUEST_VALUE_SOURCE) String source, HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException, IOException {
    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.UPDATE, request);
    }

    // check that sourceUrl is valid, and points to a europeana.eu domain
    if (!isValidAnnotationId(source, annotationIdPattern)) {
      throw new InvalidUriException(
          String.format(
              "'%s' request parameter must be a valid annotation id", REQUEST_VALUE_SOURCE));
    }

    Optional<AnnotationItem> itemOptional = annotationsApiRestService.retrieveAnnotation(source);
    if (itemOptional.isEmpty()) {
      // annotationItem not present, meaning 410 returned by Annotation API - so it has been deleted

      TranslationAnnoPage annoPage = ftWriteService.getShellAnnoPageBySource(source);
      long count = ftWriteService.deleteAnnoPagesWithSource(source);

      DeleteAnnoSyncResponse response =
          new DeleteAnnoSyncResponse(
              source, count > 0 ? Status.DELETED.getValue() : Status.NOOP.getValue(), annoPage);

      return generateResponse(request, serializeResponse(response), HttpStatus.ACCEPTED);
    }

    AnnotationItem item = itemOptional.get();
    // motivation must be subtitling

    if (!MOTIVATION_SUBTITLING.equals(item.getMotivation())) {
      throw new UnsupportedAnnotationException(
          String.format(
              "Annotation motivation '%s' not supported for sync. Only subtitles are supported",
              item.getMotivation()));
    }

    AnnotationPreview annotationPreview =
        ftWriteService.createAnnotationPreview(itemOptional.get());
    TranslationAnnoPage annoPage = ftWriteService.createAnnoPage(annotationPreview);

    // Morphia creates a new _id value if none exists, so we can't directly call save() – as this
    // could be an update.
    ftWriteService.upsertAnnoPage(List.of(annoPage));

    return generateResponse(request, serializeJsonLd(annoPage), HttpStatus.ACCEPTED);
  }

  @ApiOperation(
      value = "Submits a new fulltext document for a given Europeana ID (dataset + localID)")
  @PostMapping(
      value = "/presentation/{datasetId}/{localId}/annopage",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> submitNewFulltext(
      @PathVariable(value = WebConstants.REQUEST_VALUE_DATASET_ID) String datasetId,
      @PathVariable(value = WebConstants.REQUEST_VALUE_LOCAL_ID) String localId,
      @RequestParam(value = WebConstants.REQUEST_VALUE_MEDIA) String media,
      @RequestParam(value = WebConstants.REQUEST_VALUE_LANG) String lang,
      @RequestParam(
              value = WebConstants.REQUEST_VALUE_ORIGINAL_LANG,
              required = false,
              defaultValue = "false")
          boolean originalLang,
      @RequestParam(value = WebConstants.REQUEST_VALUE_RIGHTS) String rights,
      @RequestParam(value = WebConstants.REQUEST_VALUE_SOURCE, required = false) String source,
      @RequestBody String content,
      HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException, IOException,
          URISyntaxException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.CREATE, request);
    }
    return addNewFulltext(
        datasetId, localId, media, lang, originalLang, rights, source, content, request);
  }

  private ResponseEntity<String> addNewFulltext(
      String datasetId,
      String localId,
      String media,
      String lang,
      boolean originalLang,
      String rights,
      String source,
      String content,
      HttpServletRequest request)
      throws EuropeanaApiException, IOException, URISyntaxException {
    /*
     * Check if there is a fulltext annotation page associated with the combination of DATASET_ID,
     * LOCAL_ID and the media URL, if so then return a HTTP 301 with the URL of the Annotation Page
     */
    if (ftWriteService.annoPageExistsByTgtId(datasetId, localId, media, lang)) {
      // return 301 redirect
      return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
          .location(
              UriComponentsBuilder.newInstance()
                  .uri(new URI(appSettings.getFulltextApiUrl()))
                  .path(
                      "/presentation/"
                          + datasetId
                          + "/"
                          + localId
                          + "/annopage/"
                          + FulltextWriteUtils.derivePageId(media))
                  .query(WebConstants.REQUEST_VALUE_LANG + "=" + lang)
                  .build()
                  .toUri())
          .build();
    }

    SubtitleType type = SubtitleType.getValueByMimetype(request.getContentType());
    if (type == null) {
      throw new MediaTypeNotSupportedException(
          "The content type " + request.getContentType() + " is not supported");
    }
    AnnotationPreview annotationPreview =
        createAnnotationPreview(
            datasetId, localId, lang, originalLang, rights, source, media, content, type);
    AnnoPage savedAnnoPage = ftWriteService.createAndSaveAnnoPage(annotationPreview);
    return generateResponse(request, serializeJsonLd(savedAnnoPage), HttpStatus.OK);
  }

  @ApiOperation(value = "Replaces existing fulltext for a media resource with a new document")
  @PutMapping(
      value = "/presentation/{datasetId}/{localId}/annopage/{pageId}",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> replaceFullText(
      @PathVariable(value = WebConstants.REQUEST_VALUE_DATASET_ID) String datasetId,
      @PathVariable(value = WebConstants.REQUEST_VALUE_LOCAL_ID) String localId,
      @PathVariable(value = WebConstants.REQUEST_VALUE_PAGE_ID) String pageId,
      @RequestParam(value = WebConstants.REQUEST_VALUE_LANG) String lang,
      @RequestParam(
              value = WebConstants.REQUEST_VALUE_ORIGINAL_LANG,
              required = false,
              defaultValue = "false")
          boolean originalLang,
      @RequestParam(value = WebConstants.REQUEST_VALUE_RIGHTS) String rights,
      @RequestParam(value = WebConstants.REQUEST_VALUE_SOURCE, required = false) String source,
      @RequestBody(required = false) String content,
      HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException, IOException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.UPDATE, request);
    }
    return replaceExistingFullText(
        datasetId, localId, pageId, lang, originalLang, rights, source, content, request);
  }

  private ResponseEntity<String> replaceExistingFullText(
      String datasetId,
      String localId,
      String pgId,
      String lang,
      boolean originalLang,
      String rights,
      String source,
      String content,
      HttpServletRequest request)
      throws AnnoPageDoesNotExistException, MediaTypeNotSupportedException, IOException,
          InvalidFormatException, FTWriteConversionException, SubtitleParsingException {
    /*
     * Check if there is a fulltext annotation page associated with the combination of DATASET_ID,
     * LOCAL_ID and the PAGE_ID and LANG, if not then return a HTTP 404
     */
    TranslationAnnoPage annoPage = ftWriteService.getAnnoPageByPgId(datasetId, localId, pgId, lang);

    if (annoPage == null) {
      throw new AnnoPageDoesNotExistException(
          "Annotation page does not exits for "
              + appSettings.getFulltextApiUrl()
              + FulltextWriteUtils.getTranslationAnnoPageUrl(
                  new AnnoPage(datasetId, localId, pgId, null, lang, null)));
    }
    // determine type
    SubtitleType type = null;
    if (!StringUtils.isEmpty(content)) {
      type = SubtitleType.getValueByMimetype(request.getContentType());
      if (type == null) {
        throw new MediaTypeNotSupportedException(
            "The content type " + request.getContentType() + " is not supported");
      }
    }
    AnnotationPreview annotationPreview =
        createAnnotationPreview(
            datasetId,
            localId,
            lang,
            originalLang,
            rights,
            source,
            annoPage.getTgtId(),
            content,
            type);
    TranslationAnnoPage updatedAnnoPage =
        ftWriteService.updateAnnoPage(annotationPreview, annoPage);
    return generateResponse(request, serializeJsonLd(updatedAnnoPage), HttpStatus.OK);
  }

  @ApiOperation(value = "Deletes the full-text associated to a media resource\n")
  @DeleteMapping(
      value = "/presentation/{datasetId}/{localId}/annopage/{pageId}",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> deleteFulltext(
      @PathVariable(value = WebConstants.REQUEST_VALUE_DATASET_ID) String datasetId,
      @PathVariable(value = WebConstants.REQUEST_VALUE_LOCAL_ID) String localId,
      @PathVariable(value = WebConstants.REQUEST_VALUE_PAGE_ID) String pageId,
      @RequestParam(value = WebConstants.REQUEST_VALUE_LANG, required = false) String lang,
      HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.DELETE, request);
    }
    return deleteAnnoPage(datasetId, localId, pageId, lang, request);
  }

  private ResponseEntity<String> deleteAnnoPage(
      String datasetId, String localId, String pageId, String lang, HttpServletRequest request)
      throws AnnoPageDoesNotExistException {
    /*
     * Check if there is a fulltext annotation page associated with the combination of DATASET_ID,
     * LOCAL_ID and the PAGE_ID and LANG (if provided), if not then return a HTTP 404
     */
    if (!ftWriteService.annoPageExistsByPgId(datasetId, localId, pageId, lang)) {
      throw new AnnoPageDoesNotExistException(
          "Annotation page does not exits for "
              + appSettings.getFulltextApiUrl()
              + FulltextWriteUtils.getTranslationAnnoPageUrl(
                  new AnnoPage(datasetId, localId, pageId, null, lang, null)));
    }

    /*
     * Delete the respective AnnotationPage(s) entry from MongoDB (if lang is omitted, the pages for
     * all languages will be deleted);
     */
    if (StringUtils.isNotEmpty(lang)) {
      ftWriteService.deleteAnnoPages(datasetId, localId, pageId, lang);
    } else {
      ftWriteService.deleteAnnoPages(datasetId, localId, pageId);
    }
    return noContentResponse(request);
  }

  // Creates Annotation preview object along with subtitles Items
  private AnnotationPreview createAnnotationPreview(
      String datasetId,
      String localId,
      String lang,
      boolean originalLang,
      String rights,
      String source,
      String media,
      String content,
      SubtitleType type)
      throws InvalidFormatException, SubtitleParsingException {
    // process subtitles if content is not empty
    List<SubtitleItem> subtitleItems = new ArrayList<>();
    if (!StringUtils.isEmpty(content)) {
      subtitleItems =
          subtitleHandlerService.parseSubtitle(
              new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), type);
    }
    String recordId = FulltextWriteUtils.generateRecordId(datasetId, localId);
    return new Builder(recordId, type, subtitleItems)
        .setOriginalLang(originalLang)
        .setLanguage(lang)
        .setRights(rights)
        .setMedia(media)
        .setSource(source)
        .build();
  }
}
