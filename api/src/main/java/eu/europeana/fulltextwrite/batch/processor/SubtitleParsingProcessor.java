package eu.europeana.fulltextwrite.batch.processor;

import com.dotsub.converter.model.SubtitleItem;
import eu.europeana.fulltextwrite.exception.SubtitleParsingException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.service.SubtitleHandler;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import org.springframework.batch.item.ItemProcessor;

public class SubtitleParsingProcessor
    implements ItemProcessor<AnnotationPreview, AnnotationPreview> {

  @Override
  public AnnotationPreview process(AnnotationPreview annotationPreview) throws Exception {
    if (Objects.equals(annotationPreview.getSubtitle(), InputStream.nullInputStream())) {
      throw new SubtitleParsingException(
          "No InputStream available on Annotation recordId: " + annotationPreview.getRecordId());
    }
    List<SubtitleItem> subtitleItems =
        new SubtitleHandler()
            .parseSubtitle(annotationPreview.getSubtitle(), annotationPreview.getSubtitleType());
    annotationPreview.setSubtitleItems(subtitleItems);
    return annotationPreview;
  }
}
