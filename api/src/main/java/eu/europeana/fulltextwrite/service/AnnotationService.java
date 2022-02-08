package eu.europeana.fulltextwrite.service;

import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getAnnoPageToString;
import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getDsId;
import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getLocalId;

import com.dotsub.converter.model.SubtitleItem;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.UpdateResult;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltextwrite.exception.FTWriteConversionException;
import eu.europeana.fulltextwrite.exception.InvalidFormatException;
import eu.europeana.fulltextwrite.exception.SubtitleParsingException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.SubtitleType;
import eu.europeana.fulltextwrite.model.edm.FulltextPackage;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.repository.AnnotationRepository;
import eu.europeana.fulltextwrite.repository.ResourceRepository;
import eu.europeana.fulltextwrite.util.EDMToFulltextConverter;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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

  public AnnotationPreview createAnnotationPreview(AnnotationItem item)
      throws SubtitleParsingException, InvalidFormatException {
    SubtitleType subtitleType = SubtitleType.getValueByMimetype(item.getBody().getFormat());

    if (subtitleType == null) {
      throw new SubtitleParsingException(
          String.format(
              "Unsupported mimeType in Annotation id=%s body.format=%s",
              item.getId(), item.getBody().getFormat()));
    }

    List<SubtitleItem> subtitleItems =
        subtitleHandlerService.parseSubtitle(
            new ByteArrayInputStream(item.getBody().getValue().getBytes(StandardCharsets.UTF_8)),
            subtitleType);

    return new AnnotationPreview.Builder(
            FulltextWriteUtils.getRecordIdFromUri(item.getTarget().getScope()),
            subtitleType,
            subtitleItems)
        .setSource(item.getId())
        .setMedia(item.getTarget().getSource())
        .setLanguage(item.getBody().getLanguage())
        .setRights(item.getBody().getEdmRights())
        .build();
  }

  public TranslationAnnoPage getAnnoPage(
      String datasetId, String localId, String targetId, String lang, String pgId) {
    return annotationRepository.getAnnoPage(datasetId, localId, targetId, lang, pgId);
  }

  public boolean annoPageExists(String datasetId, String localId, String targetId, String lang) {
    return annotationRepository.annoPageExists(datasetId, localId, targetId, lang);
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
    if (logger.isDebugEnabled()) {
      logger.debug("Saved annoPage to database - {} ", annoPage);
    }
  }

  public TranslationAnnoPage createAnnoPage(AnnotationPreview annotationPreview)
      throws FTWriteConversionException {
    FulltextPackage fulltext = subtitleHandlerService.convert(annotationPreview);
    // Conversion for testing
    String recordId = annotationPreview.getRecordId();
    return EDMToFulltextConverter.getAnnoPage(
        getDsId(recordId), getLocalId(recordId), annotationPreview, fulltext);
  }

  public void upsertAnnoPage(List<? extends TranslationAnnoPage> annoPageList) {
    BulkWriteResult writeResult = annotationRepository.upsertBulk(annoPageList);
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Saved annoPages to db: matched={}, modified={}, inserted={}, annoPages={}",
          writeResult.getMatchedCount(),
          writeResult.getModifiedCount(),
          writeResult.getInsertedCount(),
          getAnnoPageToString(annoPageList));
    }
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

  public long deleteAnnoPagesWithSource(String source) {
    long count = annotationRepository.deleteAnnoPagesWithSource(source);
    if (logger.isDebugEnabled()) {
      logger.debug("Deleted {} AnnoPages for source {}", count, source);
    }

    return count;
  }

  public TranslationAnnoPage getShellAnnoPageBySource(String source) {
    return annotationRepository.getAnnoPageWithSource(source, false);
  }
}
