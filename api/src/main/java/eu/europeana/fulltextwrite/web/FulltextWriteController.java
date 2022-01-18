package eu.europeana.fulltextwrite.web;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.commons.web.exception.ApplicationAuthenticationException;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltextwrite.config.AppSettings;
import eu.europeana.fulltextwrite.exception.AnnoPageExistException;
import eu.europeana.fulltextwrite.exception.MediaTypeNotSupportedException;
import eu.europeana.fulltextwrite.model.AnnotationChangeType;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.SubtitleType;
import eu.europeana.fulltextwrite.model.edm.FulltextPackage;
import eu.europeana.fulltextwrite.repository.AnnotationRepository;
import eu.europeana.fulltextwrite.serializer.JsonLdSerializer;
import eu.europeana.fulltextwrite.service.SubtitleHandler;
import eu.europeana.fulltextwrite.service.SubtitleToEDMHandler;
import eu.europeana.fulltextwrite.util.EDMToFulltextConverter;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import io.swagger.annotations.ApiOperation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/presentation")
public class FulltextWriteController extends BaseRest {

  private final AppSettings appSettings;
  private final AnnotationRepository annotationRepository;

  public FulltextWriteController(
      AppSettings appSettings, AnnotationRepository annotationRepository) {
    this.appSettings = appSettings;
    this.annotationRepository = annotationRepository;
  }

  @ApiOperation(
      value = "Submits a new fulltext document for a given Europeana ID (dataset + localID)")
  @PostMapping(
      value = "/{datasetId}/{localId}/annopage",
      produces = {HttpHeaders.CONTENT_TYPE_JSONLD, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<?> submitNewFulltext(
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
    return addNewFulltext(
        datasetId, localId, media, lang, originalLang, rights, content, request);
  }

  private ResponseEntity<?> addNewFulltext(
      String datasetId,
      String localId,
      String media,
      String lang,
      boolean originalLang,
      String rights,
      String content,
      HttpServletRequest request)
      throws EuropeanaApiException, IOException {

    // Check if there is a fulltext annotation page associated with the combination of DATASET_ID,
    // LOCAL_ID and the media URL, if so then return a HTTP 301 with the URL of the Annotation Page;
    AnnoPage annoPage = annotationRepository.getAnnoPageByTargetId(datasetId, localId, media);
    if (annoPage != null) {
      throw new AnnoPageExistException(
          "Annotation page already exists -"
              + FulltextWriteUtils.getAnnoPageUrl(appSettings.getFulltextApiUrl(), annoPage));
    }

    if (!SubtitleType.isSupported(request.getContentType())) {
      throw new MediaTypeNotSupportedException(
          "The content type " + request.getContentType() + " is not supported");
    }

    SubtitleType type = SubtitleType.getValueByMimetype(request.getContentType());
    AnnotationPreview annotationPreview =
        new AnnotationPreview(
            FulltextWriteUtils.generateRecordId(datasetId, localId),
            media,
            lang,
            rights,
            originalLang,
            type,
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
            AnnotationChangeType.NEWLY_CREATED);

    // Select a Text2EDM handler that matches the indicated Content-Type and
    // apply it to the binary to convert into an EDM based object model, if not successful return HTTP 400;
    FulltextPackage fulltext = new SubtitleToEDMHandler().subtitleToEDM(annotationPreview);
    // Conversion for testing
    AnnoPage annoPage1 = EDMToFulltextConverter.getAnnoPage(datasetId, localId, annotationPreview, fulltext);

    //TODO will save a proper record later as a part of EA-2827
    // Keep in mind to store Resource as well and based on originallanguege - AnnoPage or TranslationAnnoPage
//    AnnoPage convertedAnnoPage =
//        FulltextWriteUtils.createDummyAnnotation(datasetId, localId, media, rights, lang);
     AnnoPage saved = annotationRepository.saveAnnoPage(annoPage1);
     String jsonLd = new JsonLdSerializer().serialize(saved);
     return generateResponse(request, jsonLd, HttpStatus.OK);
   // return generateResponse(request, "done", HttpStatus.OK);
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
