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
}
