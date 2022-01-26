package eu.europeana.fulltextwrite.batch;

import static eu.europeana.fulltextwrite.AppConstants.ANNO_ITEM_READER;
import static eu.europeana.fulltextwrite.AppConstants.ANNO_SYNC_TASK_EXECUTOR;
import static eu.europeana.fulltextwrite.AppConstants.STEP_SYNCHRONISE_ANNOTATIONS;

import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltextwrite.batch.processor.AnnotationProcessor;
import eu.europeana.fulltextwrite.batch.writer.AnnoPageWriter;
import eu.europeana.fulltextwrite.config.AppSettings;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.service.AnnotationsApiRestService;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class AnnotationSyncJobConfig {
  private final AppSettings appSettings;

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  private final ItemReader<AnnotationItem> annotationItemItemReader;
  private final AnnotationProcessor annotationProcessor;
  private final AnnoPageWriter annoPageWriter;

  private final TaskExecutor annoSyncTaskExecutor;

  /** SkipPolicy to ignore all failures when executing jobs, as they can be handled later */
  private final SkipPolicy noopSkipPolicy = (Throwable t, int skipCount) -> true;

  public AnnotationSyncJobConfig(
      AppSettings appSettings,
      JobBuilderFactory jobBuilderFactory,
      StepBuilderFactory stepBuilderFactory,
      @Qualifier(ANNO_ITEM_READER) ItemReader<AnnotationItem> annotationItemItemReader,
      AnnotationProcessor annotationProcessor,
      AnnoPageWriter annoPageWriter,
      AnnotationsApiRestService annotationsApiRestService,
      @Qualifier(ANNO_SYNC_TASK_EXECUTOR) TaskExecutor updatesStepExecutor) {
    this.appSettings = appSettings;
    this.jobBuilderFactory = jobBuilderFactory;
    this.stepBuilderFactory = stepBuilderFactory;
    this.annotationItemItemReader = annotationItemItemReader;
    this.annotationProcessor = annotationProcessor;
    this.annoPageWriter = annoPageWriter;
    this.annoSyncTaskExecutor = updatesStepExecutor;
  }

  private Step syncAnnotations() {
    return this.stepBuilderFactory
        .get(STEP_SYNCHRONISE_ANNOTATIONS)
        .<AnnotationItem, AnnoPage>chunk(appSettings.getAnnotationItemsPageSize())
        .reader(annotationItemItemReader)
        .processor(annotationProcessor)
        .writer(annoPageWriter)
        .faultTolerant()
        .skipPolicy(noopSkipPolicy)
        .taskExecutor(annoSyncTaskExecutor)
        .build();
  }
}
