package eu.europeana.fulltextwrite.web;

import com.dotsub.converter.model.SubtitleItem;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.commons.web.exception.ApplicationAuthenticationException;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltextwrite.config.AppSettings;
import eu.europeana.fulltextwrite.exception.MediaTypeNotSupportedException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.AnnotationPreview.Builder;
import eu.europeana.fulltextwrite.model.SubtitleType;
import eu.europeana.fulltextwrite.service.AnnotationService;
import eu.europeana.fulltextwrite.service.SubtitleHandlerService;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import io.swagger.annotations.ApiOperation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@Validated
@RequestMapping("/presentation")
public class FulltextWriteController extends BaseRest {

  private final AppSettings appSettings;
  private final SubtitleHandlerService subtitleHandlerService;

  private final AnnotationService annotationService;

  public FulltextWriteController(
      AppSettings appSettings,
      SubtitleHandlerService subtitleHandlerService,
      AnnotationService annotationService) {
    this.appSettings = appSettings;
    this.subtitleHandlerService = subtitleHandlerService;
    this.annotationService = annotationService;
  }

  @ApiOperation(
      value = "Submits a new fulltext document for a given Europeana ID (dataset + localID)")
  @PostMapping(
      value = "/{datasetId}/{localId}/annopage",
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
      throws ApplicationAuthenticationException, EuropeanaApiException, IOException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.CREATE, request);
    }
    return addNewFulltext(datasetId, localId, media, lang, originalLang, rights, content, request);
  }

  private ResponseEntity<String> addNewFulltext(
      String datasetId,
      String localId,
      String media,
      String lang,
      boolean originalLang,
      String rights,
      String content,
      HttpServletRequest request)
      throws EuropeanaApiException, IOException {

    /*
     * Check if there is a fulltext annotation page associated with the combination of DATASET_ID,
     * LOCAL_ID and the media URL, if so then return a HTTP 301 with the URL of the Annotation Page
     */
    AnnoPage annoPage = annotationService.getAnnoPageByTargetId(datasetId, localId, media);
    if (annoPage != null) {
      // return 301 redirect
      return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
          .location(
              UriComponentsBuilder.newInstance()
                  .host(appSettings.getFulltextApiUrl())
                  .path(FulltextWriteUtils.getAnnoPageUrl(annoPage))
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
      value = "/{datasetId}/{localId}/annopage/{pageId}",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<?> replaceFullText(
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
      value = "/{datasetId}/{localId}/annopage/{annoId}",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<?> deleteFulltext(
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
