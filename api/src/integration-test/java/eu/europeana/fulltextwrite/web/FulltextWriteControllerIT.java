package eu.europeana.fulltextwrite.web;

import static eu.europeana.fulltextwrite.AppConstants.CONTENT_TYPE_VTT;
import static eu.europeana.fulltextwrite.IntegrationTestUtils.SUBTITLE_VTT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.fulltextwrite.BaseIntegrationTest;
import eu.europeana.fulltextwrite.IntegrationTestUtils;
import eu.europeana.fulltextwrite.repository.AnnotationRepository;
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
  @Autowired private AnnotationRepository repository;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    this.repository.dropCollection();
  }

  @Test
  void fulltextSubmissionShouldBeSuccessful() throws Exception {
    String requestBody = IntegrationTestUtils.loadFile(SUBTITLE_VTT);

    mockMvc
        .perform(
            post("/presentation/08604/FDE2205EEE384218A8D986E5138F9691/annopage")
                .param(WebConstants.REQUEST_VALUE_MEDIA, "https://www.filmportal.de/node/1197365")
                .param(WebConstants.REQUEST_VALUE_LANG, "nl")
                .param(
                    WebConstants.REQUEST_VALUE_RIGHTS,
                    "http://creativecommons.org/licenses/by-sa/4.0/")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(CONTENT_TYPE_VTT)
                .content(requestBody))
        .andExpect(status().isOk());

    // TODO: check DB content
  }

  @Test
  void fulltextUpdateShouldBeSuccessful() throws Exception {
    String requestBody = "{}";

    mockMvc
        .perform(
            put("/presentation/9200338/BibliographicResource_3000094252504/annopage/1")
                .param(WebConstants.REQUEST_VALUE_LANG, "en")
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk());
  }

  @Test
  void fulltextRemovalShouldBeSuccessful() throws Exception {
    mockMvc
        .perform(
            delete("/presentation/9200338/BibliographicResource_3000094252504/annopage/1")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }
}
