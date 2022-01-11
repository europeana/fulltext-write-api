package eu.europeana.fulltextwrite.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.fulltextwrite.BaseIntegrationTest;
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
  public static final String BASE_SERVICE_URL = "/presentation";

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
  }

  @Test
  void fulltextSubmissionShouldBeSuccessful() throws Exception {
    String requestBody = "{}";

    mockMvc
        .perform(
            post(BASE_SERVICE_URL + "/9200338/BibliographicResource_3000094252504/annopage")
                .param(
                    WebConstants.REQUEST_VALUE_MEDIA, "https://iiif.europeana.eu/image/default.jpg")
                .param(WebConstants.REQUEST_VALUE_LANG, "en")
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk());
  }

  @Test
  void fulltextUpdateShouldBeSuccessful() throws Exception {
    String requestBody = "{}";

    mockMvc
        .perform(
            put(BASE_SERVICE_URL + "/9200338/BibliographicResource_3000094252504/annopage/1")
                .param(WebConstants.REQUEST_VALUE_LANG, "en")
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk());
  }

  @Test
  void fulltextRemovalShouldBeSuccessful() throws Exception {
    mockMvc
        .perform(
            delete(BASE_SERVICE_URL + "/9200338/BibliographicResource_3000094252504/annopage/1")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }
}
