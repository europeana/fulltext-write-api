package eu.europeana.fulltextwrite.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/** Exception thrown when errors occur during AnnoSync */
public class AnnotationSyncException extends EuropeanaApiException {

  public AnnotationSyncException(String msg) {
    super(msg);
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.UNPROCESSABLE_ENTITY;
  }
}
