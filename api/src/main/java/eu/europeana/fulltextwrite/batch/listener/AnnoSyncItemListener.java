package eu.europeana.fulltextwrite.batch.listener;

import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getAnnoPageToString;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AnnoSyncItemListener extends ItemListenerSupport<AnnotationItem, AnnoPage> {
  private static final Logger logger = LogManager.getLogger(AnnoSyncItemListener.class);

  @Override
  public void onReadError(@NonNull Exception e) {
    // No item linked to error, so we just log a warning
    logger.warn("onReadError", e);
  }

  @Override
  public void onProcessError(@NonNull AnnotationItem item, @NonNull Exception e) {
    // just log warning for now
    logger.warn(
        "Error processing AnnotationItem id={}; recordId={}",
        item.getId(),
        item.getTarget().getScope(),
        e);
  }

  @Override
  public void onWriteError(@NonNull Exception ex, @NonNull List<? extends AnnoPage> annoPages) {
    logger.warn("Error saving AnnoPages {}, ", getAnnoPageToString(annoPages), ex);
  }
}
