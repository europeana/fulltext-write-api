package eu.europeana.fulltextwrite.batch;

import static eu.europeana.fulltextwrite.AppConstants.ANNO_SYNC_TASK_SCHEDULER;

import eu.europeana.batch.config.MongoBatchConfigurer;
import eu.europeana.fulltextwrite.config.AppSettings;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Configures the scheduling of periodic Annotation synchronisation. */
@Configuration
@PropertySources({
  @PropertySource("classpath:fulltext-write.properties"),
  @PropertySource(value = "classpath:fulltext-write.user.properties", ignoreResourceNotFound = true)
})
@ConditionalOnProperty(
    prefix = "batch.scheduling",
    value = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableScheduling
public class AnnotionSyncSchedulingConfig implements InitializingBean {

  private final AnnotationSyncJobConfig annoSyncJobConfig;

  private final String startTimeJobParam = "startTime";

  private final TaskScheduler annoSyncTaskScheduler;
  private static final Logger logger = LogManager.getLogger(AnnotionSyncSchedulingConfig.class);

  private final JobLauncher jobLauncher;
  private final int annoSyncInitialDelay;
  private final int annoSyncInterval;

  public AnnotionSyncSchedulingConfig(
      AnnotationSyncJobConfig annoSyncJobConfig,
      @Qualifier(ANNO_SYNC_TASK_SCHEDULER) TaskScheduler annoSyncTaskScheduler,
      MongoBatchConfigurer mongoBatchConfigurer,
      AppSettings appSettings)
      throws Exception {
    this.annoSyncJobConfig = annoSyncJobConfig;
    this.annoSyncTaskScheduler = annoSyncTaskScheduler;
    // use default jobLauncher
    this.jobLauncher = mongoBatchConfigurer.getJobLauncher();
    this.annoSyncInitialDelay = appSettings.getAnnoSyncInitialDelay();
    this.annoSyncInterval = appSettings.getAnnoSyncInterval();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (logger.isInfoEnabled()) {
      logger.info(
          "AnnoSync scheduling initialized – initialDelay: {}; interval: {}",
          toMinutesAndSeconds(annoSyncInitialDelay),
          toMinutesAndSeconds(annoSyncInterval));
    }

    schedulePeriodicAnnoSync();
  }

  private void schedulePeriodicAnnoSync() {
    annoSyncTaskScheduler.scheduleWithFixedDelay(
        this::runScheduledAnnoSyncJob,
        Instant.now().plusSeconds(annoSyncInitialDelay),
        Duration.ofSeconds(annoSyncInterval));
  }

  /** Periodically run full entity updates. */
  @Async
  void runScheduledAnnoSyncJob() {
    logger.info("Triggering scheduled AnnoSync job");
    try {
      jobLauncher.run(
          annoSyncJobConfig.syncAnnotations(),
          new JobParametersBuilder()
              .addDate(startTimeJobParam, Date.from(Instant.now()))
              .toJobParameters());
    } catch (Exception e) {
      logger.warn("Error running AnnoSync job", e);
    }
  }
  /** Converts Seconds to "x min, y sec" */
  private String toMinutesAndSeconds(long seconds) {
    return String.format(
        "%d min, %d sec",
        TimeUnit.SECONDS.toMinutes(seconds),
        seconds - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds)));
  }
}
