package eu.europeana.fulltextwrite;

import com.dotsub.converter.model.Configuration;

public class AppConstants {

  private AppConstants() {
    // hide implicit public constructor
  }

  // Bean names
  public static final String FULLTEXT_DATASTORE_BEAN = "fulltextDatastore";
  public static final String SPRINGBATCH_DATASTORE_BEAN = "springBatchDatastore";
  public static final String ANNO_SYNC_TASK_EXECUTOR = "annoSyncTaskExecutor";

  public static final String ANNO_ITEM_READER = "annoItemReader";

  public static final String STEP_SYNCHRONISE_ANNOTATIONS = "synchroniseAnnoStep";

  public static final Configuration defaultSubtitleConfig = new Configuration();

  public static final String CONTENT_TYPE_VTT = "text/vtt";
}
