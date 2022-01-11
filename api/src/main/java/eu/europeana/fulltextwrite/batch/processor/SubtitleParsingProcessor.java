package eu.europeana.fulltextwrite.batch.processor;

import static eu.europeana.fulltextwrite.AppConstants.defaultSubtitleConfig;

import com.dotsub.converter.importer.SubtitleImporter;
import com.dotsub.converter.model.SubtitleItem;
import eu.europeana.fulltextwrite.exception.SubtitleParsingException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import org.springframework.batch.item.ItemProcessor;

public class SubtitleParsingProcessor
    implements ItemProcessor<AnnotationPreview, AnnotationPreview> {
  private final SubtitleImporter subtitleImporter;

  public SubtitleParsingProcessor(SubtitleImporter subtitleImporter) {
    this.subtitleImporter = subtitleImporter;
  }

  @Override
  public AnnotationPreview process(AnnotationPreview annotationPreview) throws Exception {
    if (Objects.equals(annotationPreview.getSubtitle(), InputStream.nullInputStream())) {
      throw new SubtitleParsingException(
          "No InputStream available on Annotation recordId: " + annotationPreview.getRecordId());
    }

    List<SubtitleItem> subtitleItems =
        subtitleImporter.importFile(annotationPreview.getSubtitle(), defaultSubtitleConfig);

    annotationPreview.setSubtitleItems(subtitleItems);
    return annotationPreview;
  }
}
