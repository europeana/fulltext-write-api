package eu.europeana.fulltextwrite.model.edm;

import java.util.ArrayList;

public class FulltextPackage extends ArrayList<Annotation> {

  private String baseURI;
  private FullTextResource resource;

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
}
