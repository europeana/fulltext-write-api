package eu.europeana.fulltextwrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.IOUtils;

public class IntegrationTestUtils {

  public static final String SUBTITLE_VTT = "/subtitles/submission.vtt";

  public static final String ANNOPAGE_FILMPORTAL_1197365_JSON =
      "/annopages/annopage-filmportal-1197365.json";
  public static final String ANNOPAGE_REPOZYTORIUM_8333_JSON =
      "/annopages/annopage-repozytorium-8333.json";
  public static final String ANNOPAGE_REPOZYTORIUM_9927_JSON =
      "/annopages/annopage-repozytorium-9927.json";
  public static final String ANNOPAGE_VIMEO_208310501_JSON =
      "/annopages/annopage-vimeo-208310501.json";

  public static String loadFile(String resourcePath) throws IOException {
    return IOUtils.toString(
            Objects.requireNonNull(IntegrationTestUtils.class.getResourceAsStream(resourcePath)),
            StandardCharsets.UTF_8)
        .replace("\n", "");
  }
}
