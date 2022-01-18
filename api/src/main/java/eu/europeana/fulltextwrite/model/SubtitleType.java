package eu.europeana.fulltextwrite.model;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

/** Enum for supported Mime Types along with the handler */
public enum SubtitleType {
  WEB_VTT("text/vtt", "com.dotsub.converter.importer.impl.WebVttImportHandler"),
  SRT("text/plain", "com.dotsub.converter.importer.impl.QtTextImportHandler");
  // not sure which handler may be this SrtImportHandler or QtTextImportHandler

  private final String mimeType;
  private final String handler;

  SubtitleType(String mimeType, String handler) {
    this.mimeType = mimeType;
    this.handler = handler;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getHandler() {
    return handler;
  }

  /**
   * get the Subtitle Type based on the mime type value
   *
   * @param mimeType
   * @return
   */
  public static SubtitleType getValueByMimetype(String mimeType) {
    for (SubtitleType type : SubtitleType.values()) {
      if (type.getMimeType().equalsIgnoreCase(mimeType)) {
        return type;
      }
    }
    return null;
  }

  /**
   * Check if the mime Type is supported
   *
   * @param mimeType value to check
   * @return
   */
  public static boolean isSupported(String mimeType) {
    return Arrays.stream(SubtitleType.values())
        .anyMatch(subtitleType -> StringUtils.equals(subtitleType.getMimeType(), mimeType));
  }
}
