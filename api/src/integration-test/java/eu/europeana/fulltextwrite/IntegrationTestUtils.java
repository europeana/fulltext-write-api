package eu.europeana.fulltextwrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.IOUtils;

public class IntegrationTestUtils {

  public static final String SUBTITLE_VTT = "/subtitles/submission.vtt";

  public static String loadFile(String resourcePath) throws IOException {
    return IOUtils.toString(
            Objects.requireNonNull(IntegrationTestUtils.class.getResourceAsStream(resourcePath)),
            StandardCharsets.UTF_8)
        .replace("\n", "");
  }
}
