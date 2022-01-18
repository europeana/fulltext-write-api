package eu.europeana.fulltextwrite.service;

import com.dotsub.converter.model.SubtitleItem;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltextwrite.exception.FTWriteInstantiationException;
import eu.europeana.fulltextwrite.exception.InvalidFormatException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.edm.*;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import eu.europeana.fulltextwrite.web.WebConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class SubtitleToEDMHandler {

    private static final Logger logger = LogManager.getLogger(SubtitleToEDMHandler.class);
    private static Pattern PATTERN = Pattern.compile("[<][/]?[^<]+[/]?[>]");

    /**
     * Converts the Annotation preview request object to EDM Fulltext
     *
     * @param annotationPreview
     * @return
     * @throws FTWriteInstantiationException
     * @throws IOException
     * @throws InvalidFormatException
     */
    public FulltextPackage subtitleToEDM(AnnotationPreview annotationPreview)
            throws FTWriteInstantiationException, IOException, InvalidFormatException {
        List<SubtitleItem> subtitles = new SubtitleHandler().parseSubtitle(annotationPreview.getSubtitle(), annotationPreview.getSubtitleType());
        logger.info("{} subtitle processed for record {}", subtitles.size(), annotationPreview.getRecordId());
        annotationPreview.setSubtitleItems(subtitles);
        return convert(annotationPreview);
    }

    private FulltextPackage convert(AnnotationPreview preview) {
        String uri = WebConstants.BASE_ITEM_URL + preview.getRecordId();
        String annotationPageURI = FulltextWriteUtils.getAnnotationPageURI(preview.getRecordId());
        String fullTextResourceURI = FulltextWriteUtils.getFullTextResourceURI(preview.getRecordId(),
                FulltextWriteUtils.toID(preview.getRecordId()));

        FulltextPackage page = new FulltextPackage(annotationPageURI, null);

        // generate Fulltext Resource
        FullTextResource resource = new FullTextResource(fullTextResourceURI, null, preview.getLanguage(), preview.getRights(), uri);
        // add first annotation of type Media - this will not have any targets or text boundary
        TextBoundary tb = new TextBoundary(fullTextResourceURI);
        page.add(new Annotation(null, tb, null, AnnotationType.MEDIA, null, null));

        // add the subtitles as annotations
        SubtitleContext subtitleContext = new SubtitleContext();
        subtitleContext.start(fullTextResourceURI);
        int i = 0;
        for (SubtitleItem item : preview.getSubtitleItems()) {
            if (i++ != 0) {
                subtitleContext.separator();
            }
            int start = item.getStartTime();
            int end = start + item.getDuration();
            TimeBoundary mr = new TimeBoundary(preview.getMedia(), start, end);
            TextBoundary tr = subtitleContext.newItem(processSubtitle(item.getContent()));
            page.add(new Annotation(null, tr, mr, AnnotationType.CAPTION, null, null));
        }
        // ADD the resource in Fulltext page
        resource.setValue(subtitleContext.end());
        page.setResource(resource);
        logger.info("Successfully converted SRT to EDM for record {}. Processed Annotations - {}", preview.getRecordId(), page.size());
        return page;
    }

    private String processSubtitle(String text) {
        return PATTERN.matcher(text).replaceAll("");
    }
}
