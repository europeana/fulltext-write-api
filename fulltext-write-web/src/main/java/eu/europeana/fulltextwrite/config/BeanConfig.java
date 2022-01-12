package eu.europeana.fulltextwrite.config;

import static eu.europeana.fulltext.util.MorphiaUtils.MAPPER_OPTIONS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoClient;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public Datastore datastore(MongoClient mongoClient, MongoProperties mongoProperties) {
    String database = mongoProperties.getDatabase();
    MongoClientURI uri = new MongoClientURI(mongoProperties.getUri());
    if (StringUtils.isEmpty(database)) {
      database = uri.getDatabase();
    }
    LogManager.getLogger(BeanConfig.class)
        .info("Connecting to {} Mongo database on hosts {}...", database, uri.getHosts());

    return Morphia.createDatastore(mongoClient, database, MAPPER_OPTIONS);
  }
}
