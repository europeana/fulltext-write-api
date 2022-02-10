package eu.europeana.fulltextwrite.batch.writer;

import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltextwrite.service.FTWriteService;
import java.util.List;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnoPageWriter implements ItemWriter<TranslationAnnoPage> {

  private final FTWriteService annotationService;

  public AnnoPageWriter(FTWriteService annotationService) {
    this.annotationService = annotationService;
  }

  @Override
  public void write(@NonNull List<? extends TranslationAnnoPage> annoPages) throws Exception {
    annotationService.upsertAnnoPage(annoPages);
  }
}
