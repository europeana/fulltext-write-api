package eu.europeana.fulltextwrite.model.edm;

import java.util.ArrayList;
import java.util.List;

public class FulltextPackage extends ArrayList<Annotation> {

  private String baseURI;
  private FullTextResource resource;
  List<Annotation> annotationList;

  public FulltextPackage(String baseURI, FullTextResource resource) {
    this.resource = resource;
    this.baseURI = baseURI;
  }

  public String getBaseURI() {
    return baseURI;
  }

  public void setBaseURI(String baseURI) {
    this.baseURI = baseURI;
  }

  public FullTextResource getResource() {
    return resource;
  }

  public void setResource(FullTextResource resource) {
    this.resource = resource;
  }

  public boolean isLangOverriden(String lang) {
    if (lang == null) {
      return false;
    }
    return !lang.equals(resource.getLang());
  }

  @Override
  public String toString() {
    return "FulltextPackage{"
        + "baseURI='"
        + baseURI
        + '\''
        +
         ", resource=" + resource +
        "Annoations ="
        + printAnnotations()
        + "}";
  }

  private List<Annotation> printAnnotations() {
    List<Annotation> annotations = new ArrayList<>();
    for (int i = 0; i < size(); i++) {
      annotations.add(get(i));
    }
    return annotations;
  }
}
