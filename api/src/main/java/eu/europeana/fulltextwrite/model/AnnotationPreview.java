package eu.europeana.fulltextwrite.model;

import com.dotsub.converter.model.SubtitleItem;
import java.io.InputStream;
import java.util.List;

public class AnnotationPreview {

  private String recordId;
  private String media;
  private String language;
  private String rights;
  private boolean originalLang;
  private SubtitleType subtitleType;
  private InputStream subtitle;
  private AnnotationChangeType changeType;
  private List<SubtitleItem> subtitleItems;

  public AnnotationPreview() {}

  public AnnotationPreview(
      String recordId,
      String media,
      String language,
      String rights,
      boolean originalLang,
      SubtitleType subtitleType,
      InputStream subtitle,
      AnnotationChangeType changeType) {
    this.recordId = recordId;
    this.media = media;
    this.language = language;
    this.rights = rights;
    this.originalLang = originalLang;
    this.subtitleType = subtitleType;
    this.subtitle = subtitle;
    this.changeType = changeType;
  }

  public void setRecordId(String recordId) {
    this.recordId = recordId;
  }

  public void setMedia(String media) {
    this.media = media;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public void setRights(String rights) {
    this.rights = rights;
  }

  public void setOriginalLang(boolean originalLang) {
    this.originalLang = originalLang;
  }

  public void setSubtitleType(SubtitleType subtitleType) {
    this.subtitleType = subtitleType;
  }

  public void setSubtitle(InputStream subtitle) {
    this.subtitle = subtitle;
  }

  public void setChangeType(AnnotationChangeType changeType) {
    this.changeType = changeType;
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

  public InputStream getSubtitle() {
    return subtitle;
  }

  public AnnotationChangeType getChangeType() {
    return changeType;
  }

  public List<SubtitleItem> getSubtitleItems() {
    return subtitleItems;
  }

  public void setSubtitleItems(List<SubtitleItem> subtitleItems) {
    this.subtitleItems = subtitleItems;
  }
}
