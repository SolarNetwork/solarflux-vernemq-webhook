
package net.solarnetwork.flux.vernemq.webhook.domain.test;

import static com.spotify.hamcrest.jackson.IsJsonStringMatching.isJsonStringMatching;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonBoolean;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonObject;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonText;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.flux.vernemq.webhook.domain.RegisterModifiers;
import net.solarnetwork.flux.vernemq.webhook.domain.Response;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link Response} class.
 * 
 * @author matt
 */
public class ResponseTests extends TestSupport {

  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @Test
  public void jsonOkSimple() throws JsonProcessingException {
    Response r = new Response();
    String json = objectMapper.writeValueAsString(r);
    log.debug("OK simple JSON: {}", json);

    // @formatter:off
    assertThat(json, isJsonStringMatching(
        jsonObject()
          .where("result", is(jsonText("ok")))
        ));
    // @formatter:on
  }

  @Test
  public void jsonErrorSimple() throws JsonProcessingException {
    Response r = new Response("fail");
    String json = objectMapper.writeValueAsString(r);
    log.debug("Error simple JSON: {}", json);

    // @formatter:off
    assertThat(json, isJsonStringMatching(
        jsonObject()
          .where("result", is(
              jsonObject()
                .where("error", is(jsonText("fail")))
              ))
        ));
    // @formatter:on
  }

  @Test
  public void jsonOkWithModifiers() throws JsonProcessingException {
    RegisterModifiers mods = RegisterModifiers.builder().withUpgradeQos(false).build();
    Response r = new Response(mods);
    String json = objectMapper.writeValueAsString(r);
    log.debug("Ok with mods JSON: {}", json);

    // @formatter:off
    assertThat(json, isJsonStringMatching(
        jsonObject()
          .where("result", is(jsonText("ok")))
          .where("modifiers", is(jsonObject()
            .where("upgrade_qos", is(jsonBoolean(false)))    
          ))
        ));
    // @formatter:on
  }

}
