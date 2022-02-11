package eu.europeana.fulltextwrite.service;

import eu.europeana.fulltextwrite.config.AppSettings;
import eu.europeana.fulltextwrite.exception.AnnotationGoneException;
import eu.europeana.fulltextwrite.exception.AnnotationNotFoundException;
import eu.europeana.fulltextwrite.model.external.AnnotationItem;
import eu.europeana.fulltextwrite.model.external.AnnotationSearchResponse;
import io.netty.handler.logging.LogLevel;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
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
import org.springframework.web.util.UriBuilder;
import reactor.core.Exceptions;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Service
public class AnnotationsApiRestService {
  private final WebClient webClient;
  private static final Logger logger = LogManager.getLogger(AnnotationsApiRestService.class);

  /** Date format used by Annotation API for to and from param in deleted endpoint */
  private static final DateFormat ANNOTATION_QUERY_DATE_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

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

  public Optional<AnnotationItem> retrieveAnnotation(String annotationId)
      throws AnnotationNotFoundException {
    // add wskey to request
    String uri = annotationId + "?wskey=" + wskey;

    try {
      return Optional.ofNullable(
          webClient
              .get()
              .uri(URI.create(uri))
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              // throw custom exception so we can handle 410 and 404 responses separately

              .onStatus(
                  HttpStatus.GONE::equals,
                  response -> response.bodyToMono(String.class).map(AnnotationGoneException::new))
              .onStatus(
                  HttpStatus.NOT_FOUND::equals,
                  response ->
                      response.bodyToMono(String.class).map(AnnotationNotFoundException::new))
              .bodyToMono(AnnotationItem.class)
              .block());
    } catch (Exception e) {
      /*
       * Spring WebFlux wraps exceptions in ReactiveError (see Exceptions.propagate())
       * So we need to unwrap the underlying exception, for it to be handled by callers of this method
       **/
      Throwable t = Exceptions.unwrap(e);

      // return empty optional if annotation has been deleted on Annotation API
      if (t instanceof AnnotationGoneException) {
        return Optional.empty();
      }

      if (t instanceof AnnotationNotFoundException) {
        // rethrow Not Found error so @ControllerAdvice can handle it correctly
        throw new AnnotationNotFoundException("Annotation does not exist");
      }

      // all other exceptions should be propagated
      throw e;
    }
  }

  public List<String> getDeletedAnnotations(int page, int pageSize, Instant from, Instant to) {
    return webClient
        .get()
        .uri(buildUriForDeletedAnnotations(page, pageSize, from, to))
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
        .block();
  }

  private String generateQuery(@Nullable Instant from, @NonNull Instant to) {
    // if from is null, fetch from the earliest representable time
    String fromString = from != null ? from.toString() : "*";
    String toString = to.toString();

    String queryString = "generated:[" + fromString + " TO " + toString + "]";
    // escape colons in dates, as the colon is a special character to Solr's parser
    return queryString.replace(":", "\\:");
  }

  /**
   * Helper method for constructing request URI. Only includes "from" and "to" parameters if not
   * null.
   */
  private Function<UriBuilder, URI> buildUriForDeletedAnnotations(
      int page, int pageSize, Instant from, Instant to) {
    return uriBuilder -> {
      UriBuilder builder =
          uriBuilder
              .path("/annotations/deleted")
              .queryParam("wskey", wskey)
              .queryParam("page", page)
              .queryParam("limit", pageSize);

      if (from != null) {
        builder.queryParam("from", ANNOTATION_QUERY_DATE_FORMAT.format(Date.from(from)));
      }

      if (to != null) {
        builder.queryParam("to", ANNOTATION_QUERY_DATE_FORMAT.format(Date.from(to)));
      }

      return builder.build();
    };
  }
}
