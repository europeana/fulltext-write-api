package eu.europeana.fulltextwrite.service;

import eu.europeana.fulltextwrite.model.edm.TextBoundary;

/** Class handles the values of the subtitle and generates the Fulltext resource value */
public class SubtitleContext {
  private final StringBuilder textValue = new StringBuilder();
  private String fulltextURI = null;

  protected void start(String fulltextURl) {
    fulltextURI = fulltextURl;
  }

  protected TextBoundary newItem(String str) {
    int s = textValue.length();
    textValue.append(str);
    int e = textValue.length();
    return new TextBoundary(fulltextURI, s, e);
  }

  protected void separator() {
    textValue.append('\n');
  }

  protected String end() {
    try {
      return textValue.toString();
    } finally {
      textValue.setLength(0);
      fulltextURI = null;
    }
  }
}
