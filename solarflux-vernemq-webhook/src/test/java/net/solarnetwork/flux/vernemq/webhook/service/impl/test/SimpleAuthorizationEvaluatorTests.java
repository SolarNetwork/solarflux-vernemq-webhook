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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.flux.vernemq.webhook.domain.ActorDetails;
import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSubscriptionSetting;
import net.solarnetwork.flux.vernemq.webhook.service.impl.SimpleAuthorizationEvaluator;

/**
 * Test cases for the {@link SimpleAuthorizationEvaluator} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleAuthorizationEvaluatorTests {

  private SimpleAuthorizationEvaluator service;

  @Before
  public void setup() {
    service = new SimpleAuthorizationEvaluator();
  }

  private SecurityPolicy policyForNodes(Long... nodes) {
    return new BasicSecurityPolicy.Builder().withNodeIds(Arrays.stream(nodes).collect(toSet()))
        .build();
  }

  private ActorDetails actor(SecurityPolicy policy, Long... nodes) {
    Set<Long> nodeIds = null;
    if (nodes != null) {
      nodeIds = Arrays.stream(nodes).collect(toSet());
    }
    return new ActorDetails(UUID.randomUUID().toString(), false, 1L, policy, nodeIds);
  }

  private TopicSettings requestForTopics(String... topics) {
    List<TopicSubscriptionSetting> settings = Arrays.stream(topics)
        .map(s -> TopicSubscriptionSetting.builder().withTopic(s).withQos(Qos.AtLeastOnce).build())
        .collect(toList());
    return new TopicSettings(settings);
  }

  @Test
  public void subscribeNoPolicyAllowedNode() {
    ActorDetails actor = actor(null, 2L);
    TopicSettings request = requestForTopics("/node/2/datum/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed via ownership", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("/node/2/datum/foo/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeNoPolicyDeniedNode() {
    ActorDetails actor = actor(null, 2L);
    TopicSettings request = requestForTopics("/node/3/datum/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic denied via ownership", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("/node/3/datum/foo/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyAllowedNodeNoRestriction() {
    SecurityPolicy policy = new BasicSecurityPolicy.Builder().build();
    ActorDetails actor = actor(policy, 2L);
    TopicSettings request = requestForTopics("/node/2/datum/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed via ownership and empty policy restriction", 
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("/node/2/datum/foo/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyAllowedNodeWithRestriction() {
    SecurityPolicy policy = policyForNodes(2L);
    ActorDetails actor = actor(policy, 2L);
    TopicSettings request = requestForTopics("/node/2/datum/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed via policy restriction", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("/node/2/datum/foo/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyDeniedNodeWithRestriction() {
    SecurityPolicy policy = policyForNodes(3L);
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("/node/2/datum/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic denied via policy restriction", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("/node/2/datum/foo/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }
}
