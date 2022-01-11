package eu.europeana.fulltextwrite.model.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationSearchResponse {
  private String id;
  private int total;
  private String next;

  private List<AnnotationItem> items;

  public String getId() {
    return id;
  }

  public int getTotal() {
    return total;
  }

  public String getNext() {
    return next;
  }

  public List<AnnotationItem> getItems() {
    return items;
  }
}
