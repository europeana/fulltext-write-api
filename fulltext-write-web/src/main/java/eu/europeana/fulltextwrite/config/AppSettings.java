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

  public String getFulltextApiUrl() {
    return fulltextApiUrl;
  }
}
