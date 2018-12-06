
package net.solarnetwork.flux.vernemq.webhook.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * WebMVC configuration.
 * 
 * @author matt
 */
@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**").allowedMethods("GET", "POST")
        // setting allowCredentials to false to Spring returns Access-Control-Allow-Origin: *
        .allowCredentials(false);
  }

}
