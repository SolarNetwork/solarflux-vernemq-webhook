
package net.solarnetwork.flux.vernemq.webhook.domain.v311.test;

import static com.spotify.hamcrest.pojo.IsPojo.pojo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSubscriptionSetting;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.SubscribeRequest;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link SubscribeRequest} class.
 * 
 * @author matt
 */
public class SubscribeRequestTests extends TestSupport {

  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void parseFull() throws IOException {
    SubscribeRequest req = objectMapper.readValue(classResourceAsBytes("auth_on_subscribe-01.json"),
        SubscribeRequest.class);
    assertThat("client_id", req.getClientId(), equalTo("clientid"));
    assertThat("mountpoint", req.getMountpoint(), equalTo(""));
    assertThat("username", req.getUsername(), equalTo("username"));

    // @formatter:off
    assertThat("topics", req.getTopics(), 
        pojo(TopicSettings.class)
            .withProperty("settings", contains(
                pojo(TopicSubscriptionSetting.class)
                  .withProperty("topic", equalTo("a/b"))
                  .withProperty("qos", equalTo(Qos.AtLeastOnce)),
                pojo(TopicSubscriptionSetting.class)
                  .withProperty("topic", equalTo("c/d"))
                  .withProperty("qos", equalTo(Qos.ExactlyOnce))
            ))
    );
    // @formatter:on
  }

}
