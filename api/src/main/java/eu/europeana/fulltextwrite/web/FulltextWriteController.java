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
import eu.europeana.fulltextwrite.exception.InvalidUriException;
import eu.europeana.fulltextwrite.exception.MediaTypeNotSupportedException;
import eu.europeana.fulltextwrite.exception.UnsupportedAnnotationException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.AnnotationPreview.Builder;
import eu.europeana.fulltextwrite.model.DeleteAnnoSyncResponse;
import eu.europeana.fulltextwrite.model.DeleteAnnoSyncResponse.Status;
import eu.europeana.fulltextwrite.model.SubtitleType;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.service.AnnotationService;
import eu.europeana.fulltextwrite.service.AnnotationsApiRestService;
import eu.europeana.fulltextwrite.service.SubtitleHandlerService;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import io.swagger.annotations.ApiOperation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
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

  private final AnnotationService annotationService;

  private final Predicate<String> annotationIdPattern;

  public FulltextWriteController(
      AppSettings appSettings,
      SubtitleHandlerService subtitleHandlerService,
      AnnotationsApiRestService annotationsApiRestService,
      AnnotationService annotationService) {
    this.appSettings = appSettings;
    this.subtitleHandlerService = subtitleHandlerService;
    this.annotationsApiRestService = annotationsApiRestService;
    this.annotationService = annotationService;
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

      TranslationAnnoPage annoPage = annotationService.getShellAnnoPageBySource(source);
      long count = annotationService.deleteAnnoPagesWithSources(Collections.singletonList(source));

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
        annotationService.createAnnotationPreview(itemOptional.get());
    TranslationAnnoPage annoPage = annotationService.createAnnoPage(annotationPreview);

    // Morphia creates a new _id value if none exists, so we can't directly call save() – as this
    // could be an update.
    annotationService.upsertAnnoPage(List.of(annoPage));

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
      @RequestBody String content,
      HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException, IOException,
          URISyntaxException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.CREATE, request);
    }

    /*
     * Check if there is a fulltext annotation page associated with the combination of DATASET_ID,
     * LOCAL_ID and the media URL, if so then return a HTTP 301 with the URL of the Annotation Page
     */
    if (annotationService.annoPageExists(datasetId, localId, media, lang)) {
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
                  .build()
                  .toUri())
          .build();
    }

    SubtitleType type = SubtitleType.getValueByMimetype(request.getContentType());

    if (type == null) {
      throw new MediaTypeNotSupportedException(
          "The content type " + request.getContentType() + " is not supported");
    }

    List<SubtitleItem> subtitleItems =
        subtitleHandlerService.parseSubtitle(
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), type);

    String recordId = FulltextWriteUtils.generateRecordId(datasetId, localId);
    AnnotationPreview annotationPreview =
        new Builder(recordId, type, subtitleItems)
            .setOriginalLang(originalLang)
            .setLanguage(lang)
            .setRights(rights)
            .setMedia(media)
            .build();

    AnnoPage savedAnnoPage = annotationService.createAndSaveAnnoPage(annotationPreview);
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
      HttpServletRequest request)
      throws ApplicationAuthenticationException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.UPDATE, request);
    }
    return generateResponse(request, "", HttpStatus.OK);
  }

  @ApiOperation(value = "Deletes the full-text associated to a media resource\n")
  @DeleteMapping(
      value = "/presentation/{datasetId}/{localId}/annopage/{annoId}",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<String> deleteFulltext(
      @PathVariable(value = WebConstants.REQUEST_VALUE_DATASET_ID) String datasetId,
      @PathVariable(value = WebConstants.REQUEST_VALUE_LOCAL_ID) String localId,
      @PathVariable(value = WebConstants.REQUEST_VALUE_ANNO_ID) String annoId,
      HttpServletRequest request)
      throws ApplicationAuthenticationException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.DELETE, request);
    }
    return noContentResponse(request);
  }
}
