package eu.europeana.fulltextwrite.batch.writer;

import eu.europeana.fulltextwrite.service.AnnotationService;
import java.util.List;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnoPageDeletionWriter implements ItemWriter<String> {

  private final AnnotationService annotationService;

  public AnnoPageDeletionWriter(AnnotationService annotationService) {
    this.annotationService = annotationService;
  }

  @Override
  public void write(@NonNull List<? extends String> annoPages) throws Exception {
    annotationService.deleteAnnoPagesWithSources(annoPages);
  }
}
