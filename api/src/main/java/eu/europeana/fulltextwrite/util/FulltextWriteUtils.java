package eu.europeana.fulltextwrite.util;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltextwrite.model.edm.Reference;
import eu.europeana.fulltextwrite.web.WebConstants;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.jena.ext.com.google.common.hash.HashFunction;
import org.apache.jena.ext.com.google.common.hash.Hasher;
import org.apache.jena.ext.com.google.common.hash.Hashing;

public class FulltextWriteUtils {

  //TODO switch to message digest or something that is not deprecated
  private static HashFunction hfText = Hashing.md5();
  private static HashFunction hfAnno = Hashing.md5();

  private FulltextWriteUtils() {
    // private constructor to hide implicit one
  }

  /**
   * Generates Annotation ID
   * @param annotation
   * @return
   */
  public static String toID(eu.europeana.fulltextwrite.model.edm.Annotation annotation) {
    Hasher h = hfAnno.newHasher().putInt(annotation.getType().ordinal());
    if (annotation.hasTargets()) {
      Reference mr = annotation.getTargets().get(0);
      h.putString(mr.getURL(), Charsets.UTF_8);
    }
    String url = annotation.getTextReference().getURL();
    h.putString(url, Charsets.UTF_8);
    return h.hash().toString();
  }

  public static String toID(String itemID) {
    return (hfText.newHasher().putString(itemID, Charsets.UTF_8).hash().toString());
  }

  /**
   * Generates Annotation page url
   * @param itemID
   * @return
   */
  public static String getAnnotationPageURI(String itemID) {
    return "http://data.europeana.eu/annotation" + itemID;
  }

  /**
   * Genrated fulltext Uri
   * @param itemID
   * @param id
   * @return
   */
  public static String getFullTextResourceURI(String itemID, String id) {
    return WebConstants.BASE_FULLTEXT_URL + itemID + "/" + id;
  }

  /**
   * Generates record Id
   * @param datasetId
   * @param localId
   * @return
   */
  public static String generateRecordId(String datasetId, String localId) {
    return new StringBuilder(WebConstants.URL_SEPARATOR)
        .append(datasetId)
        .append(WebConstants.URL_SEPARATOR)
        .append(localId)
        .toString();
  }

  /**
   * Returns the Existing AnnoPage Url
   *
   * @param fulltextBaseUrl
   * @param annoPage
   * @return
   */
  public static String getAnnoPageUrl(String fulltextBaseUrl, AnnoPage annoPage) {
    StringBuilder annoPageUrl = new StringBuilder(fulltextBaseUrl);
    annoPageUrl.append(WebConstants.URL_SEPARATOR).append(WebConstants.PRESENTATION);
    annoPageUrl.append(WebConstants.URL_SEPARATOR).append(annoPage.getDsId());
    annoPageUrl.append(WebConstants.URL_SEPARATOR).append(annoPage.getLcId());
    annoPageUrl.append(WebConstants.URL_SEPARATOR).append(WebConstants.ANNOPAGE);
    annoPageUrl.append(WebConstants.URL_SEPARATOR).append(annoPage.getPgId());
    return annoPageUrl.toString();
  }

  /**
   * Creates a dummy shell record
   *
   * @param datasetId
   * @param localId
   * @param rights
   * @param lang
   * @return
   */
  public static AnnoPage createDummyAnnotation(
      String datasetId, String localId, String media, String rights, String lang) {
    Resource resource =
        new Resource(
            datasetId + localId,
            lang,
            "SEPTEMBRE.\n"
                + "Nique te ut miretur turb* > labore* „ €*ntcntus paucis lectoribus. Hor. Sat. 10» I. 1.\n"
                + "A MAESTRICHT,\n"
                + "Chez François Cavelier.",
            rights,
            datasetId,
            localId);

    List<Annotation> annotations = new ArrayList<>();
    Annotation annotation = new Annotation("annid-123", 'W', 0, 7);
    annotations.add(annotation);
    annotation = new Annotation("annid-456", 'L', 0, 7);
    annotations.add(annotation);

    AnnoPage annoPage = new AnnoPage(datasetId, localId, "1", media, lang, resource);
    annoPage.setAns(annotations);
    return annoPage;
  }

}