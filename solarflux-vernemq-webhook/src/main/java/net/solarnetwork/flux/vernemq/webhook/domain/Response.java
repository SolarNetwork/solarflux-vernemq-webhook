
package net.solarnetwork.flux.vernemq.webhook.domain;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A webhook response object.
 * 
 * @author matt
 */
@JsonPropertyOrder(value = { "result", "modifiers" })
public class Response {

  private final ResponseStatus status;

  private final Map<Object, Object> errorStatus;

  private final ResponseModifiers modifiers;

  /**
   * Simple OK response constructor.
   */
  public Response() {
    this(ResponseStatus.OK);
  }

  /**
   * OK response constructor with modifiers.
   * 
   * @param modifiers
   *        the modifiers
   */
  public Response(ResponseModifiers modifiers) {
    this(ResponseStatus.OK, null, modifiers);
  }

  /**
   * Construct with specific status.
   * 
   * @param status
   *        the status
   */
  public Response(ResponseStatus status) {
    this(status, null, null);
  }

  /**
   * Simple ERROR response constructor.
   * 
   * @param errorMessage
   *        the error message
   */
  public Response(String errorMessage) {
    this(ResponseStatus.ERROR, errorMessage, null);
  }

  /**
   * Constructor.
   * 
   * @param status
   *        the status
   * @param message
   *        the message
   * @param modifiers
   *        the modifiers
   */
  private Response(ResponseStatus status, String message, ResponseModifiers modifiers) {
    super();
    this.status = status;
    if (message != null) {
      this.errorStatus = Collections.singletonMap(status, message);
    } else {
      this.errorStatus = null;
    }
    this.modifiers = modifiers;
  }

  @JsonProperty(value = "result")
  public Object getResult() {
    return errorStatus != null ? errorStatus : status;
  }

  @JsonIgnore
  public ResponseStatus getStatus() {
    return status;
  }

  @JsonIgnore
  public Map<Object, ?> getErrorStatus() {
    return errorStatus;
  }

  @JsonProperty(value = "modifiers")
  public ResponseModifiers getModifiers() {
    return modifiers;
  }

}
