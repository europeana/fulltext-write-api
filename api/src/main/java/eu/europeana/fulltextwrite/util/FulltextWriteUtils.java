package eu.europeana.fulltextwrite.util;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltextwrite.model.edm.Reference;
import eu.europeana.fulltextwrite.web.WebConstants;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;

public class FulltextWriteUtils {

  private FulltextWriteUtils() {
    // private constructor to hide implicit one
  }

  /**
   * Generates Annotation ID
   *
   * @param annotation
   * @return
   */
  public static String generateHash(eu.europeana.fulltextwrite.model.edm.Annotation annotation) {
    StringBuilder hashInput = new StringBuilder(annotation.getType().name());
    if (annotation.hasTargets()) {
      Reference mr = annotation.getTargets().get(0);
      hashInput.append(mr.getURL());
    }

    hashInput.append(annotation.getTextReference().getURL());

    return DigestUtils.md5Hex(hashInput.toString()).toLowerCase();
  }

  public static String generateHash(String itemID) {
    return DigestUtils.md5Hex(itemID).toLowerCase();
  }

  /**
   * Generates Annotation page url
   *
   * @param itemID
   * @return
   */
  public static String getAnnotationPageURI(String itemID) {
    return "http://data.europeana.eu/annotation" + itemID;
  }

  /**
   * Genrated fulltext Uri
   *
   * @param itemID
   * @param id
   * @return
   */
  public static String getFullTextResourceURI(String itemID, String id) {
    return WebConstants.BASE_FULLTEXT_URL + itemID + "/" + id;
  }

  /**
   * Generates record Id
   *
   * @param datasetId
   * @param localId
   * @return
   */
  public static String generateRecordId(String datasetId, String localId) {
    return "/" + datasetId + "/" + localId;
  }

  public static String getDsId(String recordId) {
    return recordId.split("/")[1];
  }

  public static String getLocalId(String recordId) {
    return recordId.split("/")[2];
  }

  /**
   * Returns the Existing AnnoPage Url
   *
   * @param fulltextBaseUrl
   * @param annoPage
   * @return
   */
  public static String getAnnoPageUrl(AnnoPage annoPage) {
    return "/"
        + WebConstants.PRESENTATION
        + "/"
        + annoPage.getDsId()
        + "/"
        + annoPage.getLcId()
        + "/"
        + WebConstants.ANNOPAGE
        + "/"
        + annoPage.getPgId();
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

  public static String[] getAnnoPageToString(List<? extends AnnoPage> annoPages) {
    return annoPages.stream().map(AnnoPage::toString).toArray(String[]::new);
  }

  /** Gets the "{dsId}/{lcId}" part from an EntityId string */
  public static String getRecordIdFromUri(String recordUri) {
    // recordUri is always http://data.europeana.eu/item/{dsId}/{lcId}"
    String[] parts = recordUri.split("/");

    return "/" + parts[parts.length - 2] + "/" + parts[parts.length - 1];
  }
}
