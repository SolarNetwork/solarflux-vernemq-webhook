
package net.solarnetwork.flux.vernemq.webhook.domain.test;

import static com.spotify.hamcrest.jackson.IsJsonStringMatching.isJsonStringMatching;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonArray;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonText;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.flux.vernemq.webhook.domain.TopicList;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link TopicList} class.
 * 
 * @author matt
 */
public class TopicListTests extends TestSupport {

  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void toJsonFull() throws JsonProcessingException {
    TopicList list = new TopicList(Arrays.asList("foo", "bar"));
    String json = objectMapper.writeValueAsString(list);
    log.debug("Topic settings full JSON: {}", json);

    // @formatter:off
    assertThat(json, isJsonStringMatching(
        jsonArray(contains(
          jsonText("foo"),
          jsonText("bar")
        ))));
    // @formatter:on
  }

  @Test
  public void fromJson() throws IOException {
    String json = "[\"bim\",\"bam\"]";

    TopicList list = objectMapper.readValue(json, TopicList.class);
    assertThat("List size", list.getTopics(), hasSize(2));
    assertThat("Topic 1", list.getTopics().get(0), equalTo("bim"));
    assertThat("Topic 2", list.getTopics().get(1), equalTo("bam"));
  }

}
