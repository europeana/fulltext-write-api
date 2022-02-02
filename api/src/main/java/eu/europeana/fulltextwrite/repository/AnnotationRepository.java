package eu.europeana.fulltextwrite.repository;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.ANNOTATIONS;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DATASET_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LANGUAGE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LOCAL_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.MODIFIED;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.PAGE_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.RESOURCE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.TARGET_ID;
import static eu.europeana.fulltextwrite.AppConstants.FULLTEXT_DATASTORE_BEAN;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.stages.Projection;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class AnnotationRepository {

  private final Datastore datastore;

  // Indicates that an update query should be executed as an "upsert",
  // ie. creates new records if they do not already exist, or updates them if they do.
  public static final UpdateOptions UPSERT_OPTS = new UpdateOptions().upsert(true);

  public AnnotationRepository(@Qualifier(FULLTEXT_DATASTORE_BEAN) Datastore datastore) {
    this.datastore = datastore;
  }

  public TranslationAnnoPage getAnnoPageByTargetId(
      String datasetId, String localId, String targetId) {
    Aggregation<TranslationAnnoPage> query =
        datastore
            .aggregate(TranslationAnnoPage.class)
            .match(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId), eq(TARGET_ID, targetId))
            .project(
                Projection.of()
                    .include(DATASET_ID)
                    .include(LOCAL_ID)
                    .include(PAGE_ID)
                    .include(TARGET_ID));

    return query.execute(TranslationAnnoPage.class).tryNext();
  }

  /**
   * Saves an TranslationAnnoPage to the database
   *
   * @param annoPage TranslationAnnoPage object to save
   * @return the saved TranslationAnnoPage document
   */
  public TranslationAnnoPage saveAnnoPage(TranslationAnnoPage annoPage) {
    return datastore.save(annoPage);
  }

  public BulkWriteResult upsertBulk(List<? extends TranslationAnnoPage> annoPageList) {
    MongoCollection<TranslationAnnoPage> collection =
        datastore.getMapper().getCollection(TranslationAnnoPage.class);

    List<WriteModel<TranslationAnnoPage>> updates = new ArrayList<>();

    Instant now = Instant.now();

    for (TranslationAnnoPage annoPage : annoPageList) {
      Document updateDoc =
          new Document(DATASET_ID, annoPage.getDsId())
              .append(LOCAL_ID, annoPage.getLcId())
              .append(PAGE_ID, annoPage.getPgId())
              .append(TARGET_ID, annoPage.getTgtId())
              .append(ANNOTATIONS, annoPage.getAns())
              .append(MODIFIED, now)
              .append(LANGUAGE, annoPage.getLang())
              .append(RESOURCE, annoPage.getRes());
      // className discriminator no longer used

      updates.add(
          new UpdateOneModel<>(
              new Document(
                  Map.of(
                      DATASET_ID,
                      annoPage.getDsId(),
                      LOCAL_ID,
                      annoPage.getLcId(),
                      LANGUAGE,
                      annoPage.getLang(),
                      PAGE_ID,
                      annoPage.getPgId())),
              new Document("$set", updateDoc),
              UPSERT_OPTS));
    }

    return collection.bulkWrite(updates);
  }

  /** Only for tests */
  public void dropCollection() {
    datastore.getMapper().getCollection(TranslationAnnoPage.class).drop();
  }

  public long count() {
    return datastore.getMapper().getCollection(TranslationAnnoPage.class).countDocuments();
  }
}
