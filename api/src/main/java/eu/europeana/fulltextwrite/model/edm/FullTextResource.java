package eu.europeana.fulltextwrite.model.edm;

public class FullTextResource implements Reference {

  private String fullTextResourceURI;
  private String value;
  private String lang;
  private String rights;
  private String recordURI;

  public FullTextResource(
      String fullTextResourceURI, String value, String lang, String rights, String recordURI) {
    this.fullTextResourceURI = fullTextResourceURI;
    this.value = value;
    this.lang = lang;
    this.rights = rights;
    this.recordURI = recordURI;
  }

  public String getFullTextResourceURI() {
    return fullTextResourceURI;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getLang() {
    return lang;
  }

  public String getRights() {
    return rights;
  }

  public String getRecordURI() {
    return recordURI;
  }

  public FullTextResource getResource() {
    return this;
  }

  @Override
  public String getResourceURL() {
    return getFullTextResourceURI();
  }

  @Override
  public String getURL() {
    return getResourceURL();
  }
}
