package eu.europeana.fulltextwrite.batch.processor;

import eu.europeana.fulltextwrite.model.AnnotationChangeType;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.SubtitleType;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.batch.item.ItemProcessor;

public class AnnotationPreviewCreateProcessor
    implements ItemProcessor<AnnotationItem, AnnotationPreview> {

  private Instant lastUpdateRun;

  @Override
  public AnnotationPreview process(AnnotationItem item) throws Exception {
    AnnotationPreview annotationPreview = new AnnotationPreview();
    annotationPreview.setRecordId(item.getTarget().getScope());
    annotationPreview.setMedia(item.getTarget().getSource());
    annotationPreview.setLanguage(item.getBody().getLanguage());
    annotationPreview.setRights(item.getBody().getEdmRights());
    annotationPreview.setSubtitle(
        new ByteArrayInputStream(item.getBody().getValue().getBytes(StandardCharsets.UTF_8)));
    annotationPreview.setChangeType(getChangeType(item));
    annotationPreview.setSubtitleType(SubtitleType.getValueByMimetype(item.getBody().getFormat()));

    return annotationPreview;
  }

  private AnnotationChangeType getChangeType(AnnotationItem item) {
    // TODO: handle deletions
    return item.getCreated().isAfter(lastUpdateRun)
        ? AnnotationChangeType.NEWLY_CREATED
        : AnnotationChangeType.UPDATED;
  }
}
