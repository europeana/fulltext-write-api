package eu.europeana.fulltextwrite.web;

import eu.europeana.api.commons.service.authorization.AuthorizationService;
import eu.europeana.api.commons.web.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;

public class BaseRest extends BaseRestController {
  @Autowired private AuthorizationService emAuthorizationService;

  public AuthorizationService getAuthorizationService() {
    return emAuthorizationService;
  }
}
