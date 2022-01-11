package eu.europeana.fulltextwrite;

import com.dotsub.converter.model.Configuration;

public class AppConstants {

  private AppConstants() {
    // hide implicit public constructor
  }

  // Bean names
  public static final String FULLTEXT_DATASTORE_BEAN = "fulltextDatastore";
  public static final String SPRINGBATCH_DATASTORE_BEAN = "springBatchDatastore";

  public static final Configuration defaultSubtitleConfig = new Configuration();
}
