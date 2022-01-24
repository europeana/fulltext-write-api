package eu.europeana.fulltextwrite.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;

public class SubtitleParsingException extends EuropeanaApiException {

  public SubtitleParsingException(String msg) {
    super(msg);
  }
}
