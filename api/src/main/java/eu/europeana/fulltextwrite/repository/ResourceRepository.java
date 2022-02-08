package eu.europeana.fulltextwrite.repository;

import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;
import static eu.europeana.fulltextwrite.AppConstants.FULLTEXT_DATASTORE_BEAN;

import com.mongodb.client.result.DeleteResult;
import dev.morphia.Datastore;
import eu.europeana.fulltext.entity.TranslationResource;
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

  public DeleteResult deleteResources(String datasetId, String localId) {
    return datastore
        .getDatabase()
        .getCollection(TranslationResource.class.getSimpleName())
        .deleteMany(new Document(DATASET_ID, datasetId).append(LOCAL_ID, localId));
  }

  public DeleteResult deleteResource(String datasetId, String localId, String lang) {
    return datastore
        .getDatabase()
        .getCollection(TranslationResource.class.getSimpleName())
        .deleteOne(
            new Document(DATASET_ID, datasetId).append(LOCAL_ID, localId).append(LANGUAGE, lang));
  }

  public long count() {
    return datastore.getMapper().getCollection(TranslationResource.class).countDocuments();
  }
}
