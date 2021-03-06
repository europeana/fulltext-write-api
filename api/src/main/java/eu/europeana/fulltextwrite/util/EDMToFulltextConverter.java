package eu.europeana.fulltextwrite.util;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.entity.*;
import eu.europeana.fulltextwrite.exception.FTWriteConversionException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.edm.FullTextResource;
import eu.europeana.fulltextwrite.model.edm.FulltextPackage;
import eu.europeana.fulltextwrite.model.edm.TextBoundary;
import eu.europeana.fulltextwrite.model.edm.TimeBoundary;
import eu.europeana.fulltextwrite.web.WebConstants;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class EDMToFulltextConverter {

  private EDMToFulltextConverter() {
    // private constructor to hide implicit one
  }

  /**
   * Converts FulltextPackage to AnnoPage class
   *
   * @param datasetId
   * @param localId
   * @param request
   * @param fulltext
   * @return
   * @throws FTWriteConversionException
   */
  public static TranslationAnnoPage getAnnoPage(
      String datasetId, String localId, AnnotationPreview request, FulltextPackage fulltext)
      throws FTWriteConversionException {
    TranslationResource resource = getResource(fulltext.getResource(), request, datasetId, localId);
    TranslationAnnoPage annoPage = new TranslationAnnoPage();
    annoPage.setDsId(datasetId);
    annoPage.setLcId(localId);
    annoPage.setPgId(FulltextWriteUtils.derivePageId(request.getMedia()));
    annoPage.setTgtId(request.getMedia());
    annoPage.setLang(request.getLanguage());
    // set the source if present
    if (!StringUtils.isEmpty(request.getSource())) {
      annoPage.setSource(request.getSource());
    }
    annoPage.setRes(resource);
    annoPage.setAns(getAnnotations(fulltext));
    annoPage.setSource(request.getSource());
    // fail-safe check
    if (annoPage.getAns().size() != fulltext.size()) {
      throw new FTWriteConversionException(
          "Mismatch in Annotations while converting from EDM to fulltext. "
              + "Annotations obtained - "
              + fulltext.size()
              + ". Annotations converted - "
              + annoPage.getAns().size());
    }
    return annoPage;
  }

  private static TranslationResource getResource(
      FullTextResource ftResource, AnnotationPreview request, String datasetId, String localId) {
    TranslationResource resource = new TranslationResource();
    resource.setId(
        getFulltextResourceId(ftResource.getFullTextResourceURI(), request.getRecordId()));
    resource.setLang(request.getLanguage());
    resource.setValue(ftResource.getValue());
    resource.setRights(request.getRights());
    resource.setDsId(datasetId);
    resource.setLcId(localId);
    return resource;
  }

  private static List<Annotation> getAnnotations(FulltextPackage fulltext) {
    List<Annotation> annotationList = new ArrayList<>();
    for (eu.europeana.fulltextwrite.model.edm.Annotation sourceAnnotation : fulltext) {
      TextBoundary boundary = (TextBoundary) sourceAnnotation.getTextReference();
      List<Target> targets = new ArrayList<>();
      if (sourceAnnotation.hasTargets()) {
        TimeBoundary tB = sourceAnnotation.getTargets().get(0);
        targets.add(new Target(tB.getStart(), tB.getEnd()));
      }
      // for media don't add default to, from values
      if (sourceAnnotation.getType().equals(AnnotationType.MEDIA)) {
        Annotation annotation = new Annotation();
        annotation.setAnId(sourceAnnotation.getAnnoId());
        annotation.setDcType(sourceAnnotation.getType().getAbbreviation());
        annotationList.add(annotation);
      } else {
        annotationList.add(
            new Annotation(
                sourceAnnotation.getAnnoId(),
                sourceAnnotation.getType().getAbbreviation(),
                boundary.getFrom(),
                boundary.getTo(),
                targets));
      }
    }
    return annotationList;
  }

  /**
   * Extracts fulltext Resource ID from the url. url ex :
   * http://data.europeana.eu/fulltext/456-test/data_euscreenXL_EUS_test/161d895530ccefd51e08611fde992c7e
   *
   * @param fulltextResourceUri
   * @param itemID
   * @return
   */
  private static String getFulltextResourceId(String fulltextResourceUri, String itemID) {
    return StringUtils.substringAfter(
        fulltextResourceUri, WebConstants.BASE_FULLTEXT_URL + itemID + "/");
  }
}
