
package net.solarnetwork.flux.vernemq.webhook;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import net.solarnetwork.flux.vernemq.webhook.config.ServerConfiguration;
import net.solarnetwork.flux.vernemq.webhook.web.config.WebConfig;

/**
 * Main entry point for the SolarFlux VerneMQ webhook server.
 * 
 * @author matt
 */
@SpringBootApplication(scanBasePackageClasses = { Server.class, ServerConfiguration.class,
    WebConfig.class })
public class Server {

  private static final Logger LOG = LoggerFactory.getLogger(Server.class);

  /**
   * Command-line entry point to launching server.
   * 
   * @param args
   *        command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(Server.class, args);
  }

  /**
   * Get a command line argument processor.
   * 
   * @param ctx
   *        The application context.
   * @return The command line runner.
   */
  @Bean
  public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
    return args -> {
      if (LOG.isTraceEnabled()) {
        StringBuilder buf = new StringBuilder();
        String[] beanNames = ctx.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        for (String beanName : beanNames) {
          buf.append(beanName).append('\n');
        }
        LOG.trace("Beans provided by Spring Boot:\n{}", buf);
      }
    };
  }
}
