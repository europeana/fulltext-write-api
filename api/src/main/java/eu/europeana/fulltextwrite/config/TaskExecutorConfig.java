package eu.europeana.fulltextwrite.config;

import static eu.europeana.fulltextwrite.AppConstants.ANNO_SYNC_TASK_EXECUTOR;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TaskExecutorConfig {

  private final AppSettings appSettings;

  public TaskExecutorConfig(AppSettings appSettings) {
    this.appSettings = appSettings;
  }

  @Bean(ANNO_SYNC_TASK_EXECUTOR)
  public TaskExecutor annoSyncTaskExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(appSettings.getBatchCorePoolSize());
    taskExecutor.setMaxPoolSize(appSettings.getBatchMaxPoolSize());
    taskExecutor.setQueueCapacity(appSettings.getBatchQueueSize());

    return taskExecutor;
  }
}
