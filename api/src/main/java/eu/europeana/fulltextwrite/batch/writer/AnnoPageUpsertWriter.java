package eu.europeana.fulltextwrite.batch.writer;

import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltextwrite.service.AnnotationService;
import java.util.List;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnoPageUpsertWriter implements ItemWriter<TranslationAnnoPage> {

  private final AnnotationService annotationService;

  public AnnoPageUpsertWriter(AnnotationService annotationService) {
    this.annotationService = annotationService;
  }

  @Override
  public void write(@NonNull List<? extends TranslationAnnoPage> annoPages) throws Exception {
    annotationService.upsertAnnoPage(annoPages);
  }
}
