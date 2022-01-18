package eu.europeana.fulltextwrite.model.edm;

import org.apache.commons.lang3.time.DurationFormatUtils;

public class TimeBoundary implements Reference {

  private static String FORMAT = "HH:mm:ss.SSS";

  private String resourceUrl;
  private int start;
  private int end;

  public TimeBoundary(String resourceUrl) {
    this.resourceUrl = resourceUrl;
  }

  public TimeBoundary(String resourceUrl, int start, int end) {
    this.resourceUrl = resourceUrl;
    this.start = start;
    this.end = end;
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getEnd() {
    return end;
  }

  public void setEnd(int end) {
    this.end = end;
  }

  public String getFragment() {
    String start = DurationFormatUtils.formatDuration(this.start, FORMAT);
    String end = DurationFormatUtils.formatDuration(this.end, FORMAT);
    return ("#t=" + start + "," + end);
  }

  public boolean isValid() {
    return (this.start >= 0 && this.end >= 0 && this.start < this.end);
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
    return "TimeBoundary{"
        + "resourceUrl='"
        + resourceUrl
        + '\''
        + ", start="
        + start
        + ", end="
        + end
        + '}';
  }
}
