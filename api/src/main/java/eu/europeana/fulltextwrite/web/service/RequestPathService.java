package eu.europeana.fulltextwrite.web.service;

import eu.europeana.api.commons.web.service.AbstractRequestPathMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
public class RequestPathService extends AbstractRequestPathMethodService {

  @Autowired
  public RequestPathService(WebApplicationContext applicationContext) {
    super(applicationContext);
  }
}
