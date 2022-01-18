package eu.europeana.fulltextwrite.model.edm;

public class TextBoundary implements Reference {

  private String resourceUrl;
  public int from;
  public int to;

  public TextBoundary(String resourceUrl) {
    this.resourceUrl = resourceUrl;
  }

  public TextBoundary(String resourceUrl, int from, int to) {
    this.resourceUrl = resourceUrl;
    this.from = from;
    this.to = to;
  }

  public int getFrom() {
    return from;
  }

  public int getTo() {
    return to;
  }

  public String getFragment() {
    if(from == 0 && to == 0) {
      return "";
    } else {
      return ("#char=" + from + "," + to);
    }
  }

  public void shift(int chars) {
    this.from += chars;
    this.to += chars;
  }

  @Override
  public String getResourceURL() {
    return resourceUrl;
  }

  @Override
  public String getURL() {
    return getResourceURL() + getFragment();
  }

  @Override
  public String toString() {
    return "TextBoundary{" + " from=" + from + ", to=" + to + '}';
  }
}
