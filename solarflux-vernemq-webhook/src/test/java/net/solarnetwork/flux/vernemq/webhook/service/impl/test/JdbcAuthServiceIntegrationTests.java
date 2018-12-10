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

package net.solarnetwork.flux.vernemq.webhook.service.impl.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import net.solarnetwork.flux.vernemq.webhook.domain.Response;
import net.solarnetwork.flux.vernemq.webhook.domain.ResponseStatus;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterRequest;
import net.solarnetwork.flux.vernemq.webhook.service.impl.JdbcAuthService;
import net.solarnetwork.flux.vernemq.webhook.test.DbUtils;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * JDBC integration tests.
 * 
 * @author matt
 * @version 1.0
 */
@RunWith(SpringRunner.class)
@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class JdbcAuthServiceIntegrationTests extends TestSupport {

  @Autowired
  private DataSource dataSource;

  private JdbcOperations jdbcOps;
  private JdbcAuthService authService;

  @Before
  public void setup() {
    jdbcOps = new JdbcTemplate(dataSource);
    authService = new JdbcAuthService(jdbcOps);
  }

  private static final String password(long date, String signature) {
    return String.format("%s=%d;%s=%s", JdbcAuthService.DATE_PASSWORD_TOKEN, date,
        JdbcAuthService.SIGNATURE_PASSWORD_TOKEN, signature);
  }

  @Test
  public void authenticateOk() {
    // given
    final Long userId = 123L;
    DbUtils.createUser(jdbcOps, userId);
    final String tokenId = "test.token";
    final String tokenSecret = "foobar";
    DbUtils.createToken(jdbcOps, tokenId, tokenSecret, userId, true,
        DbUtils.READ_NODE_DATA_TOKEN_TYPE, null);
    final long reqDate = new DateTime(2018, 12, 10, 11, 34, DateTimeZone.UTC).getMillis() / 1000L;

    RegisterRequest req = RegisterRequest.builder().withUsername(tokenId)
        .withPassword(
            password(reqDate, "924e73bef6f4a10d0c477ee2205a9dc3709967fb9627617fc8565de50507e41b"))
        .build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Result", r.getStatus(), equalTo(ResponseStatus.OK));
  }

}
