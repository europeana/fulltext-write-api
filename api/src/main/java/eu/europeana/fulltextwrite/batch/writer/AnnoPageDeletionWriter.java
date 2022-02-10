package eu.europeana.fulltextwrite.batch.writer;

import eu.europeana.fulltextwrite.service.FTWriteService;
import java.util.List;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnoPageDeletionWriter implements ItemWriter<String> {

  private final FTWriteService ftWriteService;

  public AnnoPageDeletionWriter(FTWriteService ftWriteService) {
    this.ftWriteService = ftWriteService;
  }

  @Override
  public void write(@NonNull List<? extends String> annoPages) throws Exception {
    ftWriteService.deleteAnnoPagesWithSources(annoPages);
  }
}
