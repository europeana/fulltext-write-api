package eu.europeana.fulltextwrite.web;

import static eu.europeana.fulltextwrite.IntegrationTestUtils.ANNOPAGE_FILMPORTAL_SALEM06_JSON;
import static eu.europeana.fulltextwrite.IntegrationTestUtils.loadFileAndReplaceServerUrl;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltextwrite.BaseIntegrationTest;
import eu.europeana.fulltextwrite.IntegrationTestUtils;
import eu.europeana.fulltextwrite.service.AnnotationService;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class AnnoSyncIT extends BaseIntegrationTest {

  private static MockWebServer mockAnnotationApi;
  @Autowired ObjectMapper mapper;

  private static String serverBaseUrl;

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired AnnotationService annoPageService;
  private MockMvc mockMvc;

  private static final List<String> MOCK_DELETED_ANNOTATIONS = new ArrayList<>();

  @BeforeAll
  static void beforeAll() throws IOException {
    mockAnnotationApi = new MockWebServer();

    serverBaseUrl =
        String.format("http://%s:%s", mockAnnotationApi.getHostName(), mockAnnotationApi.getPort());

    mockAnnotationApi.setDispatcher(
        new Dispatcher() {
          // create mapper here, as we can't access the Autowired one from static method
          ObjectMapper dispatcherMapper = new ObjectMapper();

          @Override
          public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

            try {
              String path = request.getPath();
              // can't use String.equals() as path contains wskey param
              if (path.startsWith("/annotations/deleted")) {
                return new MockResponse()
                    .setBody(dispatcherMapper.writeValueAsString(MOCK_DELETED_ANNOTATIONS))
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
              }
              List<String> pathSegments = request.getRequestUrl().pathSegments();

              if (pathSegments.size() != 2) {
                // Unsupported request path
                return new MockResponse().setResponseCode(404);
              }

              // path is /annotation/<annotationId>
              String annotationId = pathSegments.get(1);

              try {
                String body =
                    IntegrationTestUtils.loadFileAndReplaceServerUrl(
                        "/annotations/" + annotationId + ".json", serverBaseUrl);
                return new MockResponse()
                    .setBody(body)
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
              } catch (IOException e) {
                // if annotation file cannot be loaded, return 404
                return new MockResponse().setResponseCode(404);
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        });
  }

  @BeforeEach
  void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    this.annoPageService.dropCollection();
  }

  @Test
  void annoSyncShouldFetchNewAnnoPage() throws Exception {
    String annotationId = serverBaseUrl + "/annotation/53696";

    String expectedTgtId = "https://vimeo.com/524898134";
    mockMvc
        .perform(
            post("/fulltext/annosync")
                .param(WebConstants.REQUEST_VALUE_SOURCE, annotationId)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.dsId", is("08604")))
        .andExpect(jsonPath("$.lcId", is("node_1680982")))
        .andExpect(jsonPath("$.tgtId", is(expectedTgtId)))
        .andExpect(jsonPath("$.lang", is("es")))
        .andExpect(jsonPath("$.pgId", is(FulltextWriteUtils.derivePageId(expectedTgtId))));
  }

  @Test
  void annoSyncShouldDeleteRemovedAnnoPage() throws Exception {
    String deletedAnnotation = serverBaseUrl + "/annotation/53707";

    // mark annotation as deleted
    MOCK_DELETED_ANNOTATIONS.add(deletedAnnotation);

    // create AnnoPage in DB (source property in JSON matches url in deleted annotations list)
    TranslationAnnoPage annoPage =
        mapper.readValue(
            loadFileAndReplaceServerUrl(ANNOPAGE_FILMPORTAL_SALEM06_JSON, serverBaseUrl),
            TranslationAnnoPage.class);
    annoPageService.saveAnnoPage(annoPage);

    mockMvc
        .perform(
            post("/fulltext/annosync")
                .param(WebConstants.REQUEST_VALUE_SOURCE, deletedAnnotation)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isAccepted());
  }
}
