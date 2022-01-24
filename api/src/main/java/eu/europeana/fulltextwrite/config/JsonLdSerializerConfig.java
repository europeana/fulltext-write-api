package eu.europeana.fulltextwrite.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.fulltext.entity.AnnoPage;
import ioinformarics.oss.jackson.module.jsonld.JsonldModule;
import ioinformarics.oss.jackson.module.jsonld.JsonldResource;
import ioinformarics.oss.jackson.module.jsonld.JsonldResourceBuilder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JsonLdSerializerConfig {

  public static final String CONTEXT = "http://www.europeana.eu/schemas/context/collection.jsonld";
  public final DateFormat dateFormat =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);

  @Bean
  public ObjectMapper mapper() {
    ObjectMapper mapper =
        new Jackson2ObjectMapperBuilder()
            .defaultUseWrapper(false)
            .dateFormat(dateFormat)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();
    mapper.findAndRegisterModules();
    return mapper;
  }

  @Bean
  public com.fasterxml.jackson.databind.Module jsonLdModule() {
    return new JsonldModule();
  }

  @Bean
  public JsonldResourceBuilder<AnnoPage> getUserSetResourceBuilder() {
    JsonldResourceBuilder<AnnoPage> annoPageJsonldResourceBuilder = JsonldResource.Builder.create();
    annoPageJsonldResourceBuilder.context(CONTEXT);
    return annoPageJsonldResourceBuilder;
  }
}
