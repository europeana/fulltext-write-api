package eu.europeana.fulltextwrite.batch.processor;

import com.dotsub.converter.model.SubtitleItem;
import eu.europeana.fulltextwrite.exception.SubtitleParsingException;
import eu.europeana.fulltextwrite.model.AnnotationChangeType;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.SubtitleType;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.service.SubtitleHandlerService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.batch.item.ItemProcessor;

public class AnnotationPreviewCreateProcessor
    implements ItemProcessor<AnnotationItem, AnnotationPreview> {

  private SubtitleHandlerService subtitleHandlerService;
  private Instant lastUpdateRun;

  @Override
  public AnnotationPreview process(AnnotationItem item) throws Exception {
    SubtitleType subtitleType = SubtitleType.getValueByMimetype(item.getBody().getFormat());

    if (subtitleType == null) {
      throw new SubtitleParsingException(
          String.format(
              "Unsupported mimeType in Annotation id=%s body.format=%s",
              item.getId(), item.getBody().getFormat()));
    }

    List<SubtitleItem> subtitleItems =
        subtitleHandlerService.parseSubtitle(
            new ByteArrayInputStream(item.getBody().getValue().getBytes(StandardCharsets.UTF_8)),
            subtitleType);

    return new AnnotationPreview.Builder(item.getTarget().getScope(), subtitleType, subtitleItems)
        .setMedia(item.getTarget().getSource())
        .setLanguage(item.getBody().getLanguage())
        .setRights(item.getBody().getEdmRights())
        .setChangeType(getChangeType(item))
        .build();
  }

  private AnnotationChangeType getChangeType(AnnotationItem item) {
    // TODO: handle deletions
    return item.getCreated().isAfter(lastUpdateRun)
        ? AnnotationChangeType.NEWLY_CREATED
        : AnnotationChangeType.UPDATED;
  }
}
