package eu.europeana.fulltextwrite;

import eu.europeana.fulltextwrite.testutils.MongoContainer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
public abstract class BaseIntegrationTest {
  private static final MongoContainer MONGO_CONTAINER;

  static {
    MONGO_CONTAINER =
        new MongoContainer("fulltext", "fulltext-write-batch")
            .withLogConsumer(new WaitingConsumer().andThen(new ToStringConsumer()));
    MONGO_CONTAINER.start();
  }

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("auth.enabled", () -> "false");
    registry.add("batch.scheduling.enabled", () -> "false");
    registry.add("mongo.connectionUrl", MONGO_CONTAINER::getConnectionUrl);
    registry.add("mongo.fulltext.database", MONGO_CONTAINER::getFulltextDb);
    registry.add("mongo.batch.database", MONGO_CONTAINER::getBatchDb);
    // remove annotationId domain restriction for tests
    registry.add("annotations.id.hosts", () -> ".*");
  }
}
