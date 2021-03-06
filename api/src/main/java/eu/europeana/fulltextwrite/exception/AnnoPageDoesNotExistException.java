package eu.europeana.fulltextwrite.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

public class AnnoPageDoesNotExistException extends EuropeanaApiException {

  /**
   * Initialise a new exception for which there is no root cause
   *
   * @param msg error message
   */
  public AnnoPageDoesNotExistException(String msg) {
    super(msg);
  }

  /**
   * Initialise a new exception for which there is no root cause
   *
   * @param msg error message
   * @param errorCode error code
   */
  public AnnoPageDoesNotExistException(String msg, String errorCode) {
    super(msg, errorCode);
  }

  /**
   * We don't want to log the stack trace for this exception
   *
   * @return false
   */
  @Override
  public boolean doLogStacktrace() {
    return false;
  }

  @Override
  public HttpStatus getResponseStatus() {
    return HttpStatus.NOT_FOUND;
  }
}
