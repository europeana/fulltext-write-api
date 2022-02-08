package eu.europeana.fulltextwrite.batch.processor;

import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.service.AnnotationService;
import eu.europeana.fulltextwrite.service.SubtitleHandlerService;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnotationProcessor implements ItemProcessor<AnnotationItem, TranslationAnnoPage> {

  private final SubtitleHandlerService subtitleHandlerService;
  private final AnnotationService annotationService;

  public AnnotationProcessor(
      SubtitleHandlerService subtitleHandlerService, AnnotationService annotationService) {
    this.subtitleHandlerService = subtitleHandlerService;
    this.annotationService = annotationService;
  }

  @Override
  public TranslationAnnoPage process(@NonNull AnnotationItem item) throws Exception {
    AnnotationPreview annotationPreview = annotationService.createAnnotationPreview(item);
    return annotationService.createAnnoPage(annotationPreview);
  }
}
