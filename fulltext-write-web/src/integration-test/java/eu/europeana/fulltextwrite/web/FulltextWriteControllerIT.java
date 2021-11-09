package eu.europeana.fulltextwrite.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                    WebConstants.REQUEST_VALUE_MEDIA,
                    "https://iiif.europeana.eu/image/KK5LTYOHAXSTDXDUBG7JGXBEBBGS7WDLZNOBS4QF426UOOYUEB5Q/presentation_images/b9ac63a0-02ce-11e6-a651-fa163e2dd531/node-1/image/SUBHH/Neue_Hamburger_Zeitung/1896/04/17/00000001/full/full/0/default.jpg")
                .param(WebConstants.REQUEST_VALUE_LANG, "en")
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk());
  }
}
