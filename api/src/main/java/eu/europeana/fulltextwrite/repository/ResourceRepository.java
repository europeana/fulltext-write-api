package eu.europeana.fulltextwrite.repository;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.DATASET_ID;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LANGUAGE;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.LOCAL_ID;
import static eu.europeana.fulltextwrite.AppConstants.FULLTEXT_DATASTORE_BEAN;
import static eu.europeana.fulltextwrite.AppConstants.RIGHTS;
import static eu.europeana.fulltextwrite.AppConstants.VALUE;
import static eu.europeana.fulltextwrite.repository.AnnoPageRepository.UPSERT_OPTS;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import dev.morphia.Datastore;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltext.entity.TranslationResource;
import eu.europeana.fulltext.util.MorphiaUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class ResourceRepository {

  private final Datastore datastore;

  public ResourceRepository(@Qualifier(FULLTEXT_DATASTORE_BEAN) Datastore datastore) {
    this.datastore = datastore;
  }

  /**
   * Saves a Resource to the database
   *
   * @param resource Translation Resource object to save
   * @return the saved resource document
   */
  public TranslationResource saveResource(TranslationResource resource) {
    return datastore.save(resource);
  }

  public long deleteResources(String datasetId, String localId) {
    return datastore
        .find(TranslationResource.class)
        .filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId))
        .delete(MorphiaUtils.MULTI_DELETE_OPTS)
        .getDeletedCount();
  }

  public long deleteResource(String datasetId, String localId, String lang) {
    return datastore
        .find(TranslationResource.class)
        .filter(eq(DATASET_ID, datasetId), eq(LOCAL_ID, localId), eq(LANGUAGE, lang))
        .delete()
        .getDeletedCount();
  }

  public long count() {
    return datastore.getMapper().getCollection(TranslationResource.class).countDocuments();
  }

  /** Only for tests */
  public void deleteAll() {
    datastore.find(TranslationResource.class).delete(MorphiaUtils.MULTI_DELETE_OPTS);
  }

  public BulkWriteResult upsertFromAnnoPage(List<? extends TranslationAnnoPage> annoPageList) {
    List<WriteModel<TranslationResource>> resourceUpdates = new ArrayList<>();
    for (TranslationAnnoPage annoPage : annoPageList) {
      TranslationResource res = annoPage.getRes();
      if (res == null) {
        continue;
      }

      String id = new ObjectId().toString();

      // set resId on AnnoPage, so they can be linked. Only used for new records
      res.setId(id);

      resourceUpdates.add(
          new UpdateOneModel<>(
              new Document(
                  // filter
                  Map.of(
                      DATASET_ID, res.getDsId(), LOCAL_ID, res.getLcId(), LANGUAGE, res.getLang())),
              // update doc
              new Document(
                      "$set",
                      new Document(DATASET_ID, res.getDsId())
                          .append(LOCAL_ID, res.getLcId())
                          .append(LANGUAGE, res.getLang())
                          .append(VALUE, res.getValue())
                          .append(RIGHTS, res.getRights()))
                  // only create _id for new records
                  .append("$setOnInsert", new Document("_id", id)),
              UPSERT_OPTS));
    }

    return datastore
        .getMapper()
        .getCollection(TranslationResource.class)
        .bulkWrite(resourceUpdates);
  }
}
