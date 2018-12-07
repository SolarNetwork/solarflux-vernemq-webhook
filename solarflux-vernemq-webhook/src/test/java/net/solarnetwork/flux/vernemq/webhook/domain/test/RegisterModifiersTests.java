
package net.solarnetwork.flux.vernemq.webhook.domain.test;

import static com.spotify.hamcrest.jackson.IsJsonMissing.jsonMissing;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonBoolean;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonInt;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonLong;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonObject;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonText;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.flux.vernemq.webhook.domain.RegisterModifiers;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;

/**
 * Test cases for the {@link RegisterModifiers} class.
 * 
 * @author matt
 */
public class RegisterModifiersTests {

  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @Test
  public void jsonFull() {
    RegisterModifiers mods = RegisterModifiers.builder().withCleanSession(true)
        .withMaxInflightMessages(1).withMaxMessageRate(2).withMaxMessageSize(3).withRegView("foo")
        .withRetryInterval(4L).withSubscriberId("bar").withUpgradeQos(true).build();

    JsonNode json = objectMapper.valueToTree(mods);
    // @formatter:off
    assertThat(json, is(
        jsonObject()
          .where("clean_session", is(jsonBoolean(mods.getCleanSession())))
          .where("max_message_rate", is(jsonInt(mods.getMaxMessageRate())))
          .where("max_message_size", is(jsonInt(mods.getMaxMessageSize())))
          .where("max_inflight_messages", is(jsonInt(mods.getMaxInflightMessages())))
          .where("reg_view", is(jsonText(mods.getRegView())))
          .where("retry_interval", is(jsonLong(mods.getRetryInterval())))
          .where("subscriber_id", is(jsonText(mods.getSubscriberId())))
          .where("upgrade_qos", is(jsonBoolean(mods.getCleanSession())))
        ));
    // @formatter:on
  }

  @Test
  public void jsonSome() {
    RegisterModifiers mods = RegisterModifiers.builder().withMaxMessageSize(1)
        .withMaxInflightMessages(2).withRetryInterval(3L).build();

    JsonNode json = objectMapper.valueToTree(mods);
    // @formatter:off
    assertThat(json, is(
        jsonObject()
          .where("clean_session", is(jsonMissing()))
          .where("max_message_rate", is(jsonMissing()))
          .where("max_message_size", is(jsonInt(mods.getMaxMessageSize())))
          .where("max_inflight_messages", is(jsonInt(mods.getMaxInflightMessages())))
          .where("reg_view", is(jsonMissing()))
          .where("retry_interval", is(jsonLong(mods.getRetryInterval())))
          .where("subscriber_id", is(jsonMissing()))
          .where("upgrade_qos", is(jsonMissing()))
        ));
    // @formatter:on
  }

}
