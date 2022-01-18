package eu.europeana.fulltextwrite.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.entity.AnnoPage;
import ioinformarics.oss.jackson.module.jsonld.JsonldModule;
import ioinformarics.oss.jackson.module.jsonld.JsonldResource;
import ioinformarics.oss.jackson.module.jsonld.JsonldResourceBuilder;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class JsonLdSerializer {

  public static final String CONTEXT = "http://www.europeana.eu/schemas/context/collection.jsonld";
  public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  ObjectMapper mapper = new ObjectMapper();
  JsonldResourceBuilder<AnnoPage> annoPageJsonldResourceBuilder;

  public JsonLdSerializer() {
    SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
    mapper.setDateFormat(df);
  }

  public JsonldResourceBuilder<AnnoPage> getUserSetResourceBuilder() {
    if (annoPageJsonldResourceBuilder == null) {
      annoPageJsonldResourceBuilder = JsonldResource.Builder.create();
      annoPageJsonldResourceBuilder.context(CONTEXT);
    }
    return annoPageJsonldResourceBuilder;
  }

  /**
   * This method provides full serialization of a Anno page
   *
   * @param annoPage
   * @return full user set view
   * @throws IOException
   */
  public String serialize(AnnoPage annoPage) throws IOException {
    mapper.registerModule(new JsonldModule());
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper.writer().writeValueAsString(getUserSetResourceBuilder().build(annoPage));
  }
}
