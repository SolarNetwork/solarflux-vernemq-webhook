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

  private SecurityPolicy policyForSources(String... sources) {
    return new BasicSecurityPolicy.Builder().withSourceIds(Arrays.stream(sources).collect(toSet()))
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
    TopicSettings request = requestForTopics("node/2/datum/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed via ownership", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeNoPolicyDeniedNode() {
    ActorDetails actor = actor(null, 2L);
    TopicSettings request = requestForTopics("node/3/datum/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic denied via ownership", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/3/datum/foo/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyAllowedNodeNoRestriction() {
    SecurityPolicy policy = new BasicSecurityPolicy.Builder().build();
    ActorDetails actor = actor(policy, 2L);
    TopicSettings request = requestForTopics("node/2/datum/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed via ownership and empty policy restriction", 
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyAllowedNodeWithRestriction() {
    SecurityPolicy policy = policyForNodes(2L);
    ActorDetails actor = actor(policy, 2L);
    TopicSettings request = requestForTopics("node/2/datum/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed via policy restriction", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyDeniedNodeWithRestriction() {
    SecurityPolicy policy = policyForNodes(3L);
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic denied via policy restriction", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceAllowed() {
    SecurityPolicy policy = policyForSources("/foo");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source restriction", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceDenied() {
    SecurityPolicy policy = policyForSources("/foo");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/bar/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic deinied via source restriction", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/bar/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceWildStepDenied() {
    SecurityPolicy policy = policyForSources("/foo");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/+/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic deinied via source restriction", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/+/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceWildPathDenied() {
    SecurityPolicy policy = policyForSources("/foo");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/#/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic deinied via source restriction", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/#/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceWildCharAllowed() {
    SecurityPolicy policy = policyForSources("/fo?");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source restriction", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceWildCharDenied() {
    SecurityPolicy policy = policyForSources("/fo?");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/boo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic deinied via source restriction", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/boo/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithMultilevelSourceAllowed() {
    SecurityPolicy policy = policyForSources("/foo/bar/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/bar/bam/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with multi-level source restriction", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/bar/bam/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithMultilevelSourceDenied() {
    SecurityPolicy policy = policyForSources("/foo/bar/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/bim/bam/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic denied via multi-level source restriction", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/bim/bam/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithMultilevelSourceWildStepDenied() {
    SecurityPolicy policy = policyForSources("/foo/bar/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/bim/+/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic denied via multi-level source restriction", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/bim/+/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithMultilevelSourceWildPathDenied() {
    SecurityPolicy policy = policyForSources("/foo/bar/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/#/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic denied via multi-level source restriction", result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/#/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceSimpleStepPatternAllowed() {
    SecurityPolicy policy = policyForSources("/*");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with simple source wildcard restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceSimpleStepPatternDenied() {
    SecurityPolicy policy = policyForSources("/*");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/z/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic denied via simple source wildcard restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/z/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceStartStepPatternAllowed() {
    SecurityPolicy policy = policyForSources("/*/bar/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/z/bar/bam/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source start step wildcard restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/z/bar/bam/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceStartStepPatternDenied() {
    SecurityPolicy policy = policyForSources("/*/bar/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/z/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic denied via source start wildcard restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/z/foo/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceMiddleStepPatternAllowed() {
    SecurityPolicy policy = policyForSources("/foo/*/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/z/bam/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source middle wildcard restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/z/bam/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceMiddleStepPatternWildStepAllowed() {
    SecurityPolicy policy = policyForSources("/foo/*/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/+/bam/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source middle wildcard restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/+/bam/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceMiddleStepPatternWildPathDenied() {
    SecurityPolicy policy = policyForSources("/foo/*/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/#/bam/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source middle wildcard restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/#/bam/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceMiddleStepPatternDenied() {
    SecurityPolicy policy = policyForSources("/foo/*/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/z/bim/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic denied via source middle wildcard restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/z/bim/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceEndStepPatternAllowed() {
    SecurityPolicy policy = policyForSources("/foo/bar/*");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/bar/z/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source end step wildcard restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/bar/z/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceEndStepPatternWildStepAllowed() {
    SecurityPolicy policy = policyForSources("/foo/bar/*");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/bar/+/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source end step wildcard restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/bar/+/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceEndStepPatternWildPathDenied() {
    SecurityPolicy policy = policyForSources("/foo/bar/*");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/bar/#/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source end step wildcard restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/bar/#/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceEndStepPatternDenied() {
    SecurityPolicy policy = policyForSources("/foo/bar/*");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/bim/z/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic denied via source end step wildcard restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/bim/z/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourcePathPatternSingleStepAllowed() {
    SecurityPolicy policy = policyForSources("/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source path wildcard single step restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourcePathPatternMultiStepAllowed() {
    SecurityPolicy policy = policyForSources("/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/bar/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source path wildcard multi step restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/bar/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourcePathPatternWildStepAllowed() {
    SecurityPolicy policy = policyForSources("/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/+/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source path wildcard single step restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/+/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourcePathPatternWildPathAllowed() {
    SecurityPolicy policy = policyForSources("/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/#/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source path wildcard single step restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/#/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourcePathPatternComplexWildPathAllowed() {
    SecurityPolicy policy = policyForSources("/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/*/foo/#/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source path wildcard single step restriction",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/*/foo/#/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourcePrefixedPathPatternAllowed() {
    SecurityPolicy policy = policyForSources("/foo/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/bar/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source path wildcard",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/bar/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourcePrefixedPathPatternDenied() {
    SecurityPolicy policy = policyForSources("/foo/**");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/z/bar/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic denied via source path wildcard",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/z/bar/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceComplexPathPatternAllowed() {
    SecurityPolicy policy = policyForSources("/foo/*/bar/**/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/z/bar/a/b/c/bam/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source path wildcard",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/z/bar/a/b/c/bam/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceComplexPathPatternDenied() {
    SecurityPolicy policy = policyForSources("/foo/*/bar/**/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/z/bar/a/b/c/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic denied via source path wildcard",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/z/bar/a/b/c/0"))
            .withProperty("qos", equalTo(Qos.NotAllowed)));
    // @formatter:on
  }

  @Test
  public void subscribeWithPolicyWithSourceComplexSubPathPatternAllowed() {
    SecurityPolicy policy = policyForSources("/foo/**/bam");
    ActorDetails actor = actor(policy, 2L, 3L);
    TopicSettings request = requestForTopics("node/2/datum/foo/a/*/b/bam/0");
    TopicSettings result = service.evaluateSubscribe(actor, request);
    assertThat("Result provided", result, notNullValue());
    assertThat("Topic provided", result.getSettings(), allOf(notNullValue(), hasSize(1)));
    // @formatter:off
    assertThat("Topic allowed with source path wildcard",
        result.getSettings().get(0),
        pojo(TopicSubscriptionSetting.class)
            .withProperty("topic", equalTo("node/2/datum/foo/a/*/b/bam/0"))
            .withProperty("qos", equalTo(Qos.AtLeastOnce)));
    // @formatter:on
  }

}
