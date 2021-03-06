package eu.europeana.fulltextwrite.web;

import static eu.europeana.fulltextwrite.AppConstants.CONTENT_TYPE_VTT;
import static eu.europeana.fulltextwrite.IntegrationTestUtils.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltextwrite.BaseIntegrationTest;
import eu.europeana.fulltextwrite.IntegrationTestUtils;
import eu.europeana.fulltextwrite.repository.AnnoPageRepository;
import eu.europeana.fulltextwrite.repository.ResourceRepository;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
class FulltextWriteControllerIT extends BaseIntegrationTest {
  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private AnnoPageRepository annotationRepository;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired ObjectMapper mapper;

  public static final String BASE_SERVICE_URL = "/presentation";

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    this.annotationRepository.deleteAll();
    this.resourceRepository.deleteAll();
  }

  // TODO - check DB data in all test
  @Test
  void fulltextSubmissionShouldBeSuccessful() throws Exception {
    String requestBody = IntegrationTestUtils.loadFile(SUBTITLE_VTT);

    String result =
        mockMvc
            .perform(
                post("/presentation/08604/FDE2205EEE384218A8D986E5138F9691/annopage")
                    .param(
                        WebConstants.REQUEST_VALUE_MEDIA, "https://www.filmportal.de/node/1197365")
                    .param(WebConstants.REQUEST_VALUE_LANG, "nl")
                    .param(
                        WebConstants.REQUEST_VALUE_RIGHTS,
                        "http://creativecommons.org/licenses/by-sa/4.0/")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(CONTENT_TYPE_VTT)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Assertions.assertNotNull(result);
  }

  @Test
  void fulltextUpdateShouldBeSuccessfulWithoutBody() throws Exception {
    // add the anno page and resource first
    TranslationAnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), TranslationAnnoPage.class);
    annotationRepository.saveAnnoPage(annoPage);
    resourceRepository.saveResource(annoPage.getRes());

    String result =
        mockMvc
            .perform(
                put(FulltextWriteUtils.getAnnoPageUrl(annoPage))
                    .param(WebConstants.REQUEST_VALUE_LANG, annoPage.getLang())
                    .param(
                        WebConstants.REQUEST_VALUE_RIGHTS,
                        annoPage.getRes().getRights() + "updated")
                    .param(WebConstants.REQUEST_VALUE_SOURCE, "https:annotation/source/value")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Assertions.assertNotNull(result);
  }

  @Test
  void fulltextUpdateAnnoPageDoesNotExist() throws Exception {
    mockMvc
        .perform(
            put(BASE_SERVICE_URL + "/9200338/BibliographicResource_3000094252504/annopage/1")
                .param(WebConstants.REQUEST_VALUE_LANG, "es")
                .param(WebConstants.REQUEST_VALUE_RIGHTS, "rights_testing_update")
                .param(WebConstants.REQUEST_VALUE_SOURCE, "https:annotation/source/value")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void fulltextRemovalShouldBeSuccessfulWithoutLang() throws Exception {
    // add the anno page and resource first
    TranslationAnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), TranslationAnnoPage.class);
    annotationRepository.saveAnnoPage(annoPage);
    resourceRepository.saveResource(annoPage.getRes());
    mockMvc
        .perform(
            delete(FulltextWriteUtils.getAnnoPageUrl(annoPage)).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  void fulltextRemovalShouldBeSuccessfulWithLang() throws Exception {
    // add the anno page and resource first
    TranslationAnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), TranslationAnnoPage.class);
    annotationRepository.saveAnnoPage(annoPage);
    resourceRepository.saveResource(annoPage.getRes());
    mockMvc
        .perform(
            delete(FulltextWriteUtils.getAnnoPageUrl(annoPage))
                .param(WebConstants.REQUEST_VALUE_LANG, annoPage.getLang())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  void fulltextRemovalAnnopageDoesNotExist() throws Exception {
    mockMvc
        .perform(
            delete(BASE_SERVICE_URL + "/9200338/BibliographicResource_3000094252504/annopage/1")
                .param(WebConstants.REQUEST_VALUE_LANG, "it")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void submissionWithMissingRequestBodyShouldReturn400() throws Exception {
    mockMvc
        .perform(
            post("/presentation/08604/FDE2205EEE384218A8D986E5138F9691/annopage")
                .param(WebConstants.REQUEST_VALUE_MEDIA, "https://www.filmportal.de/node/1197365")
                .param(WebConstants.REQUEST_VALUE_LANG, "nl")
                .param(
                    WebConstants.REQUEST_VALUE_RIGHTS,
                    "http://creativecommons.org/licenses/by-sa/4.0/")
                .accept(MediaType.APPLICATION_JSON)
            // no contentType
            )
        .andExpect(status().isBadRequest());
  }

  @Test
  void submissionWithInvalidRequestParamShouldReturn400() throws Exception {
    String requestBody = IntegrationTestUtils.loadFile(SUBTITLE_VTT);

    mockMvc
        .perform(
            post("/presentation/08604/FDE2205EEE384218A8D986E5138F9691/annopage")
                .param(WebConstants.REQUEST_VALUE_MEDIA, "https://www.filmportal.de/node/1197365")
                .param(WebConstants.REQUEST_VALUE_LANG, "nl")
                // originalLang is boolean
                .param(WebConstants.REQUEST_VALUE_ORIGINAL_LANG, "nl")
                .param(
                    WebConstants.REQUEST_VALUE_RIGHTS,
                    "http://creativecommons.org/licenses/by-sa/4.0/")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE_VTT)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }
}
