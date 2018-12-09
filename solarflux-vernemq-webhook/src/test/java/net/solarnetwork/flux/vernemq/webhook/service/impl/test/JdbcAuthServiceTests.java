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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcOperations;

import net.solarnetwork.flux.vernemq.webhook.domain.Response;
import net.solarnetwork.flux.vernemq.webhook.domain.ResponseStatus;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterRequest;
import net.solarnetwork.flux.vernemq.webhook.service.impl.JdbcAuthService;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link JdbcAuthService} class.
 * 
 * @author matt
 * @version 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class JdbcAuthServiceTests extends TestSupport {

  @Mock
  private JdbcOperations jdbcOps;

  private JdbcAuthService authService;

  @Before
  public void setup() {
    authService = new JdbcAuthService(jdbcOps);
  }

  @Test
  public void authenticateUsernameMissing() {
    // given
    RegisterRequest req = RegisterRequest.builder().build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  @Test
  public void authenticateUsernameEmpty() {
    // given
    RegisterRequest req = RegisterRequest.builder().withUsername("").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  @Test
  public void authenticatePasswordMissing() {
    // given
    RegisterRequest req = RegisterRequest.builder().withUsername("token").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  @Test
  public void authenticatePasswordEmpty() {
    // given
    RegisterRequest req = RegisterRequest.builder().withUsername("token").withPassword("").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  @Test
  public void authenticatePasswordMalformedTokens() {
    // given
    RegisterRequest req = RegisterRequest.builder().withUsername("token")
        .withPassword("not a password").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  private static final String password(long date, String signature) {
    return String.format("%s=%d;%s=%s", JdbcAuthService.DATE_PASSWORD_TOKEN, date,
        JdbcAuthService.SIGNATURE_PASSWORD_TOKEN, signature);
  }

  @Test
  public void authenticatePasswordRequestDateMissing() {
    // given
    RegisterRequest req = RegisterRequest.builder().withUsername("token")
        .withPassword("Signature=010203").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  @Test
  public void authenticatePasswordRequestDateEmpty() {
    // given
    RegisterRequest req = RegisterRequest.builder().withUsername("token")
        .withPassword("Date=;Signature=010203").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Error", r.getStatus(), equalTo(ResponseStatus.ERROR));
    log.debug("Got error: {}", r.getErrorStatus());
  }

  @Test
  public void authenticatePasswordRequestDateMalformed() {
    // given
    RegisterRequest req = RegisterRequest.builder().withUsername("token")
        .withPassword("Date=foo;Signature=010203").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Error", r.getStatus(), equalTo(ResponseStatus.ERROR));
    log.debug("Got error: {}", r.getErrorStatus());
  }

  @Test
  public void authenticatePasswordSignatureMissing() {
    // given
    RegisterRequest req = RegisterRequest.builder().withUsername("token").withPassword("Date=123")
        .build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  @Test
  public void authenticatePasswordSignatureEmpty() {
    // given
    RegisterRequest req = RegisterRequest.builder().withUsername("token")
        .withPassword(password(123L, "")).build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

}
