package eu.europeana.fulltextwrite.model;

public enum SubtitleType {
  WEB_VTT("text/vtt"),
  SRT("text/plain");

  private final String mimeType;

  SubtitleType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getMimeType() {
    return mimeType;
  }
}
