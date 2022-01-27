package eu.europeana.fulltextwrite.batch.processor;

import com.dotsub.converter.model.SubtitleItem;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltextwrite.exception.InvalidFormatException;
import eu.europeana.fulltextwrite.exception.SubtitleParsingException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.SubtitleType;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.service.AnnotationService;
import eu.europeana.fulltextwrite.service.SubtitleHandlerService;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnotationProcessor implements ItemProcessor<AnnotationItem, AnnoPage> {

  private final SubtitleHandlerService subtitleHandlerService;
  private final AnnotationService annotationService;

  public AnnotationProcessor(
      SubtitleHandlerService subtitleHandlerService, AnnotationService annotationService) {
    this.subtitleHandlerService = subtitleHandlerService;
    this.annotationService = annotationService;
  }

  @Override
  public AnnoPage process(@NonNull AnnotationItem item) throws Exception {
    AnnotationPreview annotationPreview = createAnnotationPreview(item);
    AnnoPage annoPage = annotationService.getAnnoPage(annotationPreview);

    return annoPage;
  }

  private AnnotationPreview createAnnotationPreview(AnnotationItem item)
      throws SubtitleParsingException, IOException, InvalidFormatException {
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

    return new AnnotationPreview.Builder(
            FulltextWriteUtils.getRecordIdFromUri(item.getTarget().getScope()),
            subtitleType,
            subtitleItems)
        .setMedia(item.getTarget().getSource())
        .setLanguage(item.getBody().getLanguage())
        .setRights(item.getBody().getEdmRights())
        .build();
  }
}
