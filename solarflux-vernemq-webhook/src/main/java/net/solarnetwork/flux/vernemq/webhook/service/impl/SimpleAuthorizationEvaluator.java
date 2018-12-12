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

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.flux.vernemq.webhook.domain.Actor;
import net.solarnetwork.flux.vernemq.webhook.domain.Message;
import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSubscriptionSetting;
import net.solarnetwork.flux.vernemq.webhook.service.AuthorizationEvaluator;
import net.solarnetwork.util.StringUtils;

/**
 * Basic implementation of {@link AuthorizationEvaluator}.
 * 
 * <p>
 * This service works with topics adhering to the following syntax:
 * </p>
 * 
 * <pre>
 * <code>node/{nodeId}/datum/{sourceId}/{aggregation}</code>
 * </pre>
 * 
 * <p>
 * 
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleAuthorizationEvaluator implements AuthorizationEvaluator {

  /**
   * The default value for the {@code nodeDatumTopicRegex} property.
   */
  // CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINE
  public static final String DEFAULT_NODE_DATUM_TOPIC_REGEX = "node/(\\d+|\\+)/datum(/.*)?/([^/]+)";

  private Pattern nodeDatumTopicRegex = Pattern.compile(DEFAULT_NODE_DATUM_TOPIC_REGEX);

  @Override
  public Message evaluatePublish(Actor actor, Message message) {
    if (actor == null || message == null || message.getTopic() == null
        || message.getTopic().isEmpty()) {
      return message;
    }
    String topic = message.getTopic();
    if (!actor.isPublishAllowed()) {
      AUDIT_LOG.info("Topic [{}] access denied to {}: publish not allowed", topic, actor);
      return null;
    }
    // TODO: publish support
    return null;
  }

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
        if (!(topicNodeAllowed(actor, topic, topicNode)
            && topicSourceAllowed(actor, topic, topicSource)
            && topicAggregationAllowed(actor, topic, topicAgg))) {
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

  private PathMatcher createPathMatcher() {
    AntPathMatcher matcher = new AntPathMatcher();
    matcher.setCachePatterns(true);
    matcher.setCaseSensitive(true);
    return matcher;
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
    Set<String> policySources = (actor.getPolicy() != null ? actor.getPolicy().getSourceIds()
        : null);
    if (policySources == null || policySources.isEmpty()) {
      return true;
    }
    // to make source wildcard step * NOT match MQTT wildcard path, insert path for all #
    String topicSourceToMatch = topicSource.replaceAll("#", "#/#");
    PathMatcher pathMatcher = createPathMatcher();
    for (String policySource : policySources) {
      if (pathMatcher.isPattern(policySource)) {
        if (pathMatcher.match(policySource, topicSourceToMatch)) {
          return true;
        }
      } else if (policySource.equals(topicSource)) {
        return true;
      }
    }
    AUDIT_LOG.info("Topic [{}] access denied to {}: source policy restrictions: {}", topic, actor,
        StringUtils.commaDelimitedStringFromCollection(policySources));
    return false;
  }

  private boolean topicAggregationAllowed(Actor actor, String topic, String topicAgg) {
    Set<Aggregation> policyAggregations = (actor.getPolicy() != null
        ? actor.getPolicy().getAggregations()
        : null);
    if (policyAggregations == null || policyAggregations.isEmpty()) {
      return true;
    }
    Aggregation agg;
    try {
      agg = Aggregation.forKey(topicAgg);
    } catch (IllegalArgumentException e) {
      AUDIT_LOG.info("Topic [{}] access denied to {}: invalid aggregation [{}]", topic, actor,
          topicAgg);
      return false;
    }
    if (!policyAggregations.contains(agg)) {
      AUDIT_LOG.info("Topic [{}] access denied to {}: aggregation policy restrictions: {}", topic,
          actor, StringUtils.commaDelimitedStringFromCollection(policyAggregations));
      return false;
    }
    return true;
  }

  /**
   * Get the node datum topic regular expression.
   * 
   * @return the regular expression
   */
  public Pattern getNodeDatumTopicRegex() {
    return nodeDatumTopicRegex;
  }

  /**
   * Set the node datum topic regular expression.
   * 
   * <p>
   * This expression is matched against the topic requests, and must provide the following matching
   * groups:
   * </p>
   * 
   * <ol>
   * <li>node ID</li>
   * <li>source ID</li>
   * <li>aggregation</li>
   * </ol>
   * 
   * <p>
   * Each group should be treated as a string, to accommodate topic wild cards.
   * </p>
   * 
   * @param nodeDatumTopicRegex
   *        the regular expression to use
   * @throws IllegalArgumentException
   *         if {@code nodeDatumTopicRegex} is {@literal null}
   */
  public void setNodeDatumTopicRegex(Pattern nodeDatumTopicRegex) {
    if (nodeDatumTopicRegex == null) {
      throw new IllegalArgumentException("nodeDatumTopicRegex must not be null");
    }
    this.nodeDatumTopicRegex = nodeDatumTopicRegex;
  }

}
