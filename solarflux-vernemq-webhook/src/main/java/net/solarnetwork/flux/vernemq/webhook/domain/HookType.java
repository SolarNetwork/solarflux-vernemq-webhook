
package net.solarnetwork.flux.vernemq.webhook.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enumeration of VerneMQ hook types.
 * 
 * @author matt
 */
public enum HookType implements HookNames {

  @JsonProperty(AUTH_ON_REGISTER)
  AuthenticateOnRegister(AUTH_ON_REGISTER),

  @JsonProperty(AUTH_ON_SUBSCRIBE)
  AuthoriseOnSubscribe(AUTH_ON_SUBSCRIBE);

  /** The name of the HTTP header that is used to transmit the hook type. */
  public static final String HOOK_HEADER = "vernemq-hook";

  private String key;

  private HookType(String key) {
    this.key = key;
  }

  /**
   * Returns the {@code key} value.
   */
  @Override
  public String toString() {
    return key;
  }

}
