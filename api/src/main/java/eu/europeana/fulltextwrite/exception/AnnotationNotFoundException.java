package eu.europeana.fulltextwrite.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/** Exception thrown when an Annotation does not exist in Annotations API */
public class AnnotationNotFoundException extends EuropeanaApiException {

  /**
   * Initialise a new exception for which there is no root cause
   *
   * @param msg error message
   */
  public AnnotationNotFoundException(String msg) {
    super(msg);
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public boolean doLogStacktrace() {
    return false;
  }
}
