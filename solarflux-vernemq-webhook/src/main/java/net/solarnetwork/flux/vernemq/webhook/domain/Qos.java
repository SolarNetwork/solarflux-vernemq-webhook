
package net.solarnetwork.flux.vernemq.webhook.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A quality-of-service enumeration.
 * 
 * @author matt
 */
public enum Qos {

  AtMostOnce(0),

  AtLeastOnce(1),

  ExactlyOnce(2),

  NotAllowed(128);

  private final int key;

  private Qos(int key) {
    this.key = key;
  }

  /**
   * Get the key value.
   * 
   * @return the key
   */
  @JsonValue
  public int getKey() {
    return key;
  }

  /**
   * Get an enum from a key value.
   * 
   * @param key
   *        the key to get the enum for
   * @return the enum
   * @throws IllegalArgumentException
   *         if {@code key} is not valid
   */
  @JsonCreator
  public static Qos forKey(int key) {
    for (Qos q : Qos.values()) {
      if (q.key == key) {
        return q;
      }
    }
    throw new IllegalArgumentException("Qos key value not supported: " + key);
  }

}
