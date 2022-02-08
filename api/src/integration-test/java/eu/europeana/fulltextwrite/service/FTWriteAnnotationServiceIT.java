package eu.europeana.fulltextwrite.service;

import static eu.europeana.fulltextwrite.IntegrationTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.entity.Annotation;
import eu.europeana.fulltext.entity.Target;
import eu.europeana.fulltext.entity.TranslationAnnoPage;
import eu.europeana.fulltextwrite.BaseIntegrationTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.europeana.fulltextwrite.exception.FTWriteConversionException;
import eu.europeana.fulltextwrite.model.AnnotationPreview;
import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
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
    assertEquals(1, service.countResource());
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
    assertEquals(0, service.countResource());

  }

  @Test
  void updateAnnoPageShouldBeSuccessful() throws IOException, FTWriteConversionException {
    assertEquals(0, service.count());
    assertEquals(0, service.countResource());

    // add the anno page and resource
    TranslationAnnoPage annoPage =
            mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), TranslationAnnoPage.class);
    service.saveAnnoPage(annoPage);
    assertEquals(1, service.count());
    assertEquals(1, service.countResource());

    String rights = annoPage.getRes().getRights() + "updated";
    // now update
    AnnotationPreview preview = new AnnotationPreview.Builder(FulltextWriteUtils.generateRecordId(annoPage.getDsId(),
            annoPage.getLcId()), null, new ArrayList<>()).setLanguage(annoPage.getLang())
            .setSource("https://annotation/source/value")
            .setRights(rights)
            .setMedia(annoPage.getTgtId())
            .setOriginalLang(false).build();

    TranslationAnnoPage updatedAnnopage = service.updateAnnoPage(preview, annoPage);

    // check
    assertEquals(updatedAnnopage.getSource(), "https://annotation/source/value");
    assertEquals(rights, updatedAnnopage.getRes().getRights());
    assertEquals(annoPage.getAns().size(), updatedAnnopage.getAns().size());
    assertEquals(annoPage.getLang(), updatedAnnopage.getLang());
    assertEquals(1, service.count());
    assertEquals(1, service.countResource());
  }

  @Test
  void deleteAnnoPageWithLangSuccessful() throws IOException {

    assertEquals(0, service.count());
    assertEquals(0, service.countResource());

    // add the anno page and resource
    TranslationAnnoPage annoPage =
            mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), TranslationAnnoPage.class);
    service.saveAnnoPage(annoPage);
    assertEquals(1, service.count());
    assertEquals(1, service.countResource());

    service.deleteAnnoPage(annoPage.getDsId(), annoPage.getLcId(), annoPage.getPgId(), annoPage.getLang());

    assertEquals(0, service.count());
    assertEquals(0, service.countResource());
  }

  @Test
  void deleteAnnoPageWithoutLangSuccessful() throws IOException {

    assertEquals(0, service.count());
    assertEquals(0, service.countResource());

    // add the anno page and resource with same dsId, lcId, pgId but different lang
    TranslationAnnoPage annoPage =
            mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_JSON), TranslationAnnoPage.class);
    service.saveAnnoPage(annoPage);
    annoPage = mapper.readValue(loadFile(ANNOPAGE_FILMPORTAL_1197365_EN_JSON), TranslationAnnoPage.class);
    service.saveAnnoPage(annoPage);
    assertEquals(2, service.count());
    assertEquals(2, service.countResource());

    service.deleteAnnoPages(annoPage.getDsId(), annoPage.getLcId(), annoPage.getPgId());

    assertEquals(0, service.count());
    assertEquals(0, service.countResource());
  }

}
