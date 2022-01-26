package eu.europeana.fulltextwrite.service;

import eu.europeana.fulltextwrite.config.AppSettings;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.model.external.AnnotationSearchResponse;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AnnotationsApiRestService {
  private final WebClient webClient;
  private static final Logger logger = LogManager.getLogger(AnnotationsApiRestService.class);

  private final String wskey;

  public AnnotationsApiRestService(AppSettings settings) {
    this.wskey = settings.getAnnotationsApiKey();
    this.webClient = WebClient.builder().baseUrl(settings.getAnnotationsApiUrl()).build();
  }

  public List<AnnotationItem> getAllItems(int page, int pageSize) {
    AnnotationSearchResponse response =
        webClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/annotation/search")
                        .queryParam("wskey", wskey)
                        .queryParam("query", "motivation:subtitling")
                        .queryParam("sort", "changeType")
                        .queryParam("sortOrder", "desc")
                        .queryParam("page", page)
                        .queryParam("pageSize", pageSize)
                        .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(AnnotationSearchResponse.class)
            .block();

    if (response == null) {
      logger.warn("AnnotationSearchResponse not deserialized");
      return Collections.emptyList();
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Retrieved {} items from Annotation API", response.getTotal());
    }

    return response.getItems();
  }
}
