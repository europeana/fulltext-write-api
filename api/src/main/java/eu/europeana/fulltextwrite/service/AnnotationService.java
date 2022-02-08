package eu.europeana.fulltextwrite.service;

import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getAnnoPageToString;
import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getDsId;
import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getLocalId;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.UpdateResult;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltextwrite.exception.FTWriteConversionException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.edm.FulltextPackage;
import eu.europeana.fulltextwrite.repository.AnnotationRepository;
import eu.europeana.fulltextwrite.repository.ResourceRepository;
import eu.europeana.fulltextwrite.util.EDMToFulltextConverter;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class AnnotationService {

  private final AnnotationRepository annotationRepository;
  private final ResourceRepository resourceRepository;
  private final SubtitleHandlerService subtitleHandlerService;

  private static final Logger logger = LogManager.getLogger(AnnotationService.class);

  public AnnotationService(
      AnnotationRepository annotationRepository,
      ResourceRepository resourceRepository,
      SubtitleHandlerService subtitleHandlerService) {
    this.annotationRepository = annotationRepository;
    this.resourceRepository = resourceRepository;
    this.subtitleHandlerService = subtitleHandlerService;
  }

  public TranslationAnnoPage getAnnoPageByTargetId(
      String datasetId, String localId, String targetId, String lang) {
    return annotationRepository.getAnnoPageByTargetIdLang(datasetId, localId, targetId, lang);
  }

  public TranslationAnnoPage getAnnoPageByPageIdLang(
      String datasetId, String localId, String pageId, String lang) {
    return annotationRepository.getAnnoPageByPageIdLang(datasetId, localId, pageId, lang);
  }

  public boolean existsTranslationByPageIdLang(
      String datasetId, String localId, String pageId, String lang) {
    return annotationRepository.existsTranslationByPageIdLang(datasetId, localId, pageId, lang);
  }

  /**
   * Creates an AnnoPage from the AnnotationPreview object, saving it in the database
   *
   * @param annotationPreview
   * @return
   * @throws FTWriteConversionException
   */
  public TranslationAnnoPage createAndSaveAnnoPage(AnnotationPreview annotationPreview)
      throws FTWriteConversionException {
    TranslationAnnoPage annoPage = getAnnoPage(annotationPreview);
    resourceRepository.saveResource(annoPage.getRes());
    return annotationRepository.saveAnnoPage(annoPage);
  }

  public void saveAnnoPage(TranslationAnnoPage annoPage) {
    annotationRepository.saveAnnoPage(annoPage);
    resourceRepository.saveResource(annoPage.getRes());
    logger.info("Saved annoPage to database - {} ", annoPage);
  }

  public TranslationAnnoPage updateAnnoPage(
      AnnotationPreview annotationPreview, TranslationAnnoPage existingAnnoPage)
      throws FTWriteConversionException {
    TranslationAnnoPage annoPage = getAnnoPageToUpdate(annotationPreview, existingAnnoPage);
    resourceRepository.saveResource(annoPage.getRes());
    logger.info("Updated Resource in db : id={}", annoPage.getRes().getId());
    if (isAnnoPageUpdateRequired(annotationPreview)) {
      UpdateResult results = annotationRepository.updateAnnoPage(annoPage);
      logger.info(
          "Updated annoPage in db : dsId={}, lcId={}, pgId={}, lang={}, matched={}, modified={}",
          annoPage.getDsId(),
          annoPage.getLcId(),
          annoPage.getPgId(),
          annoPage.getLang(),
          results.getMatchedCount(),
          results.getModifiedCount());
    }
    return annoPage;
  }

  public void deleteAnnoPage(String datasetId, String localId, String pageId, String lang) {
    resourceRepository.deleteResource(datasetId, localId, lang);
    annotationRepository.deleteAnnoPage(datasetId, localId, pageId, lang);
    logger.info(
        "AnnoPage and Resource with datasetId={}, localId={}, pageId={}, lang={} are deleted",
        datasetId,
        localId,
        pageId,
        lang);
  }

  public void deleteAnnoPages(String datasetId, String localId, String pageId) {
    long resourceCount = resourceRepository.deleteResources(datasetId, localId).getDeletedCount();
    long annoPageCount =
        annotationRepository.deleteAnnoPages(datasetId, localId, pageId).getDeletedCount();
    logger.info(
        "{} AnnoPage and {} Resource with datasetId={}, localId={}, pageId={} are deleted",
        annoPageCount,
        resourceCount,
        datasetId,
        localId,
        pageId);
  }

  private TranslationAnnoPage getAnnoPageToUpdate(
      AnnotationPreview annotationPreview, TranslationAnnoPage existingAnnoPage)
      throws FTWriteConversionException {
    TranslationAnnoPage annoPageTobeUpdated = null;
    // if there is no subtitles ie; content was empty, only update rights in the resource
    if (annotationPreview.getSubtitleItems().isEmpty()) {
      annoPageTobeUpdated = existingAnnoPage;
      annoPageTobeUpdated.getRes().setRights(annotationPreview.getRights());
      // if new source value is present, add the value in annoPage
      if (StringUtils.isNotEmpty(annotationPreview.getSource())) {
        annoPageTobeUpdated.setSource(annotationPreview.getSource());
      }
    } else { // process the subtitle list and update annotations in AnnoPage. Also, rights and value
      // in Resource
      annoPageTobeUpdated = getAnnoPage(annotationPreview);
      if (StringUtils.isEmpty(annoPageTobeUpdated.getSource())
          && StringUtils.isNotEmpty(existingAnnoPage.getSource())) {
        annoPageTobeUpdated.setSource(existingAnnoPage.getSource());
      }
    }
    return annoPageTobeUpdated;
  }

  public TranslationAnnoPage getAnnoPage(AnnotationPreview annotationPreview)
      throws FTWriteConversionException {
    FulltextPackage fulltext = subtitleHandlerService.convert(annotationPreview);
    // Conversion for testing
    String recordId = annotationPreview.getRecordId();
    return EDMToFulltextConverter.getAnnoPage(
        getDsId(recordId), getLocalId(recordId), annotationPreview, fulltext);
  }

  public void saveAnnoPageBulk(List<? extends TranslationAnnoPage> annoPageList) {
    BulkWriteResult writeResult = annotationRepository.upsertBulk(annoPageList);
    logger.info(
        "Saved annoPages to db: matched={}, modified={}, inserted={}, annoPages={}",
        writeResult.getMatchedCount(),
        writeResult.getModifiedCount(),
        writeResult.getInsertedCount(),
        getAnnoPageToString(annoPageList));
  }

  /**
   * AnnoPage will only be updated if, source field is passed OR if the new SRT was uploaded ie; the
   * new subtitles were processed
   *
   * @param preview
   * @return
   */
  private boolean isAnnoPageUpdateRequired(AnnotationPreview preview) {
    return (StringUtils.isNotEmpty(preview.getSource()) || !preview.getSubtitleItems().isEmpty());
  }

  public long count() {
    return annotationRepository.count();
  }

  public long countResource() {
    return resourceRepository.count();
  }

  public void dropCollection() {
    annotationRepository.dropCollection();
    resourceRepository.dropCollection();
  }
}
