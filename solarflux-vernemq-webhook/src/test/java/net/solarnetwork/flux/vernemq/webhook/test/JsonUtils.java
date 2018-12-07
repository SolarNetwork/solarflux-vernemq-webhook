
package net.solarnetwork.flux.vernemq.webhook.test;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON utilities to help with tests.
 * 
 * @author matt
 */
public final class JsonUtils {

  /**
   * Get a default {@link ObjectMapper} instance.
   * 
   * @return the new instance
   */
  public static ObjectMapper defaultObjectMapper() {
    return Jackson2ObjectMapperBuilder.json().serializationInclusion(Include.NON_EMPTY).build();
  }

}
