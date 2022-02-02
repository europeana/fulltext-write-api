package eu.europeana.fulltextwrite.service;

import static eu.europeana.fulltextwrite.AppConstants.defaultSubtitleConfig;
import static eu.europeana.fulltextwrite.model.SubtitleType.SRT;
import static eu.europeana.fulltextwrite.model.SubtitleType.WEB_VTT;

import com.dotsub.converter.exception.FileFormatException;
import com.dotsub.converter.importer.SubtitleImportHandler;
import com.dotsub.converter.importer.impl.QtTextImportHandler;
import com.dotsub.converter.importer.impl.WebVttImportHandler;
import com.dotsub.converter.model.SubtitleItem;
import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltextwrite.exception.InvalidFormatException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.SubtitleType;
import eu.europeana.fulltextwrite.model.edm.Annotation;
import eu.europeana.fulltextwrite.model.edm.FullTextResource;
import eu.europeana.fulltextwrite.model.edm.FulltextPackage;
import eu.europeana.fulltextwrite.model.edm.TextBoundary;
import eu.europeana.fulltextwrite.model.edm.TimeBoundary;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import eu.europeana.fulltextwrite.web.WebConstants;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class SubtitleHandlerService {

  private static final Logger logger = LogManager.getLogger(SubtitleHandlerService.class);
  private static final Pattern PATTERN = Pattern.compile("[<][/]?[^<]+[/]?[>]");

  private static final Map<SubtitleType, SubtitleImportHandler> subtitleHandlerMapping =
      Map.of(WEB_VTT, new WebVttImportHandler(), SRT, new QtTextImportHandler());

  public FulltextPackage convert(AnnotationPreview preview) {
    String uri = WebConstants.BASE_ITEM_URL + preview.getRecordId();
    String annotationPageURI = FulltextWriteUtils.getAnnotationPageURI(preview.getRecordId());
    String fullTextResourceURI =
        FulltextWriteUtils.getFullTextResourceURI(
            preview.getRecordId(), FulltextWriteUtils.generateHash(preview.getRecordId()));

    FulltextPackage page = new FulltextPackage(annotationPageURI, null);

    // generate Fulltext Resource
    FullTextResource resource =
        new FullTextResource(
            fullTextResourceURI, null, preview.getLanguage(), preview.getRights(), uri);
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
    if (logger.isDebugEnabled()) {
      logger.info(
          "Successfully converted SRT to EDM for record {}. Processed Annotations - {}",
          preview.getRecordId(),
          page.size());
    }
    return page;
  }

  private String processSubtitle(String text) {
    return PATTERN.matcher(text).replaceAll("");
  }

  /** parses the text to Subtitle Item */
  public List<SubtitleItem> parseSubtitle(InputStream text, SubtitleType subtitleType)
      throws InvalidFormatException, IOException {
    SubtitleImportHandler subtitleImportHandler = subtitleHandlerMapping.get(subtitleType);
    if (subtitleImportHandler == null) {
      throw new InvalidFormatException("Format not supported : " + subtitleType.getMimeType());
    }
    try {
      return subtitleImportHandler.importFile(text, defaultSubtitleConfig);
    } catch (FileFormatException e) {
      throw new InvalidFormatException(
          "Please provide proper format!! File does not match the expected format - "
              + subtitleType.getMimeType());
    }
  }
}
