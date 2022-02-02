package eu.europeana.fulltextwrite.batch;

import static eu.europeana.fulltextwrite.AppConstants.ANNO_SYNC_TASK_EXECUTOR;
import static eu.europeana.fulltextwrite.batch.BatchUtils.ANNO_SYNC_JOB;

import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltextwrite.batch.processor.AnnotationProcessor;
import eu.europeana.fulltextwrite.batch.reader.ItemReaderConfig;
import eu.europeana.fulltextwrite.batch.writer.AnnoPageWriter;
import eu.europeana.fulltextwrite.config.AppSettings;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class AnnotationSyncJobConfig {

  private static final Logger logger = LogManager.getLogger(AnnotationSyncJobConfig.class);

  private final AppSettings appSettings;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final JobExplorer jobExplorer;

  private final ItemReaderConfig itemReaderConfig;

  private final AnnotationProcessor annotationProcessor;
  private final AnnoPageWriter annoPageWriter;

  private final TaskExecutor annoSyncTaskExecutor;

  /** SkipPolicy to ignore all failures when executing jobs, as they can be handled later */
  private static final SkipPolicy noopSkipPolicy = (Throwable t, int skipCount) -> true;

  public AnnotationSyncJobConfig(
      AppSettings appSettings,
      JobBuilderFactory jobBuilderFactory,
      StepBuilderFactory stepBuilderFactory,
      JobExplorer jobExplorer,
      ItemReaderConfig itemReaderConfig,
      AnnotationProcessor annotationProcessor,
      AnnoPageWriter annoPageWriter,
      @Qualifier(ANNO_SYNC_TASK_EXECUTOR) TaskExecutor annoSyncTaskExecutor) {
    this.appSettings = appSettings;
    this.jobBuilderFactory = jobBuilderFactory;
    this.stepBuilderFactory = stepBuilderFactory;
    this.jobExplorer = jobExplorer;
    this.itemReaderConfig = itemReaderConfig;
    this.annotationProcessor = annotationProcessor;
    this.annoPageWriter = annoPageWriter;
    this.annoSyncTaskExecutor = annoSyncTaskExecutor;
  }

  private Step syncAnnotationsStep(Instant from, Instant to) {
    return this.stepBuilderFactory
        .get("synchroniseAnnoStep")
        .<AnnotationItem, TranslationAnnoPage>chunk(appSettings.getAnnotationItemsPageSize())
        .reader(itemReaderConfig.createAnnotationReader(from, to))
        .processor(annotationProcessor)
        .writer(annoPageWriter)
        .faultTolerant()
        .skipPolicy(noopSkipPolicy)
        .taskExecutor(annoSyncTaskExecutor)
        .throttleLimit(appSettings.getAnnoSyncThrottleLimit())
        .build();
  }

  public Step deleteAnnotationsStep() {
    return this.stepBuilderFactory
        .get("deleteAnnoStep")
        .tasklet(
            (contribution, chunkContext) -> {
              logger.info("Annotations deletion yet to be implemented");
              return RepeatStatus.FINISHED;
            })
        .build();
  }

  public Job syncAnnotations() {
    Instant from = BatchUtils.getMostRecentSuccessfulStartTime(jobExplorer);
    Instant to = Instant.now();

    if (logger.isInfoEnabled()) {
      String fromLogString = from == null ? "*" : from.toString();
      logger.info(
          "Starting annotation sync job. Fetching annotations from {} to {}", fromLogString, to);
    }
    return this.jobBuilderFactory
        .get(ANNO_SYNC_JOB)
        .start(syncAnnotationsStep(from, to))
        .next(deleteAnnotationsStep())
        .build();
  }
}
