package eu.europeana.fulltextwrite.service;

import eu.europeana.api.commons.definitions.vocabulary.Role;
import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;
import eu.europeana.api.commons.service.authorization.BaseAuthorizationService;
import eu.europeana.fulltextwrite.config.AppSettings;
import eu.europeana.fulltextwrite.web.auth.Roles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.ClientDetailsService;

@Configuration
public class FTWriteAuthorizationService extends BaseAuthorizationService
    implements eu.europeana.api.commons.service.authorization.AuthorizationService {

  private final AppSettings appSettings;
  private final EuropeanaClientDetailsService clientDetailsService;

  @Autowired
  public FTWriteAuthorizationService(
      AppSettings appSettings, EuropeanaClientDetailsService clientDetailsService) {
    this.appSettings = appSettings;

    this.clientDetailsService = clientDetailsService;
  }

  @Override
  protected ClientDetailsService getClientDetailsService() {
    return clientDetailsService;
  }

  @Override
  protected String getSignatureKey() {
    return appSettings.getApiKeyPublicKey();
  }

  @Override
  protected String getApiName() {
    return appSettings.getAuthorizationApiName();
  }

  @Override
  protected Role getRoleByName(String name) {
    return Roles.getRoleByName(name);
  }
}
