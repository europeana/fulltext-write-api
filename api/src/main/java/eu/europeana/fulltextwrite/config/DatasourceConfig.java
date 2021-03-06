package eu.europeana.fulltextwrite.config;

import static eu.europeana.fulltextwrite.AppConstants.FULLTEXT_DATASTORE_BEAN;
import static eu.europeana.fulltextwrite.AppConstants.SPRINGBATCH_DATASTORE_BEAN;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import eu.europeana.batch.entity.PackageMapper;
import eu.europeana.fulltext.util.MorphiaUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatasourceConfig {

  private static final Logger logger = LogManager.getLogger(DatasourceConfig.class);

  private final AppSettings settings;

  public DatasourceConfig(AppSettings settings) {
    this.settings = settings;
  }

  @Bean
  public MongoClient mongoClient() {
    return MongoClients.create(settings.getMongoConnectionUrl());
  }

  @Bean(FULLTEXT_DATASTORE_BEAN)
  public Datastore fulltextDatastore(MongoClient mongoClient) {
    String fulltextDatabase = settings.getFulltextDatabase();
    logger.info("Configuring fulltext database: {}", fulltextDatabase);

    // use same Morphia settings as Fulltext API

    // TODO ensure indexes when TranslationAnnoPage no longer inherits index definitions from
    // AnnoPage
    return Morphia.createDatastore(mongoClient, fulltextDatabase, MorphiaUtils.MAPPER_OPTIONS);
  }

  @Bean(SPRINGBATCH_DATASTORE_BEAN)
  public Datastore batchDatastore(MongoClient mongoClient) {
    String batchDatabase = settings.getBatchDatabase();

    logger.info("Configuring Batch database: {}", batchDatabase);
    Datastore datastore = Morphia.createDatastore(mongoClient, batchDatabase);
    // Indexes aren't created unless Entity classes are explicitly mapped.
    datastore.getMapper().mapPackage(PackageMapper.class.getPackageName());
    datastore.ensureIndexes();
    return datastore;
  }
}
