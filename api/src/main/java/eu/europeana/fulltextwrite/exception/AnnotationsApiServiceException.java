package eu.europeana.fulltextwrite.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/** Exception thrown when errors occur while querying the Annotations API */
public class AnnotationsApiServiceException extends EuropeanaApiException {

  public AnnotationsApiServiceException(String msg) {
    super(msg);
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.BAD_GATEWAY;
  }
}
