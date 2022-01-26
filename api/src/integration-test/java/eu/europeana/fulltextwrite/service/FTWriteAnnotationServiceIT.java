package eu.europeana.fulltextwrite.service;

import static eu.europeana.fulltextwrite.IntegrationTestUtils.ANNOPAGE_FILMPORTAL_1197365_JSON;
import static eu.europeana.fulltextwrite.IntegrationTestUtils.ANNOPAGE_VIMEO_208310501_JSON;
import static eu.europeana.fulltextwrite.IntegrationTestUtils.loadFile;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.entity.AnnoPage;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Target;
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
    AnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);
    service.saveAnnoPage(annoPage);
    assertEquals(1, service.count());
  }

  @Test
  void saveAnnoPageBulkShouldBeSuccessful() throws Exception {
    assertEquals(0, service.count());

    AnnoPage annoPage1 =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);
    AnnoPage annoPage2 = mapper.readValue(loadFile(ANNOPAGE_VIMEO_208310501_JSON), AnnoPage.class);

    service.saveAnnoPageBulk(List.of(annoPage1, annoPage2));

    assertEquals(2, service.count());
  }

  @Test
  void saveAnnoPageBulkShouldUpsert() throws Exception {

    AnnoPage annoPage1 =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);
    service.saveAnnoPage(annoPage1);

    assertEquals(1, service.count());

    // load AnnoPage again as Morphia sets _id on object during save
    AnnoPage annoPage1Copy =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);

    // change random value in annoPage1
    Annotation newAnnotation =
        new Annotation(
            "65dacfd36f67b9b7faa983b514baf257", 'C', 851, 856, List.of(new Target(70947, 75000)));
    annoPage1Copy.getAns().add(newAnnotation);

    AnnoPage annoPage2 = mapper.readValue(loadFile(ANNOPAGE_VIMEO_208310501_JSON), AnnoPage.class);

    // try saving new annoPage (annoPage2) together with existing annoPage
    service.saveAnnoPageBulk(List.of(annoPage2, annoPage1Copy));
    assertEquals(2, service.count());
  }

  @Test
  void shouldDropCollection() throws Exception {
    AnnoPage annoPage =
        mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), AnnoPage.class);
    service.saveAnnoPage(annoPage);
    assertEquals(1, service.count());

    service.dropCollection();

    assertEquals(0, service.count());
  }
}
