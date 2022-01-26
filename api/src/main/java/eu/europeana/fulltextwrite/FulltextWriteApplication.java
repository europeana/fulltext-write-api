package eu.europeana.fulltextwrite;

import eu.europeana.fulltextwrite.config.SocksProxyConfig;
import eu.europeana.fulltextwrite.util.SocksProxyActivator;
import org.apache.logging.log4j.LogManager;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Main application. Allows deploying as a war and logs instance data when deployed in Cloud Foundry
 */
@SpringBootApplication(
    scanBasePackages = {"eu.europeana.fulltextwrite"},
    exclude = {
      // Remove these exclusions to re-enable security
      SecurityAutoConfiguration.class,
      ManagementWebSecurityAutoConfiguration.class,
    })
@EnableBatchProcessing
public class FulltextWriteApplication extends SpringBootServletInitializer {

  /**
   * Main entry point of this application
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    // When deploying to Cloud Foundry, this will log the instance index number, IP and GUID
    LogManager.getLogger(FulltextWriteApplication.class)
        .info(
            "CF_INSTANCE_INDEX  = {}, CF_INSTANCE_GUID = {}, CF_INSTANCE_IP  = {}",
            System.getenv("CF_INSTANCE_INDEX"),
            System.getenv("CF_INSTANCE_GUID"),
            System.getenv("CF_INSTANCE_IP"));

    // Activate socks proxy (if your application requires it)
    SocksProxyActivator.activate(
        new SocksProxyConfig("fulltext-write.properties", "fulltext-write.user.properties"));

    // TODO validate command-line args before passing on, or pass on null instead
    SpringApplication.run(FulltextWriteApplication.class, args);
  }

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(FulltextWriteApplication.class);
  }
}
