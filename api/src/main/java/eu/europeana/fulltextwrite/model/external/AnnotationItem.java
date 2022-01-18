package eu.europeana.fulltextwrite.model.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationItem {
  private String id;

  private String type;

  private String motivation;

  private Instant created;

  private AnnotationCreator creator;

  private Date generated;

  private AnnotationBody body;

  private AnnotationTarget target;

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getMotivation() {
    return motivation;
  }

  public AnnotationBody getBody() {
    return body;
  }

  public Instant getCreated() {
    return created;
  }

  public AnnotationCreator getCreator() {
    return creator;
  }

  public Date getGenerated() {
    return generated;
  }

  public AnnotationTarget getTarget() {
    return target;
  }
}