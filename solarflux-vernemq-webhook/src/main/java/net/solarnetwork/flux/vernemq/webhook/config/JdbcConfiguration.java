/* ========================================================================
 * Copyright 2018 SolarNetwork Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */

package net.solarnetwork.flux.vernemq.webhook.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import net.solarnetwork.flux.vernemq.webhook.service.AuthService;
import net.solarnetwork.flux.vernemq.webhook.service.AuthorizationEvaluator;
import net.solarnetwork.flux.vernemq.webhook.service.impl.JdbcAuthService;

/**
 * Configuration for JDBC based services.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class JdbcConfiguration {

  @Value("${solarnetwork.api.host:data.solarnetwork.net}")
  private String snHost = "data.solarnetwork.net";

  @Value("${solarnewtork.api.authPath:/solarflux/auth}")
  private String snPath = "/solarflux/auth";

  @Value("${solarnetwork.api.maxDateSkew:900000}")
  private long authMaxDateSkew = JdbcAuthService.DEFAULT_MAX_DATE_SKEW;

  @Autowired
  public DataSource dataSource;

  @Autowired
  public AuthorizationEvaluator authorizationEvaluator;

  /**
   * The {@link AuthService}.
   * 
   * @return the service
   */
  @Bean
  public JdbcAuthService authService() {
    JdbcAuthService service = new JdbcAuthService(new JdbcTemplate(dataSource),
        authorizationEvaluator);
    service.setSnHost(snHost);
    service.setSnPath(snPath);
    service.setMaxDateSkew(authMaxDateSkew);
    return service;
  }

}
