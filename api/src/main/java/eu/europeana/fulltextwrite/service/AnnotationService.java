package eu.europeana.fulltextwrite.service;

import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getAnnoPageToString;
import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getDsId;
import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getLocalId;

import com.dotsub.converter.model.SubtitleItem;
import com.mongodb.bulk.BulkWriteResult;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltextwrite.exception.FTWriteConversionException;
import eu.europeana.fulltextwrite.exception.InvalidFormatException;
import eu.europeana.fulltextwrite.exception.SubtitleParsingException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.SubtitleType;
import eu.europeana.fulltextwrite.model.edm.FulltextPackage;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.repository.AnnotationRepository;
import eu.europeana.fulltextwrite.util.EDMToFulltextConverter;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class AnnotationService {

  private final AnnotationRepository annotationRepository;
  private final SubtitleHandlerService subtitleHandlerService;

  private static final Logger logger = LogManager.getLogger(AnnotationService.class);

  public AnnotationService(
      AnnotationRepository annotationRepository, SubtitleHandlerService subtitleHandlerService) {
    this.annotationRepository = annotationRepository;
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

    TranslationAnnoPage annoPage = createAnnoPage(annotationPreview);

    // TODO will save a proper record later as a part of EA-2827
    // Keep in mind to store Resource as well and based on originallanguege - AnnoPage or
    // TranslationAnnoPage
    //    AnnoPage convertedAnnoPage =
    //        FulltextWriteUtils.createDummyAnnotation(datasetId, localId, media, rights, lang);
    return annotationRepository.saveAnnoPage(annoPage);
  }

  public void saveAnnoPage(TranslationAnnoPage annoPage) {
    annotationRepository.saveAnnoPage(annoPage);
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

  public long count() {
    return annotationRepository.count();
  }

  public void dropCollection() {
    annotationRepository.dropCollection();
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
