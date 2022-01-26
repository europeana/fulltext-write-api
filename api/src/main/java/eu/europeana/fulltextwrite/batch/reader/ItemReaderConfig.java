package eu.europeana.fulltextwrite.batch.reader;

import static eu.europeana.fulltextwrite.AppConstants.ANNO_ITEM_READER;

import eu.europeana.fulltextwrite.config.AppSettings;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.service.AnnotationsApiRestService;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ItemReaderConfig {
  private final AnnotationsApiRestService annotationsApiRestService;
  private final AppSettings appSettings;

  public ItemReaderConfig(
      AnnotationsApiRestService annotationsApiRestService, AppSettings appSettings) {
    this.annotationsApiRestService = annotationsApiRestService;
    this.appSettings = appSettings;
  }

  @Bean(name = ANNO_ITEM_READER)
  public SynchronizedItemStreamReader<AnnotationItem> annotationReader() {
    AnnotationItemReader reader =
        new AnnotationItemReader(
            annotationsApiRestService, appSettings.getAnnotationItemsPageSize());
    return threadSafeReader(reader);
  }

  /** Makes ItemReader thread-safe */
  private <T> SynchronizedItemStreamReader<T> threadSafeReader(ItemStreamReader<T> reader) {
    final SynchronizedItemStreamReader<T> synchronizedItemStreamReader =
        new SynchronizedItemStreamReader<>();
    synchronizedItemStreamReader.setDelegate(reader);
    return synchronizedItemStreamReader;
  }
}
