package eu.europeana.fulltextwrite.config;

import static eu.europeana.fulltextwrite.AppConstants.ANNO_SYNC_TASK_EXECUTOR;
import static eu.europeana.fulltextwrite.AppConstants.ANNO_SYNC_TASK_SCHEDULER;
import static eu.europeana.fulltextwrite.AppConstants.JOB_LAUNCHER_TASK_EXECUTOR;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Configures TaskExecutors used across the application. */
@Configuration
public class TaskExecutorConfig {

  private final AppSettings appSettings;

  public TaskExecutorConfig(AppSettings appSettings) {
    this.appSettings = appSettings;
  }

  /** Task executor used by the Spring Batch step for multi-threading */
  @Bean(ANNO_SYNC_TASK_EXECUTOR)
  public TaskExecutor annoSyncTaskExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(appSettings.getBatchCorePoolSize());
    taskExecutor.setMaxPoolSize(appSettings.getBatchMaxPoolSize());
    taskExecutor.setQueueCapacity(appSettings.getBatchQueueSize());

    return taskExecutor;
  }

  /**
   * Task executor used by the Spring Batch job launcher. Since jobs are launched via Spring
   * Scheduling, this returns a SyncTaskExecutor – so scheduling blocks while jobs are running.
   */
  @Bean(JOB_LAUNCHER_TASK_EXECUTOR)
  public TaskExecutor jobLauncherTaskExecutor() {
    // launch all Spring Batch jobs within the Spring Scheduling thread
    return new SyncTaskExecutor();
  }

  @Bean(ANNO_SYNC_TASK_SCHEDULER)
  public TaskScheduler asyncTaskScheduler() {
    return new ThreadPoolTaskScheduler();
  }
}
