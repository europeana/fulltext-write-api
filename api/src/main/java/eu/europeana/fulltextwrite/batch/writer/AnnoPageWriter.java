package eu.europeana.fulltextwrite.batch.writer;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltextwrite.service.AnnotationService;
import java.util.List;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnoPageWriter implements ItemWriter<AnnoPage> {

  private final AnnotationService annotationService;

  public AnnoPageWriter(AnnotationService annotationService) {
    this.annotationService = annotationService;
  }

  @Override
  public void write(@NonNull List<? extends AnnoPage> annoPages) throws Exception {
    annotationService.saveAnnoPageBulk(annoPages);
  }
}
