package eu.europeana.fulltextwrite.repository;

import static eu.europeana.fulltext.util.MorphiaUtils.Fields.*;

import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltext.entity.TranslationResource;
import eu.europeana.fulltextwrite.AppConstants;
import java.util.Date;
import java.util.List;
import org.bson.Document;

public class MongoUtils {

  private MongoUtils() {
    // private constructor to hide implicit one
  }

  public static TranslationAnnoPage processMongoDocument(
      Document document, String datasetId, String localId, String pageId, String lang) {
    if (document != null) {
      TranslationAnnoPage annoPage = new TranslationAnnoPage();
      annoPage.setDsId(datasetId);
      annoPage.setLcId(localId);
      annoPage.setPgId(pageId);
      annoPage.setLang(lang);
      annoPage.setAns((List<Annotation>) document.get(ANNOTATIONS));
      annoPage.setTgtId(String.valueOf(document.get(TARGET_ID)));
      annoPage.setSource(String.valueOf(document.get(SOURCE)));
      annoPage.setModified((Date) document.get(MODIFIED));
      // get the resource
      TranslationResource resource = new TranslationResource();
      Document res = ((List<Document>) document.get(RESOURCE)).get(0);
      resource.setId(String.valueOf(res.get(DOC_ID)));
      resource.setLang(lang);
      resource.setRights(String.valueOf(res.get(AppConstants.RIGHTS)));
      resource.setValue(String.valueOf(res.get(AppConstants.VALUE)));
      resource.setDsId(datasetId);
      resource.setLcId(localId);

      annoPage.setRes(resource);
      return annoPage;
    }
    return null;
  }
}
