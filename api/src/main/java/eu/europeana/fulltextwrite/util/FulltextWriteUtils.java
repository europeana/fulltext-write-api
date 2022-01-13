package eu.europeana.fulltextwrite.util;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Resource;
import eu.europeana.fulltextwrite.web.WebConstants;
import java.util.ArrayList;
import java.util.List;

public class FulltextWriteUtils {

  /**
   * Returns the Existing AnnoPage Url
   *
   * @param fulltextBaseUrl
   * @param annoPage
   * @return
   */
  public String getAnnoPageUrl(String fulltextBaseUrl, AnnoPage annoPage) {
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
  public AnnoPage createDummyAnnotation(
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
