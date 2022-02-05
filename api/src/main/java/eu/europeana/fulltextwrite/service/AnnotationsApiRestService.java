package eu.europeana.fulltextwrite.service;

import eu.europeana.fulltextwrite.config.AppSettings;
import eu.europeana.fulltextwrite.exception.AnnotationNotFoundException;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.model.external.AnnotationSearchResponse;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import io.netty.handler.logging.LogLevel;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;
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
                        .followRedirect(true)
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

  public Optional<AnnotationItem> retrieveAnnotation(String annotationId) {
    // add wskey to request
    String uri = annotationId + "?wskey=" + wskey;

    try {
      return Optional.ofNullable(
          webClient
              .get()
              .uri(URI.create(uri))
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .onStatus(
                  HttpStatus.NOT_FOUND::equals,
                  response ->
                      // throw custom exception so we can handle 404 responses separately
                      response.bodyToMono(String.class).map(AnnotationNotFoundException::new))
              .bodyToMono(AnnotationItem.class)
              .block());
    } catch (Exception e) {
      /*
       * Spring WebFlux wraps exceptions in ReactiveError (see Exceptions.propagate())
       * So we need to unwrap the underlying exception, for it to be handled by callers of this method
       **/
      Throwable t = Exceptions.unwrap(e);

      // return empty optional if annotation doesn't exist on Annotation API
      if (t instanceof AnnotationNotFoundException) {
        return Optional.empty();
      }

      // all other exceptions should be propagated
      throw e;
    }
  }

  public boolean isAnnotationDeleted(String annotationId) {
    String url = FulltextWriteUtils.getDeletedEndpoint(annotationId) + "?wskey=" + wskey;

    List<String> annotationIds =
        webClient
            .get()
            .uri(URI.create(url))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
            .block();

    if (annotationIds != null) {
      return annotationIds.contains(annotationId);
    }

    // annotation not deleted, so return false
    return false;
  }
}
