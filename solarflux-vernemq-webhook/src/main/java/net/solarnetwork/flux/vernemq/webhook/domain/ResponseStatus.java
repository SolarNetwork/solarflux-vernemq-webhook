
package net.solarnetwork.flux.vernemq.webhook.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A response status code.
 * 
 * @author matt
 */
public enum ResponseStatus {

  @JsonProperty("ok")
  OK,

  @JsonProperty("error")
  ERROR,

  @JsonProperty("next")
  NEXT;

}
