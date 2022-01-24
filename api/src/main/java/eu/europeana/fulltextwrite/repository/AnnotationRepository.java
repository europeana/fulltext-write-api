package eu.europeana.fulltextwrite.repository;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DATASET_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LOCAL_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.PAGE_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.TARGET_ID;
import static eu.europeana.fulltextwrite.AppConstants.FULLTEXT_DATASTORE_BEAN;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.stages.Projection;
import eu.europeana.fulltext.entity.AnnoPage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class AnnotationRepository {

  protected final Datastore datastore;

  public AnnotationRepository(@Qualifier(FULLTEXT_DATASTORE_BEAN) Datastore datastore) {
    this.datastore = datastore;
  }

  public AnnoPage getAnnoPageByTargetId(String datasetId, String localId, String targetId) {
    Aggregation<AnnoPage> query =
        datastore
            .aggregate(AnnoPage.class)
            .match(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId), eq(TARGET_ID, targetId))
            .project(
                Projection.of()
                    .include(DATASET_ID)
                    .include(LOCAL_ID)
                    .include(PAGE_ID)
                    .include(TARGET_ID));

    return query.execute(AnnoPage.class).tryNext();
  }

  public AnnoPage saveAnnoPage(AnnoPage annoPage) {
    return datastore.save(annoPage);
  }

  /** Only for tests */
  public void dropCollection() {
    datastore.getMapper().getCollection(AnnoPage.class).drop();
  }
}
