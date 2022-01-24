package eu.europeana.fulltextwrite.model.edm;

import eu.europeana.fulltext.AnnotationType;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import java.util.ArrayList;
import java.util.List;

public class Annotation {

  private String annoId;
  private Reference textReference;
  private List<TimeBoundary> targets = new ArrayList<>(1);
  private AnnotationType type;
  private String lang;
  private Float confidence;

  public Annotation(
      String annoId,
      Reference textReference,
      TimeBoundary target,
      AnnotationType type,
      String lang,
      Float confidence) {
    this.textReference = textReference;
    if (target != null) {
      targets.add(target);
    }
    this.type = type;
    this.lang = lang;
    this.confidence = confidence;
    this.annoId = (annoId != null ? annoId : FulltextWriteUtils.toID(this));
  }

  public String getAnnoId() {
    return annoId;
  }

  public Reference getTextReference() {
    return textReference;
  }

  public List<TimeBoundary> getTargets() {
    return targets;
  }

  public AnnotationType getType() {
    return type;
  }

  public String getLang() {
    return lang;
  }

  public Float getConfidence() {
    return confidence;
  }

  public boolean hasTargets() {
    return !targets.isEmpty();
  }
}
