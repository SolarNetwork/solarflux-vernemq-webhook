
package net.solarnetwork.flux.vernemq.webhook.domain.test;

import static com.spotify.hamcrest.jackson.IsJsonStringMatching.isJsonStringMatching;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonInt;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;

/**
 * Test cases for the {@link Qos} enum.
 * 
 * @author matt
 */
public class QosTests {

  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @Test
  public void toJson() throws JsonProcessingException {
    for (Qos qos : Qos.values()) {
      String json = objectMapper.writeValueAsString(qos);

      // @formatter:off
      assertThat("Qos " + qos, json, isJsonStringMatching(
          jsonInt(qos.getKey())
      ));
      // @formatter:on
    }
  }

  @Test
  public void fromJson() throws IOException {
    for (Qos qos : Qos.values()) {
      String json = String.valueOf(qos.getKey());

      Qos q = objectMapper.readValue(json, Qos.class);
      assertThat("Qos " + qos, q, equalTo(qos));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void forKeyBadValue() {
    Qos.forKey(-1);
  }

}
