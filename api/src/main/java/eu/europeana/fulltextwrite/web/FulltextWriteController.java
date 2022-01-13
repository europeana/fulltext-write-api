package eu.europeana.fulltextwrite.web;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.commons.web.exception.ApplicationAuthenticationException;
import eu.europeana.api.commons.web.http.HttpHeaders;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltextwrite.config.AppSettings;
import eu.europeana.fulltextwrite.exception.AnnoPageExistException;
import eu.europeana.fulltextwrite.exception.MediaTypeNotSupportedException;
import eu.europeana.fulltextwrite.repository.AnnotationRepository;
import eu.europeana.fulltextwrite.serializer.JsonLdSerializer;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
      @RequestPart(value = WebConstants.REQUEST_VALUE_DOC) MultipartFile file,
      HttpServletRequest request)
      throws ApplicationAuthenticationException, EuropeanaApiException, IOException {

    if (appSettings.isAuthEnabled()) {
      verifyWriteAccess(Operations.CREATE, request);
    }
    return submitNewFulltext(datasetId, localId, media, lang, originalLang, rights, request);
  }

  private ResponseEntity<?> submitNewFulltext(
      String datasetId,
      String localId,
      String media,
      String lang,
      boolean originalLang,
      String rights,
      HttpServletRequest request)
      throws EuropeanaApiException, IOException {

    // Check if there is a fulltext annotation page associated with the combination of DATASET_ID,
    // LOCAL_ID and the media URL, if so then return a HTTP 301 with the URL of the Annotation Page;
    AnnoPage annoPage = annotationRepository.getAnnoPageByTargetId(datasetId, localId, media);
    if (annoPage != null) {
      throw new AnnoPageExistException(
          "Annotation page already exists -"
              + getFulltextWriteUtils().getAnnoPageUrl(appSettings.getFulltextApiUrl(), annoPage));
    }

    // TODO this is still unclear to me the supported content or mime type, how do we handle it
    // for the textToEDMHandler hence for now a general check
    if (!StringUtils.contains(request.getContentType(), "multipart/form-data")) {
      throw new MediaTypeNotSupportedException(
          "The content type " + request.getContentType() + " is not supported");
    }
    // TODO - these further steps will be done once we have the Handler code

    // Select a Text2EDM handler that matches the indicated Content-Type and
    // apply it to the binary to convert into an EDM based object model, if not successful return
    // HTTP 400;
    // Assign identifiers to all assets produced by the handler (ie. Annotation Pages, Annotations,
    // Fulltext WebResource)

    // TODO will save a proper record - for now storing dummy record
    AnnoPage convertedAnnoPage =
        getFulltextWriteUtils().createDummyAnnotation(datasetId, localId, media, rights, lang);
    AnnoPage saved = annotationRepository.saveAnnoPage(convertedAnnoPage);
    String jsonLd = new JsonLdSerializer().serialize(saved);
    return generateResponse(request, jsonLd, HttpStatus.OK);
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
