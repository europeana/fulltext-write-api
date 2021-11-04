package eu.europeana.fulltextwrite.config;

import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

  private final AppSettings settings;

  public BeanConfig(AppSettings settings) {
    this.settings = settings;
  }

  @Bean
  public EuropeanaClientDetailsService getClientDetailsService() {
    EuropeanaClientDetailsService clientDetailsService = new EuropeanaClientDetailsService();
    clientDetailsService.setApiKeyServiceUrl(settings.getApiKeyUrl());
    return clientDetailsService;
  }
}
