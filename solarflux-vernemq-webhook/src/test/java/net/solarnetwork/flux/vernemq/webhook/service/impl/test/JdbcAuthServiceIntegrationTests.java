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

import static com.spotify.hamcrest.pojo.IsPojo.pojo;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.Response;
import net.solarnetwork.flux.vernemq.webhook.domain.ResponseStatus;
import net.solarnetwork.flux.vernemq.webhook.domain.ResponseTopics;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSubscriptionSetting;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterRequest;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.SubscribeRequest;
import net.solarnetwork.flux.vernemq.webhook.service.impl.JdbcAuthService;
import net.solarnetwork.flux.vernemq.webhook.service.impl.SimpleAuthorizationEvaluator;
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
    authService = new JdbcAuthService(jdbcOps, new SimpleAuthorizationEvaluator());
  }

  private static final String password(long date, String signature) {
    return String.format("%s=%d,%s=%s", JdbcAuthService.DATE_PASSWORD_TOKEN, date,
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

  private TopicSettings topics(String... topics) {
    List<TopicSubscriptionSetting> settings = Arrays.stream(topics)
        .map(s -> TopicSubscriptionSetting.builder().withTopic(s).withQos(Qos.AtLeastOnce).build())
        .collect(toList());
    return new TopicSettings(settings);
  }

  private String createReadToken(Long userId, String tokenSecret, SecurityPolicy policy) {
    final String tokenId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20);
    DbUtils.createToken(jdbcOps, tokenId, tokenSecret, userId, true,
        DbUtils.READ_NODE_DATA_TOKEN_TYPE, policy);
    return tokenId;
  }

  @Test
  public void subscribeTokenNotAvailable() {
    // given
    final String tokenId = UUID.randomUUID().toString();

    SubscribeRequest req = SubscribeRequest.builder().withUsername(tokenId)
        .withTopics(topics("node/1/datum/foo/0")).build();

    // when
    Response r = authService.authorizeRequest(req);

    // then
    assertThat("Result", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  @Test
  public void subscribeOkNoPolicy() {
    // given
    final Long userId = 123L;
    DbUtils.createUser(jdbcOps, userId);

    final Long nodeId = 1L;
    DbUtils.createUserNode(jdbcOps, userId, nodeId);

    final String tokenId = createReadToken(userId, "secret", null);

    SubscribeRequest req = SubscribeRequest.builder().withUsername(tokenId)
        .withTopics(topics("node/1/datum/foo/0")).build();

    // when
    Response r = authService.authorizeRequest(req);

    // then
    assertThat("Result", r.getStatus(), equalTo(ResponseStatus.OK));
    assertThat("Topics not customized", r.getTopics(), nullValue());
  }

  private SecurityPolicy policyForNodesAndSources(Long[] nodes, String... sources) {
    return new BasicSecurityPolicy.Builder().withNodeIds(Arrays.stream(nodes).collect(toSet()))
        .withSourceIds(Arrays.stream(sources).collect(toSet())).build();
  }

  @Test
  public void subscribeOkWithPolicyNodeAndSource() {
    // given
    final Long userId = 123L;
    DbUtils.createUser(jdbcOps, userId);

    final Long nodeId = 1L;
    DbUtils.createUserNode(jdbcOps, userId, nodeId);

    final SecurityPolicy policy = policyForNodesAndSources(new Long[] { nodeId }, "/foo", "/bar");
    final String tokenId = createReadToken(userId, "secret", policy);

    SubscribeRequest req = SubscribeRequest.builder().withUsername(tokenId)
        .withTopics(topics("node/1/datum/foo/0", "node/1/datum/bar/0")).build();

    // when
    Response r = authService.authorizeRequest(req);

    // then
    assertThat("Result", r.getStatus(), equalTo(ResponseStatus.OK));
    assertThat("Topics not customized", r.getTopics(), nullValue());
  }

  @Test
  public void subscribeDeniedFromAuthorizedNodesOverridingPolicyNodes() {
    // given
    final Long userId = 123L;
    DbUtils.createUser(jdbcOps, userId);

    final Long nodeId = 1L;
    DbUtils.createUserNode(jdbcOps, userId, nodeId);

    final SecurityPolicy policy = policyForNodesAndSources(new Long[] { 1L, 2L }, "/foo");
    final String tokenId = createReadToken(userId, "secret", policy);

    SubscribeRequest req = SubscribeRequest.builder().withUsername(tokenId)
        .withTopics(topics("node/2/datum/foo/0")).build();

    // when
    Response r = authService.authorizeRequest(req);

    // then
    assertThat("Result", r.getStatus(), equalTo(ResponseStatus.OK));
    assertThat("Topics customized", r.getTopics(), notNullValue());

    // @formatter:off
    assertThat("Topics", r.getTopics(), 
        pojo(ResponseTopics.class)
          .withProperty("settings", contains(
              pojo(TopicSubscriptionSetting.class)
                .withProperty("topic", equalTo("node/2/datum/foo/0"))
                .withProperty("qos", equalTo(Qos.NotAllowed))
        )));
    // @formatter:on
  }

  @SuppressWarnings("unchecked")
  @Test
  public void subscribeDeniedFromPolicySource() {
    // given
    final Long userId = 123L;
    DbUtils.createUser(jdbcOps, userId);

    final Long nodeId = 1L;
    DbUtils.createUserNode(jdbcOps, userId, nodeId);

    final SecurityPolicy policy = policyForNodesAndSources(new Long[] { nodeId }, "/foo");
    final String tokenId = createReadToken(userId, "secret", policy);

    SubscribeRequest req = SubscribeRequest.builder().withUsername(tokenId)
        .withTopics(topics("node/1/datum/foo/0", "node/1/datum/bar/0")).build();

    // when
    Response r = authService.authorizeRequest(req);

    // then
    assertThat("Result", r.getStatus(), equalTo(ResponseStatus.OK));
    assertThat("Topics customized", r.getTopics(), notNullValue());

    // @formatter:off
    assertThat("Topics", r.getTopics(), 
        pojo(ResponseTopics.class)
          .withProperty("settings", contains(
              pojo(TopicSubscriptionSetting.class)
                .withProperty("topic", equalTo("node/1/datum/foo/0"))
                .withProperty("qos", equalTo(Qos.AtLeastOnce)),
              pojo(TopicSubscriptionSetting.class)
                .withProperty("topic", equalTo("node/1/datum/bar/0"))
                .withProperty("qos", equalTo(Qos.NotAllowed))
        )));
    // @formatter:on
  }
}
