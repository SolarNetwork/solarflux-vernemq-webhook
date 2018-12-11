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

package net.solarnetwork.flux.vernemq.webhook.service.impl;

import static net.solarnetwork.flux.vernemq.webhook.Globals.AUDIT_LOG;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.solarnetwork.flux.vernemq.webhook.domain.Actor;
import net.solarnetwork.flux.vernemq.webhook.domain.Message;
import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSubscriptionSetting;
import net.solarnetwork.flux.vernemq.webhook.service.AuthorizationEvaluator;

/**
 * Basic implementation of {@link AuthorizationEvaluator}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleAuthorizationEvaluator implements AuthorizationEvaluator {

  /**
   * The default value for the {@code nodeDatumTopicTemplate} property.
   */
  public static final String DEFAULT_NODE_DATUM_TOPIC_TEMPLATE = "/node/%s/datum/%s/%s";

  /**
   * The default value for the {@code nodeDatumTopicRegex} property.
   */
  // CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINE
  public static final String DEFAULT_NODE_DATUM_TOPIC_REGEX = "/node/(\\d+|\\+)/datum(/.*)?/([^/]+)";

  private String nodeDatumTopicTemplate = DEFAULT_NODE_DATUM_TOPIC_TEMPLATE;
  private Pattern nodeDatumTopicRegex = Pattern.compile(DEFAULT_NODE_DATUM_TOPIC_REGEX);

  @Override
  public TopicSettings evaluateSubscribe(Actor actor, TopicSettings topics) {
    if (actor == null || topics == null || topics.getSettings() == null
        || topics.getSettings().isEmpty()) {
      return topics;
    }
    List<TopicSubscriptionSetting> req = topics.getSettings();
    List<TopicSubscriptionSetting> res = new ArrayList<>(req.size());
    boolean haveChange = false;
    for (TopicSubscriptionSetting s : req) {
      String topic = s.getTopic();
      Qos qos = s.getQos();
      Matcher m = nodeDatumTopicRegex.matcher(topic);
      if (!m.matches()) {
        AUDIT_LOG.info("Topic [{}] access denied to {}: invalid topic pattern", topic, actor);
        qos = Qos.NotAllowed;
      } else {
        String topicNode = m.group(1);
        String topicSource = m.group(2);
        String topicAgg = m.group(3);
        if (!topicNodeAllowed(actor, topic, topicNode)
            || !topicSourceAllowed(actor, topic, topicSource)
            || !topicAggregationAllowed(actor, topic, topicAgg)) {
          qos = Qos.NotAllowed;
        }
      }
      if (qos.equals(s.getQos()) && topic.equals(s.getTopic())) {
        // no change
        res.add(s);
      } else {
        // changed
        if (!haveChange) {
          haveChange = true;
        }
        res.add(TopicSubscriptionSetting.builder().withTopic(topic).withQos(qos).build());
      }
    }

    return (haveChange ? new TopicSettings(res) : topics);
  }

  private boolean topicNodeAllowed(Actor actor, String topic, String topicNode) {
    Set<Long> restrictedNodeIds = (actor.getPolicy() != null ? actor.getPolicy().getNodeIds()
        : null);
    if ("+".equals(topicNode) && !(restrictedNodeIds == null || restrictedNodeIds.isEmpty())) {
      // asked for wildcard nodes, but policy restricts access so deny
      AUDIT_LOG.info("Topic [{}] access denied to {}: wildcard node ID not allowed by policy",
          topic, actor);
      return false;
    } else {
      Long nodeId;
      try {
        nodeId = Long.valueOf(topicNode);
        if (!actor.getAllowedNodeIds().contains(nodeId)) {
          // requested node ID not allowed, by policy or via ownership
          AUDIT_LOG.info("Topic [{}] access denied to {}: node ID not allowed", topic, actor);
          return false;
        }
      } catch (NumberFormatException e) {
        // should not be here; deny access
        AUDIT_LOG.info("Topic [{}] access denied to {}: node ID not a number", topic, actor);
        return false;
      }
    }
    return true;
  }

  private boolean topicSourceAllowed(Actor actor, String topic, String topicSource) {
    Set<String> restrictedSourceIds = (actor.getPolicy() != null ? actor.getPolicy().getSourceIds()
        : null);
    // TODO
    return true;
  }

  private boolean topicAggregationAllowed(Actor actor, String topic, String topicAgg) {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public Message evaluatePublish(Actor actor, Message message) {
    // TODO Auto-generated method stub
    return message;
  }

  /**
   * Get the node datum topic template.
   * 
   * @return the node datum topic template; defaults to {@link #DEFAULT_NODE_DATUM_TOPIC_TEMPLATE}
   */
  public String getNodeDatumTopicTemplate() {
    return nodeDatumTopicTemplate;
  }

  /**
   * Set the node datum topic template.
   * 
   * <p>
   * This template takes the following parameters:
   * </p>
   * 
   * <ol>
   * <li>node ID</li>
   * <li>source ID</li>
   * <li>aggregation</li>
   * </ol>
   * 
   * @param nodeDatumTopicTemplate
   *        the template to use
   * @throws IllegalArgumentException
   *         if {@code nodeDatumTopicTemplate} is {@literal null}
   */
  public void setNodeDatumTopicTemplate(String nodeDatumTopicTemplate) {
    if (nodeDatumTopicTemplate == null) {
      throw new IllegalArgumentException("nodeDatumTopicTemplate must not be null");
    }
    this.nodeDatumTopicTemplate = nodeDatumTopicTemplate;
  }

  public Pattern getNodeDatumTopicRegex() {
    return nodeDatumTopicRegex;
  }

  public void setNodeDatumTopicRegex(Pattern nodeDatumTopicRegex) {
    this.nodeDatumTopicRegex = nodeDatumTopicRegex;
  }

}
