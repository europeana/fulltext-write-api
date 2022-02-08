package eu.europeana.fulltextwrite.model;

import com.dotsub.converter.model.SubtitleItem;
import java.util.Collections;
import java.util.List;

public class AnnotationPreview {

  private final String recordId;
  private final String media;
  private final String language;
  private final String rights;
  private final String source;
  private final boolean originalLang;
  private final SubtitleType subtitleType;
  private List<SubtitleItem> subtitleItems;

  private AnnotationPreview(
      String recordId,
      String media,
      String language,
      String rights,
      String source,
      boolean originalLang,
      SubtitleType subtitleType,
      List<SubtitleItem> subtitleItems) {
    this.recordId = recordId;
    this.media = media;
    this.language = language;
    this.rights = rights;
    this.source = source;
    this.originalLang = originalLang;
    this.subtitleType = subtitleType;
    this.subtitleItems = subtitleItems;
  }

  public String getRecordId() {
    return recordId;
  }

  public String getMedia() {
    return media;
  }

  public String getLanguage() {
    return language;
  }

  public String getRights() {
    return rights;
  }

  public boolean isOriginalLang() {
    return originalLang;
  }

  public SubtitleType getSubtitleType() {
    return subtitleType;
  }

  public List<SubtitleItem> getSubtitleItems() {
    return subtitleItems;
  }

  public String getSource() {
    return source;
  }

  public static class Builder {
    private String recordId;
    private String media;
    private String language;
    private String rights;
    private String source;
    private boolean originalLang;
    private SubtitleType subtitleType;
    private final List<SubtitleItem> subtitleItems;

    public Builder(String recordId, SubtitleType subtitleType, List<SubtitleItem> subtitleItems) {
      this.recordId = recordId;
      this.subtitleType = subtitleType;
      this.subtitleItems = Collections.unmodifiableList(subtitleItems);
    }

    public Builder setMedia(String media) {
      this.media = media;
      return this;
    }

    public Builder setLanguage(String language) {
      this.language = language;
      return this;
    }

    public Builder setRights(String rights) {
      this.rights = rights;
      return this;
    }

    public Builder setOriginalLang(boolean originalLang) {
      this.originalLang = originalLang;
      return this;
    }

    public Builder setSubtitleType(SubtitleType subtitleType) {
      this.subtitleType = subtitleType;
      return this;
    }

    public Builder setSource(String source) {
      this.source = source;
      return this;
    }

    public AnnotationPreview build() {
      return new AnnotationPreview(
          recordId, media, language, rights, source, originalLang, subtitleType, subtitleItems);
    }
  }
}
