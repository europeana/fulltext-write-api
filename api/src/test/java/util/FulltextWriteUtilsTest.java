package util;

import static org.junit.jupiter.api.Assertions.*;

import eu.europeana.fulltextwrite.util.FulltextWriteUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FulltextWriteUtilsTest {

  @Test
  void shouldGetDsIdFromRecordId() {
    String recordId = "/08604/FDE2205EEE384218A8D986E5138F9691";
    Assertions.assertEquals("08604", FulltextWriteUtils.getDsId(recordId));
  }

  @Test
  void shouldGetLocalIdFromRecordId() {
    String recordId = "/08604/FDE2205EEE384218A8D986E5138F9691";
    assertEquals("FDE2205EEE384218A8D986E5138F9691", FulltextWriteUtils.getLocalId(recordId));
  }

  @Test
  void shouldGetRecordIdFromUri() {
    String recordUri = "http://data.europeana.eu/item/08604/EAAE870171E24F05A64CE364D750631A";
    assertEquals(
        "/08604/EAAE870171E24F05A64CE364D750631A",
        FulltextWriteUtils.getRecordIdFromUri(recordUri));
  }

  @Test
  void shouldDeriveDeletionsEndpointFromAnnotationId() {
    String annotationId = "http://annotation-api-acceptance.eanadev.org/annotation/56951";

    assertEquals(
        "http://annotation-api-acceptance.eanadev.org/annotations/deleted",
        FulltextWriteUtils.getDeletedEndpoint(annotationId));
  }
}
