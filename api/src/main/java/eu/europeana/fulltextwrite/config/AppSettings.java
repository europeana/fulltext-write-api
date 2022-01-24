package eu.europeana.fulltextwrite.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources({
  @PropertySource("classpath:fulltext-write.properties"),
  @PropertySource(value = "classpath:fulltext-write.user.properties", ignoreResourceNotFound = true)
})
public class AppSettings {

  @Value("${auth.enabled}")
  private boolean authEnabled;

  @Value("${europeana.apikey.jwttoken.signaturekey}")
  private String apiKeyPublicKey;

  @Value("${authorization.api.name}")
  private String authorizationApiName;

  @Value("${europeana.apikey.serviceurl}")
  private String apiKeyUrl;

  @Value("${mongo.connectionUrl}")
  private String mongoConnectionUrl;

  @Value("${mongo.fulltext.database}")
  private String fulltextDatabase;

  @Value("${mongo.fulltext.ensureIndices: false}")
  private boolean ensureFulltextIndices;

  @Value("${mongo.batch.database}")
  private String batchDatabase;

  @Value("${annotations.serviceurl}")
  private String annotationsApiUrl;

  @Value("${annotations.wskey}")
  private String annotationsApiKey;

  @Value("${batch.annotations.pageSize: 50}")
  private int annotationItemsPageSize;

  @Value("${fulltext.service.url}")
  private String fulltextApiUrl;

  public boolean isAuthEnabled() {
    return authEnabled;
  }

  public String getAuthorizationApiName() {
    return authorizationApiName;
  }

  public String getApiKeyPublicKey() {
    return apiKeyPublicKey;
  }

  public String getApiKeyUrl() {
    return apiKeyUrl;
  }

  public String getBatchDatabase() {
    return batchDatabase;
  }

  public String getMongoConnectionUrl() {
    return mongoConnectionUrl;
  }

  public String getFulltextDatabase() {
    return fulltextDatabase;
  }

  public boolean ensureFulltextIndices() {
    return ensureFulltextIndices;
  }

  public String getAnnotationsApiKey() {
    return annotationsApiKey;
  }

  public String getAnnotationsApiUrl() {
    return annotationsApiUrl;
  }

  public int getAnnotationItemsPageSize() {
    return annotationItemsPageSize;
  }

  public String getFulltextApiUrl() {
    return fulltextApiUrl;
  }
}
