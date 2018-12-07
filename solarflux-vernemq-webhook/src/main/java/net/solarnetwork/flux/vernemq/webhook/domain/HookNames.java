
package net.solarnetwork.flux.vernemq.webhook.domain;

/**
 * Constants for hook names.
 * 
 * <p>
 * Defining the names here allows {@link HookType} to use them in both enum definitions and JSON
 * annotations.
 * </p>
 * 
 * @author matt
 */
public interface HookNames {

  String AUTH_ON_REGISTER = "auth_on_register";

  String AUTH_ON_SUBSCRIBE = "auth_on_subscribe";

}
