package eu.europeana.fulltextwrite.service;

import eu.europeana.fulltextwrite.config.AppSettings;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.model.external.AnnotationSearchResponse;
import io.netty.handler.logging.LogLevel;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Service
public class AnnotationsApiRestService {
  private final WebClient webClient;
  private static final Logger logger = LogManager.getLogger(AnnotationsApiRestService.class);

  private final String wskey;

  public AnnotationsApiRestService(AppSettings settings) {
    this.wskey = settings.getAnnotationsApiKey();
    this.webClient =
        WebClient.builder()
            .baseUrl(settings.getAnnotationsApiUrl())
            // used for logging Netty requests / responses.
            .clientConnector(
                new ReactorClientHttpConnector(
                    HttpClient.create()
                        .wiretap(
                            HttpClient.class.getName(),
                            LogLevel.TRACE,
                            AdvancedByteBufFormat.TEXTUAL)))
            .build();
  }

  public List<AnnotationItem> getAnnotations(int page, int pageSize, Instant from, Instant to) {
    String searchQuery = generateQuery(from, to) + " AND motivation:subtitling";
    AnnotationSearchResponse response =
        webClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/annotation/search")
                        .queryParam("query", searchQuery)
                        .queryParam("wskey", wskey)
                        .queryParam("sort", "created")
                        .queryParam("sortOrder", "asc")
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

    List<AnnotationItem> items = response.getItems();

    if (logger.isDebugEnabled()) {
      int fetchedItems = items == null ? 0 : items.size();
      logger.debug("Retrieved {} annotations; totalItems={} ", fetchedItems, response.getTotal());
    }

    return items;
  }

  private String generateQuery(@Nullable Instant from, @NonNull Instant to) {
    // if from is null, fetch from the earliest representable time
    String fromString = from != null ? from.toString() : "*";
    String toString = to.toString();

    String queryString = "generated:[" + fromString + " TO " + toString + "]";
    // escape colons in dates, as the colon is a special character to Solr's parser
    return queryString.replace(":", "\\:");
  }
}
