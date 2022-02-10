package eu.europeana.fulltextwrite.repository;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;
import static eu.europeana.fulltextwrite.AppConstants.FULLTEXT_DATASTORE_BEAN;
import static eu.europeana.fulltextwrite.AppConstants.RIGHTS;
import static eu.europeana.fulltextwrite.AppConstants.VALUE;
import static eu.europeana.fulltextwrite.repository.AnnoPageRepository.UPSERT_OPTS;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import dev.morphia.Datastore;
import eu.europeana.fulltext.entity.TranslationResource;
import eu.europeana.fulltext.util.MorphiaUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bson.Document;
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
  public void dropCollection() {
    datastore.getMapper().getCollection(TranslationResource.class).drop();
  }

  public BulkWriteResult upsert(Stream<TranslationResource> translationResourceStream) {
    MongoCollection<TranslationResource> resourceCollection =
        datastore.getMapper().getCollection(TranslationResource.class);

    List<WriteModel<TranslationResource>> resourceUpdates =
        translationResourceStream.map(this::createResourceUpdate).collect(Collectors.toList());

    return resourceCollection.bulkWrite(resourceUpdates);
  }

  private WriteModel<TranslationResource> createResourceUpdate(TranslationResource res) {
    Document updateDoc =
        new Document(DATASET_ID, res.getDsId())
            .append(LOCAL_ID, res.getLcId())
            .append(LANGUAGE, res.getLang())
            .append(VALUE, res.getValue())
            .append(RIGHTS, res.getRights());

    // source not always set. Prevent null field from being saved
    if (res.getSource() != null) {
      updateDoc.append(SOURCE, res.getSource());
    }
    return new UpdateOneModel<>(
        new Document(
            // filter
            Map.of(DATASET_ID, res.getDsId(), LOCAL_ID, res.getLcId(), LANGUAGE, res.getLang())),
        // update doc
        new Document("$set", updateDoc),
        UPSERT_OPTS);
  }
}
