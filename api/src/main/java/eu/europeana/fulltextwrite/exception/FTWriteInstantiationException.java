package eu.europeana.fulltextwrite.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class FTWriteInstantiationException extends EuropeanaApiException {
  /**
   * Initialise a new exception for which there is no root cause
   *
   * @param msg error message
   */
  public FTWriteInstantiationException(String msg) {
    super(msg);
  }

  /**
   * Initialise a new exception for which there is no root cause
   *
   * @param msg error message
   * @param errorCode error code
   */
  public FTWriteInstantiationException(String msg, String errorCode) {
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
