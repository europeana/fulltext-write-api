package eu.europeana.fulltextwrite.service;

import static eu.europeana.fulltextwrite.IntegrationTestUtils.ANNOPAGE_FILMPORTAL_1197365_JSON;
import static eu.europeana.fulltextwrite.IntegrationTestUtils.ANNOPAGE_VIMEO_208310501_JSON;
import static eu.europeana.fulltextwrite.IntegrationTestUtils.loadFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltextwrite.BaseIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FTWriteAnnotationServiceIT extends BaseIntegrationTest {

  @Autowired AnnotationService service;
  @Autowired ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    this.service.dropCollection();
  }

  @Test
  void saveAnnoPageShouldBeSuccessful() throws Exception {
    TranslationAnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), TranslationAnnoPage.class);
    service.saveAnnoPage(annoPage);
    assertEquals(1, service.count());
  }

  @Test
  void saveAnnoPageBulkShouldBeSuccessful() throws Exception {
    assertEquals(0, service.count());

    TranslationAnnoPage annoPage1 =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), TranslationAnnoPage.class);
    TranslationAnnoPage annoPage2 =
        mapper.readValue(loadFile(ANNOPAGE_VIMEO_208310501_JSON), TranslationAnnoPage.class);

    service.upsertAnnoPage(List.of(annoPage1, annoPage2));

    assertEquals(2, service.count());
  }

  @Test
  void saveAnnoPageBulkShouldUpsert() throws Exception {

    TranslationAnnoPage annoPage1 =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), TranslationAnnoPage.class);
    service.saveAnnoPage(annoPage1);

    assertEquals(1, service.count());

    // load TranslationAnnoPage again as Morphia sets _id on object during save
    TranslationAnnoPage annoPage1Copy =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), TranslationAnnoPage.class);

    // change random value in annoPage1
    Annotation newAnnotation =
        new Annotation(
            "65dacfd36f67b9b7faa983b514baf257", 'C', 851, 856, List.of(new Target(70947, 75000)));
    annoPage1Copy.getAns().add(newAnnotation);

    TranslationAnnoPage annoPage2 =
        mapper.readValue(loadFile(ANNOPAGE_VIMEO_208310501_JSON), TranslationAnnoPage.class);

    // try saving new annoPage (annoPage2) together with existing annoPage
    service.upsertAnnoPage(List.of(annoPage2, annoPage1Copy));
    assertEquals(2, service.count());
  }

  @Test
  void shouldDropCollection() throws Exception {
    TranslationAnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), TranslationAnnoPage.class);
    service.saveAnnoPage(annoPage);
    assertEquals(1, service.count());

    service.dropCollection();

    assertEquals(0, service.count());
  }
}
