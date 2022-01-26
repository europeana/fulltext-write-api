package eu.europeana.fulltextwrite.config;

import static eu.europeana.fulltextwrite.AppConstants.SPRINGBATCH_DATASTORE_BEAN;

import dev.morphia.Datastore;
import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;
import eu.europeana.batch.config.MongoBatchConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;

@Configuration
public class BeanConfig {

  private final AppSettings settings;

  public BeanConfig(AppSettings settings) {
    this.settings = settings;
  }

  @Bean
  public EuropeanaClientDetailsService clientDetailsService() {
    EuropeanaClientDetailsService clientDetailsService = new EuropeanaClientDetailsService();
    clientDetailsService.setApiKeyServiceUrl(settings.getApiKeyUrl());
    return clientDetailsService;
  }

  /**
   * Configures Spring Batch to use Mongo
   *
   * @param datastore Morphia datastore for Spring Batch
   * @return BatchConfigurer instance
   */
  @Bean
  public MongoBatchConfigurer mongoBatchConfigurer(
      @Qualifier(SPRINGBATCH_DATASTORE_BEAN) Datastore datastore) {
    // launch all Spring Batch jobs within the Spring Scheduling thread
    return new MongoBatchConfigurer(datastore, new SyncTaskExecutor());
  }
}
