package eu.europeana.fulltextwrite.service;

import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getAnnoPageToString;
import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getDsId;
import static eu.europeana.fulltextwrite.util.FulltextWriteUtils.getLocalId;

import com.mongodb.bulk.BulkWriteResult;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltextwrite.exception.FTWriteConversionException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.model.edm.FulltextPackage;
import eu.europeana.fulltextwrite.repository.AnnotationRepository;
import eu.europeana.fulltextwrite.util.EDMToFulltextConverter;
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

  public AnnoPage getAnnoPageByTargetId(String datasetId, String localId, String targetId) {
    return annotationRepository.getAnnoPageByTargetId(datasetId, localId, targetId);
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
}
