package eu.europeana.fulltextwrite.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/** Exception thrown when an annotation doesn't exist in Annotations API */
public class AnnotationNotFoundException extends EuropeanaApiException {

  public AnnotationNotFoundException(String msg) {
    super(msg);
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.NOT_FOUND;
  }
}
