
package net.solarnetwork.flux.vernemq.webhook.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A list of topic subscription settings.
 * 
 * @author matt
 */
public class TopicSettings implements ResponseTopics {

  private final List<TopicSubscriptionSetting> settings;

  /**
   * Constructor.
   * 
   * @param settings
   *        the settings
   */
  @JsonCreator
  public TopicSettings(List<TopicSubscriptionSetting> settings) {
    super();
    this.settings = settings;
  }

  @JsonValue
  public List<TopicSubscriptionSetting> getSettings() {
    return settings;
  }

}
