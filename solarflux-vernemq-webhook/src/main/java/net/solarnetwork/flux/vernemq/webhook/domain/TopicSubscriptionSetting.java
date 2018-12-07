
package net.solarnetwork.flux.vernemq.webhook.domain;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A topic subscription setting.
 * 
 * @author matt
 */
@JsonPropertyOrder({ "topic", "qos" })
@JsonDeserialize(builder = TopicSubscriptionSetting.Builder.class)
public class TopicSubscriptionSetting {

  private final String topic;
  private final Qos qos;

  private TopicSubscriptionSetting(Builder builder) {
    this.topic = builder.topic;
    this.qos = builder.qos;
  }

  /**
   * Creates builder to build {@link TopicSubscriptionSetting}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link TopicSubscriptionSetting}.
   */
  public static final class Builder {

    private String topic;
    private Qos qos;

    private Builder() {
    }

    public Builder withTopic(String topic) {
      this.topic = topic;
      return this;
    }

    public Builder withQos(Qos qos) {
      this.qos = qos;
      return this;
    }

    public TopicSubscriptionSetting build() {
      return new TopicSubscriptionSetting(this);
    }
  }

  public String getTopic() {
    return topic;
  }

  public Qos getQos() {
    return qos;
  }

}
