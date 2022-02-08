package eu.europeana.fulltextwrite.repository;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;
import static eu.europeana.fulltextwrite.AppConstants.FULLTEXT_DATASTORE_BEAN;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.query.experimental.filters.Filter;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltext.entity.TranslationResource;
import eu.europeana.fulltextwrite.AppConstants;
import java.time.Instant;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
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

  public TranslationAnnoPage getAnnoPageByPageIdLang(
      String datasetId, String localId, String pageId, String lang) {
    Document result = getAnnoPageAndResource(datasetId, localId, pageId, lang);
    return MongoUtils.processMongoDocument(result, datasetId, localId, pageId, lang);
  }

  public boolean existsTranslationByPageIdLang(
      String datasetId, String localId, String pageId, String lang) {
    List<Filter> filter =
        new ArrayList<>(
            Arrays.asList(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId), eq(PAGE_ID, pageId)));
    if (StringUtils.isNotEmpty(lang)) {
      filter.add(eq(LANGUAGE, lang));
    }
    return datastore.find(TranslationAnnoPage.class).filter(filter.toArray(new Filter[0])).count()
        > 0L;
  }

  /**
   * Method will return the translation AnnoPage along with the Translation resource
   *
   * <p>This is too fix the issue with translation resource being fetched as AnnoPage.res and it's
   * not possible to get the resource value from that.
   *
   * @param datasetId
   * @param localId
   * @param pageId
   * @param lang
   * @return
   */
  private Document getAnnoPageAndResource(
      String datasetId, String localId, String pageId, String lang) {
    return datastore
        .getDatabase()
        .getCollection(TranslationAnnoPage.class.getSimpleName())
        .aggregate(
            Arrays.asList(
                new Document(
                    MONGO_MATCH,
                    new Document(DATASET_ID, datasetId)
                        .append(LOCAL_ID, localId)
                        .append(PAGE_ID, pageId)
                        .append(LANGUAGE, lang)),
                new Document(
                    MONGO_LOOKUP,
                    new Document(MONGO_FROM, TranslationResource.class.getSimpleName())
                        .append(AppConstants.MONGO_LOCAL_FIELD, AppConstants.MONGO_RESOURCE_REF_ID)
                        .append(AppConstants.MONGO_FOREIGN_FIELD, DOC_ID)
                        .append(MONGO_AS, RESOURCE))))
        .iterator()
        .tryNext();
  }

  public UpdateResult updateAnnoPage(TranslationAnnoPage annoPage) {
    MongoCollection<TranslationAnnoPage> collection =
        datastore.getMapper().getCollection(TranslationAnnoPage.class);
    return collection.updateOne(
        new Document(
            Map.of(
                DATASET_ID,
                annoPage.getDsId(),
                LOCAL_ID,
                annoPage.getLcId(),
                PAGE_ID,
                annoPage.getPgId(),
                LANGUAGE,
                annoPage.getLang())),
        new Document(
            "$set",
            new Document(ANNOTATIONS, annoPage.getAns())
                .append(MODIFIED, annoPage.getModified())
                .append(SOURCE, annoPage.getSource())));
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

  public DeleteResult deleteAnnoPages(String datasetId, String localId, String pageId) {
    return datastore
        .getDatabase()
        .getCollection(TranslationAnnoPage.class.getSimpleName())
        .deleteMany(
            new Document(DATASET_ID, datasetId).append(LOCAL_ID, localId).append(PAGE_ID, pageId));
  }

  public DeleteResult deleteAnnoPage(String datasetId, String localId, String pageId, String lang) {
    return datastore
        .getDatabase()
        .getCollection(TranslationAnnoPage.class.getSimpleName())
        .deleteOne(
            new Document(DATASET_ID, datasetId)
                .append(LOCAL_ID, localId)
                .append(PAGE_ID, pageId)
                .append(LANGUAGE, lang));
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

  public TranslationAnnoPage getAnnoPageWithSource(String source, boolean fetchFullDoc) {
    FindOptions findOptions = new FindOptions().limit(1);

    if (!fetchFullDoc) {
      findOptions =
          findOptions.projection().include(DATASET_ID, LOCAL_ID, PAGE_ID, TARGET_ID, SOURCE);
    }

    return datastore
        .find(TranslationAnnoPage.class)
        .filter(eq(SOURCE, source))
        .iterator(findOptions)
        .tryNext();
  }
}
