package eu.europeana.fulltextwrite.repository;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;

import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.stages.Projection;
import eu.europeana.fulltext.entity.AnnoPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AnnotationRepository {

  @Autowired protected Datastore datastore;

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
}
