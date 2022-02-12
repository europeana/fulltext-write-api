package eu.europeana.fulltextwrite.service;

import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getAnnoPageToString;
import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getDsId;
import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getLocalId;

import com.dotsub.converter.model.SubtitleItem;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.UpdateResult;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltext.entity.TranslationResource;
import eu.europeana.fulltextwrite.exception.FTWriteConversionException;
import eu.europeana.fulltextwrite.exception.InvalidFormatException;
import eu.europeana.fulltextwrite.exception.SubtitleParsingException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.SubtitleType;
import eu.europeana.fulltextwrite.model.edm.FulltextPackage;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.repository.AnnoPageRepository;
import eu.europeana.fulltextwrite.repository.ResourceRepository;
import eu.europeana.fulltextwrite.util.EDMToFulltextConverter;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FTWriteService {

  @Value("${spring.profiles.active:}")
  private String activeProfileString;

  private final AnnoPageRepository annotationRepository;
  private final ResourceRepository resourceRepository;
  private final SubtitleHandlerService subtitleHandlerService;

  private static final Logger logger = LogManager.getLogger(FTWriteService.class);

  /** Matches spring.profiles.active property in test/resource application.properties file */
  public static final String ACTIVE_TEST_PROFILE = "test";

  public FTWriteService(
      AnnoPageRepository annotationRepository,
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

  /**
   * Checks if a TranslationAnnoPage exists with the specified field combination. Uses targetId
   * instead of pageId
   */
  public boolean annoPageExistsByTgtId(
      String datasetId, String localId, String targetId, String lang) {
    return annotationRepository.annoPageExistsByTgtId(datasetId, localId, targetId, lang);
  }

  /** Checks if a TranslationAnnoPage exists with the specified dsId, lcId, pgId and lang */
  public boolean annoPageExistsByPgId(
      String datasetId, String localId, String pageId, String lang) {
    return annotationRepository.existsByPgId(datasetId, localId, pageId, lang);
  }

  /**
   * Retrieves the AnnoPage with the specified dcId, lcId, pgId and lang
   *
   * @return AnnoPage or null if none found
   */
  public TranslationAnnoPage getAnnoPageByPgId(
      String datasetId, String localId, String pgId, String lang) {
    return annotationRepository.getAnnoPageByPageIdLang(datasetId, localId, pgId, lang);
  }

  /**
   * Saves the given TranslationAnnoPage in the database.
   *
   * @param annoPage AnnoPage to save
   */
  public void saveAnnoPage(TranslationAnnoPage annoPage) {
    annotationRepository.saveAnnoPage(annoPage);
    if (annoPage.getRes() != null) {
      resourceRepository.saveResource(annoPage.getRes());
    }
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

  /**
   * Deletes AnnoPage with the specified dsId, lcId, pgId and lang values. Can delete max 1 record.
   */
  public void deleteAnnoPages(String datasetId, String localId, String pageId, String lang) {
    long resourceCount = resourceRepository.deleteResource(datasetId, localId, lang);
    long annoPageCount = annotationRepository.deleteAnnoPage(datasetId, localId, pageId, lang);
    logger.info(
        "AnnoPage and Resource with datasetId={}, localId={}, pageId={}, lang={} are deleted. resourceCount={}, annoPageCount={}",
        datasetId,
        localId,
        pageId,
        lang,
        resourceCount,
        annoPageCount);
  }

  /** Deletes AnnoPage(s) with the specified dsId, lcId and pgId. Could delete multiple records */
  public void deleteAnnoPages(String datasetId, String localId, String pageId) {
    long resourceCount = resourceRepository.deleteResources(datasetId, localId);
    long annoPageCount = annotationRepository.deleteAnnoPages(datasetId, localId, pageId);
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
      annoPageTobeUpdated = createAnnoPage(annotationPreview);
      if (StringUtils.isEmpty(annoPageTobeUpdated.getSource())
          && StringUtils.isNotEmpty(existingAnnoPage.getSource())) {
        annoPageTobeUpdated.setSource(existingAnnoPage.getSource());
      }
    }
    return annoPageTobeUpdated;
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
    Stream<TranslationResource> translationResourceStream =
        annoPageList.stream().filter(Objects::nonNull).map(TranslationAnnoPage::getRes);

    BulkWriteResult resourceWriteResult = resourceRepository.upsert(translationResourceStream);
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Saved resources to db: matched={}, modified={}, inserted={}",
          resourceWriteResult.getMatchedCount(),
          resourceWriteResult.getModifiedCount(),
          resourceWriteResult.getInsertedCount());
    }

    BulkWriteResult annoPageWriteResult = annotationRepository.upsert(annoPageList);
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Saved annoPages to db: matched={}, modified={}, inserted={}, annoPages={}",
          annoPageWriteResult.getMatchedCount(),
          annoPageWriteResult.getModifiedCount(),
          annoPageWriteResult.getInsertedCount(),
          getAnnoPageToString(annoPageList));
    }
  }

  /**
   * Checks if AnnoPage should be updated. AnnoPage will only be updated if source field is passed
   * OR if the new SRT was uploaded ie; the new subtitles were processed
   */
  private boolean isAnnoPageUpdateRequired(AnnotationPreview preview) {
    return (StringUtils.isNotEmpty(preview.getSource()) || !preview.getSubtitleItems().isEmpty());
  }

  /** Returns the number of TranslationAnnoPage records in the database */
  public long countAnnoPage() {
    return annotationRepository.count();
  }

  /** Returns the number of TranslationResource records in the database */
  public long countResource() {
    return resourceRepository.count();
  }

  /**
   * Drops the TranslationAnnoPage and TranslationResource collections. Can only be successfully
   * invoked from tests
   */
  public void dropCollections() {
    if (Arrays.stream(activeProfileString.split(",")).noneMatch(ACTIVE_TEST_PROFILE::equals)) {
      throw new IllegalStateException(
          String.format(
              "Attempting to drop collections outside testing. activeProfiles=%s",
              activeProfileString));
    }
    annotationRepository.deleteAll();
    resourceRepository.deleteAll();
  }

  /**
   * Deletes TranslationAnnoPage(s) with the specified source
   *
   * @param sources sources to query
   * @return number of deleted documents
   */
  public long deleteAnnoPagesWithSources(List<? extends String> sources) {
    long count = annotationRepository.deleteAnnoPagesWithSources(sources);
    if (logger.isDebugEnabled()) {
      logger.debug("Deleted {} AnnoPages for sources {}", count, sources);
    }

    return count;
  }

  /**
   * Gets TranslationAnnoPage with the specified source. Only identifying properties (ie. dsId,
   * lcId, pgId, tgId, lang) are populated.
   *
   * @param source source to query for
   * @return TranslationAnnoPage
   */
  public TranslationAnnoPage getShellAnnoPageBySource(String source) {
    return annotationRepository.getAnnoPageWithSource(source, false);
  }

  /** Creates an AnnoPage from the AnnotationPreview object, saving it in the database */
  public TranslationAnnoPage createAndSaveAnnoPage(AnnotationPreview annotationPreview)
      throws FTWriteConversionException {
    TranslationAnnoPage annoPage = createAnnoPage(annotationPreview);
    resourceRepository.saveResource(annoPage.getRes());
    return annotationRepository.saveAnnoPage(annoPage);
  }
}
