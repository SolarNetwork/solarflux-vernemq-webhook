
package net.solarnetwork.flux.vernemq.webhook.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A list of topics, implementing {@link ResponseTopics}.
 * 
 * @author matt
 */
public class TopicList implements ResponseTopics {

  private final List<String> topics;

  /**
   * Constructor.
   * 
   * @param topics
   *        the topics
   */
  @JsonCreator
  public TopicList(List<String> topics) {
    super();
    this.topics = topics;
  }

  @JsonValue
  public List<String> getTopics() {
    return topics;
  }

}
