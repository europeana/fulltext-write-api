package eu.europeana.fulltextwrite.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/** Exception thrown when an annotation has been deleted in Annotations API */
public class AnnotationGoneException extends EuropeanaApiException {

  public AnnotationGoneException(String msg) {
    super(msg);
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.GONE;
  }

  @Override
  public boolean doLogStacktrace() {
    return false;
  }
}
