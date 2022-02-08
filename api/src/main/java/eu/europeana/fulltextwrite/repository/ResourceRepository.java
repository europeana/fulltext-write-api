package eu.europeana.fulltextwrite.repository;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;
import static eu.europeana.fulltextwrite.AppConstants.FULLTEXT_DATASTORE_BEAN;

import dev.morphia.Datastore;
import eu.europeana.fulltext.entity.TranslationResource;
import eu.europeana.fulltext.util.MorphiaUtils;
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
}
