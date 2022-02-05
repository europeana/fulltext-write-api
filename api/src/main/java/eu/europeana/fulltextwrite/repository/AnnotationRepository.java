package eu.europeana.fulltextwrite.repository;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.ANNOTATIONS;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DATASET_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LANGUAGE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LOCAL_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.MODIFIED;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.PAGE_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.RESOURCE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.SOURCE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.TARGET_ID;
import static eu.europeana.fulltextwrite.AppConstants.FULLTEXT_DATASTORE_BEAN;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.query.FindOptions;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltext.util.MorphiaUtils;
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

  public boolean annoPageExists(String datasetId, String localId, String targetId, String lang) {
    TranslationAnnoPage annoPage =
        datastore
            .find(TranslationAnnoPage.class)
            .filter(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(LANGUAGE, lang),
                eq(TARGET_ID, targetId))
            // Since document contents aren't important for this query, just fetch _id
            .iterator(new FindOptions().limit(1).projection().include("_id"))
            .tryNext();

    return annoPage != null;
  }

  public TranslationAnnoPage getAnnoPage(
      String datasetId, String localId, String targetId, String lang, String pgId) {
    Aggregation<TranslationAnnoPage> query =
        datastore
            .aggregate(TranslationAnnoPage.class)
            .match(
                eq(DATASET_ID, datasetId),
                eq(LOCAL_ID, localId),
                eq(TARGET_ID, targetId),
                eq(LANGUAGE, lang),
                eq(PAGE_ID, pgId));

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
              .append(SOURCE, annoPage.getSource())
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

  public long deleteAnnoPagesWithSource(String source) {
    return datastore
        .find(TranslationAnnoPage.class)
        .filter(eq(SOURCE, source))
        .delete(MorphiaUtils.MULTI_DELETE_OPTS)
        .getDeletedCount();
  }
}
