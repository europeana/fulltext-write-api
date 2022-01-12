package eu.europeana.fulltextwrite.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * An exception to demonstrate error handling with ResponseStatus annotation It's recommended that
 * all exceptions created in the API extends the EuropeanaApiException
 */
@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
public class MediaTypeNotSupportedException extends EuropeanaApiException {

  /**
   * Initialise a new exception for which there is no root cause
   *
   * @param msg error message
   */
  public MediaTypeNotSupportedException(String msg) {
    super(msg);
  }

  /**
   * Initialise a new exception for which there is no root cause
   *
   * @param msg error message
   * @param errorCode error code
   */
  public MediaTypeNotSupportedException(String msg, String errorCode) {
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
}
