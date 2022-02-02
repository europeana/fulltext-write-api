package eu.europeana.fulltextwrite.util;

import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.generateHash;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EDMToFulltextConverter {

  private static final Logger logger = LogManager.getLogger(EDMToFulltextConverter.class);

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
    Resource resource = getResource(fulltext.getResource(), request, datasetId, localId);

    TranslationAnnoPage annoPage = new TranslationAnnoPage();
    annoPage.setDsId(datasetId);
    annoPage.setLcId(localId);
    // truncate md5 hash to reduce URL length
    annoPage.setPgId(generateHash(request.getMedia()).substring(0, 5));
    annoPage.setTgtId(request.getMedia());
    annoPage.setLang(request.getLanguage());
    annoPage.setRes(resource);
    annoPage.setAns(getAnnotations(fulltext));
    // fail-safe check
    if (annoPage.getAns().size() != fulltext.size()) {
      throw new FTWriteConversionException(
          "Mismatch in Annotations while converting from EDM to fulltext. "
              + "Annotations obtained - "
              + fulltext.size()
              + ". Annotations converted - "
              + annoPage.getAns().size());
    }
    logger.info("Successfully converted EDM to AnnoPage for record {}", request.getRecordId());
    return annoPage;
  }

  private static Resource getResource(
      FullTextResource ftResource, AnnotationPreview request, String datasetId, String localId) {
    return new Resource(
        getFulltextResourceId(ftResource.getFullTextResourceURI(), request.getRecordId()),
        request.getLanguage(),
        ftResource.getValue(),
        request.getRights(),
        datasetId,
        localId);
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
