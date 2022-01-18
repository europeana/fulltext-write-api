package eu.europeana.fulltextwrite.util;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltextwrite.exception.FTWriteConversionException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.edm.FullTextResource;
import eu.europeana.fulltextwrite.model.edm.FulltextPackage;
import eu.europeana.fulltextwrite.model.edm.TextBoundary;
import eu.europeana.fulltextwrite.model.edm.TimeBoundary;
import eu.europeana.fulltextwrite.web.WebConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class EDMToFulltextConverter {

    private static final Logger logger = LogManager.getLogger(EDMToFulltextConverter.class);

    private EDMToFulltextConverter() {
        // private constructor to hide implicit one
    }

    /**
     * Converts FulltextPackage to AnnoPage class
     * @param datasetId
     * @param localId
     * @param request
     * @param fulltext
     * @return
     * @throws FTWriteConversionException
     */
    public static AnnoPage getAnnoPage(String datasetId, String localId, AnnotationPreview request, FulltextPackage fulltext) throws FTWriteConversionException {
        Resource resource = getResource(fulltext.getResource(), request, datasetId, localId);
        // TODO not sure how to calculate the pgID
        AnnoPage annoPage = new AnnoPage(datasetId, localId, "1", request.getMedia(), request.getLanguage(), resource);
        annoPage.setAns(getAnnotations(fulltext));
        // fail-safe check
        if (annoPage.getAns().size() != fulltext.size()) {
            throw new FTWriteConversionException("Mismatch in Annotations while converting from EDM to fulltext. " +
                    "Annotations obtained - "+ fulltext.size() + ". Annotations converted - " +annoPage.getAns().size());
        }
        logger.info("Successfully converted EDM to AnnoPage for record {}", request.getRecordId());
        return annoPage;
    }

    private static Resource getResource(FullTextResource ftResource, AnnotationPreview request, String datasetId, String localId ) {
       return new Resource(getFulltextResourceId(ftResource.getFullTextResourceURI(), request.getRecordId()),
                request.getLanguage(), ftResource.getValue(), request.getRights(), datasetId, localId);
    }

    private static List<Annotation> getAnnotations(FulltextPackage fulltext) {
        List<Annotation> annotationList = new ArrayList<>();
        ListIterator<eu.europeana.fulltextwrite.model.edm.Annotation> annotationListIterator = fulltext.listIterator();
        while(annotationListIterator.hasNext()){
            eu.europeana.fulltextwrite.model.edm.Annotation sourceAnnotation = annotationListIterator.next();
            TextBoundary boundary = (TextBoundary) sourceAnnotation.getTextReference();
            List<Target> targets = new ArrayList<>();
            if(sourceAnnotation.hasTargets()) {
                TimeBoundary tB = sourceAnnotation.getTargets().get(0);
                targets.add(new Target(tB.getStart(), tB.getEnd()));
            }
            // for media don't add default to, from values
            if(sourceAnnotation.getType().equals(AnnotationType.MEDIA)) {
                Annotation annotation = new Annotation();
                annotation.setAnId(sourceAnnotation.getAnnoId());
                annotation.setDcType(sourceAnnotation.getType().getAbbreviation());
                annotationList.add(annotation);
            } else {
                annotationList.add(new Annotation(sourceAnnotation.getAnnoId(), sourceAnnotation.getType().getAbbreviation(), boundary.getFrom(), boundary.getTo(), targets));
            }
        }
        return annotationList;
    }

    /**
     * Extracts fulltext Resource ID from the url.
     * url ex : http://data.europeana.eu/fulltext/456-test/data_euscreenXL_EUS_test/161d895530ccefd51e08611fde992c7e
     * @param fulltextResourceUri
     * @param itemID
     * @return
     */
    private static String getFulltextResourceId(String fulltextResourceUri, String itemID) {
        return StringUtils.substringAfter(fulltextResourceUri, WebConstants.BASE_FULLTEXT_URL + itemID + WebConstants.URL_SEPARATOR );
    }
}