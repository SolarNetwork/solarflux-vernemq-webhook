
package net.solarnetwork.flux.vernemq.webhook.domain.v311;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;

/**
 * A subscribe or auth-subscribe request model.
 * 
 * @author matt
 */
@JsonDeserialize(builder = SubscribeRequest.Builder.class)
public class SubscribeRequest {

  @JsonProperty("client_id")
  private final String clientId;

  private final String mountpoint;

  private final String username;

  private final TopicSettings topics;

  private SubscribeRequest(Builder builder) {
    this.clientId = builder.clientId;
    this.mountpoint = builder.mountpoint;
    this.username = builder.username;
    this.topics = builder.topics;
  }

  /**
   * Creates builder to build {@link SubscribeRequest}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates builder to build {@link SubscribeRequest}, configured as a copy of another request.
   * 
   * @param request
   *        the request to copy
   * @return created builder
   */
  public static Builder builder(SubscribeRequest request) {
    // @formatter:off
    return new Builder()
        .withClientId(request.getClientId())
        .withMountpoint(request.getMountpoint())
        .withTopics(request.getTopics())
        .withUsername(request.getUsername());
    // @formatter:on
  }

  /**
   * Builder to build {@link SubscribeRequest}.
   */
  public static final class Builder {

    @JsonProperty("client_id")
    private String clientId;

    private String mountpoint;

    private String username;

    private TopicSettings topics;

    private Builder() {
    }

    public Builder withClientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder withMountpoint(String mountpoint) {
      this.mountpoint = mountpoint;
      return this;
    }

    public Builder withUsername(String username) {
      this.username = username;
      return this;
    }

    public Builder withTopics(TopicSettings topics) {
      this.topics = topics;
      return this;
    }

    public SubscribeRequest build() {
      return new SubscribeRequest(this);
    }
  }

  public String getClientId() {
    return clientId;
  }

  public String getMountpoint() {
    return mountpoint;
  }

  public String getUsername() {
    return username;
  }

  public TopicSettings getTopics() {
    return topics;
  }

}
