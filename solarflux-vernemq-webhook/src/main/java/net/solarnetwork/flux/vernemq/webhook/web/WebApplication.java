
package net.solarnetwork.flux.vernemq.webhook.web;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

import net.solarnetwork.flux.vernemq.webhook.Server;

/**
 * {@code ServletInitializer} for container-based deployment.
 * 
 * @author matt
 */
public class WebApplication extends SpringBootServletInitializer {

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return builder.sources(Server.class);
  }

}
