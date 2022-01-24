package eu.europeana.fulltextwrite.model;

import com.dotsub.converter.model.SubtitleItem;
import java.util.Collections;
import java.util.List;

public class AnnotationPreview {

  private final String recordId;
  private final String media;
  private final String language;
  private final String rights;
  private final boolean originalLang;
  private final SubtitleType subtitleType;
  private final AnnotationChangeType changeType;
  private List<SubtitleItem> subtitleItems;

  private AnnotationPreview(
      String recordId,
      String media,
      String language,
      String rights,
      boolean originalLang,
      SubtitleType subtitleType,
      AnnotationChangeType changeType,
      List<SubtitleItem> subtitleItems) {
    this.recordId = recordId;
    this.media = media;
    this.language = language;
    this.rights = rights;
    this.originalLang = originalLang;
    this.subtitleType = subtitleType;
    this.changeType = changeType;
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

  public AnnotationChangeType getChangeType() {
    return changeType;
  }

  public List<SubtitleItem> getSubtitleItems() {
    return subtitleItems;
  }

  public static class Builder {
    private String recordId;
    private String media;
    private String language;
    private String rights;
    private boolean originalLang;
    private SubtitleType subtitleType;
    private AnnotationChangeType changeType;
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

    public Builder setChangeType(AnnotationChangeType changeType) {
      this.changeType = changeType;
      return this;
    }

    public AnnotationPreview build() {
      return new AnnotationPreview(
          recordId, media, language, rights, originalLang, subtitleType, changeType, subtitleItems);
    }
  }
}
