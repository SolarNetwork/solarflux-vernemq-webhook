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

import static java.lang.System.currentTimeMillis;
import static net.solarnetwork.flux.vernemq.webhook.Globals.AUDIT_LOG;
import static net.solarnetwork.util.StringUtils.delimitedStringToMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;

import net.solarnetwork.flux.vernemq.webhook.domain.Actor;
import net.solarnetwork.flux.vernemq.webhook.domain.Message;
import net.solarnetwork.flux.vernemq.webhook.domain.Response;
import net.solarnetwork.flux.vernemq.webhook.domain.ResponseStatus;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.PublishModifiers;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.PublishRequest;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterModifiers;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterRequest;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.SubscribeRequest;
import net.solarnetwork.flux.vernemq.webhook.service.AuthService;
import net.solarnetwork.flux.vernemq.webhook.service.AuthorizationEvaluator;

/**
 * {@link AuthService} implementation that uses JDBC to authenticate and authorize requests.
 * 
 * <p>
 * Two different authorization paths are performed here, one for subscription and the other for
 * publication events. See {@link #authorizeRequest(SubscribeRequest)} and
 * {@link #authorizeRequest(PublishRequest)} for details.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcAuthService implements AuthService {

  /**
   * The password token for the signature value.
   * 
   * <p>
   * The signature must be provided as a hex-encoded value.
   * </p>
   */
  public static final String SIGNATURE_PASSWORD_TOKEN = "Signature";

  /**
   * The password token for the request date value.
   * 
   * <p>
   * The date must be provided as the number of milliseconds since the epoch.
   * </p>
   */
  public static final String DATE_PASSWORD_TOKEN = "Date";

  /**
   * The default value for the {@code snHost} property.
   */
  public static final String DEFAULT_SN_HOST = "data.solarnetwork.net";

  /**
   * The default value for the {@code snHost} property.
   */
  public static final String DEFAULT_SN_PATH = "/solarflux/auth";

  /**
   * The default value for the {@code authenticateCall} property.
   */
  // CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINE
  public static final String DEFAULT_AUTHENTICATE_CALL = "SELECT user_id,token_type,jpolicy FROM solaruser.snws2_find_verified_token_details(?,?,?,?,?)";

  /**
   * The default value for the {@code authorizeNodeCall} property.
   */
  // CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINE
  public static final String DEFAULT_AUTHORIZE_NODE_CALL = "SELECT user_id,'Node' AS token_type,NULL AS jpolicy,ARRAY[node_id] AS node_ids FROM solaruser.user_node WHERE node_id = ?";

  /**
   * The default value for the {@code authorizeCall} property.
   */
  // CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINE
  public static final String DEFAULT_AUTHORIZE_CALL = "SELECT user_id,token_type,jpolicy,node_ids FROM solaruser.user_auth_token_node_ids WHERE auth_token = ?";

  /**
   * The default value for the {@code maxDateSkew} property.
   */
  public static final long DEFAULT_MAX_DATE_SKEW = 15 * 60 * 1000L;

  /**
   * The default value for the {@code publishUsername} property.
   */
  public static final String DEFAULT_PUBLISH_USERNAME = "solarnode";

  private static final Logger log = LoggerFactory.getLogger(JdbcAuthService.class);

  private final JdbcOperations jdbcOps;
  private final AuthorizationEvaluator authEvaluator;
  private String authenticateCall = DEFAULT_AUTHENTICATE_CALL;
  private String authorizeNodeCall = DEFAULT_AUTHORIZE_NODE_CALL;
  private String authorizeCall = DEFAULT_AUTHORIZE_CALL;
  private String snHost = DEFAULT_SN_HOST;
  private String snPath = DEFAULT_SN_PATH;
  private long maxDateSkew = DEFAULT_MAX_DATE_SKEW;
  private boolean forceCleanSession = false;
  private String publishUsername = DEFAULT_PUBLISH_USERNAME;

  /**
   * Constructor.
   * 
   * @param jdbcOps
   *        the JDBC API
   * @param authEvaluator
   *        the authorization evaluator to use
   */
  public JdbcAuthService(JdbcOperations jdbcOps, AuthorizationEvaluator authEvaluator) {
    super();
    this.jdbcOps = jdbcOps;
    this.authEvaluator = authEvaluator;
  }

  private Response authorizeNodeRequest(RegisterRequest request) {
    Long nodeId;
    try {
      nodeId = Long.valueOf(request.getClientId());
    } catch (NumberFormatException e) {
      return new Response(ResponseStatus.NEXT);
    }

    Actor actor = actorForNodeId(nodeId);
    if (actor == null) {
      AUDIT_LOG.info("Access denied to node [{}]: not found", nodeId);
      return new Response(ResponseStatus.NEXT);
    }

    AUDIT_LOG.info("Authorized node [{}]", nodeId);
    if (forceCleanSession
        && (request.getCleanSession() == null || !request.getCleanSession().booleanValue())) {
      return new Response(RegisterModifiers.builder().withCleanSession(true).build());
    }
    return new Response();
  }

  @Override
  public Response authenticateRequest(RegisterRequest request) {
    final String username = request.getUsername();
    if (username == null || username.isEmpty()) {
      return new Response(ResponseStatus.NEXT);
    }

    if (publishUsername.equalsIgnoreCase(username)) {
      // NOTE: we assume that node authentication has already been performed externally,
      //       e.g. via X.509 certificate; we are simply authorizing based on the existence
      //       of the provided node ID here
      return authorizeNodeRequest(request);
    }

    final String tokenId = username;

    final Map<String, String> pwTokens = delimitedStringToMap(request.getPassword(), ",", "=");
    if (pwTokens == null || !(pwTokens.containsKey(DATE_PASSWORD_TOKEN)
        && pwTokens.containsKey(SIGNATURE_PASSWORD_TOKEN))) {
      return new Response(ResponseStatus.NEXT);
    }

    final long reqDate;
    try {
      reqDate = Long.parseLong(pwTokens.get(DATE_PASSWORD_TOKEN)) * 1000L;
    } catch (NumberFormatException e) {
      return new Response(
          "Invalid Date component [" + pwTokens.get(DATE_PASSWORD_TOKEN) + "]: " + e.getMessage());
    }

    final long reqDateSkew = Math.abs(System.currentTimeMillis() - reqDate);
    if (maxDateSkew >= 0 && reqDateSkew > maxDateSkew) {
      AUDIT_LOG.info("Access denied to [{}]: date {} skew {} > {} maximum", tokenId, reqDate,
          reqDateSkew, maxDateSkew);
      return new Response(ResponseStatus.NEXT);
    }

    final String sig = pwTokens.get(SIGNATURE_PASSWORD_TOKEN);
    if (sig.isEmpty()) {
      return new Response(ResponseStatus.NEXT);
    }

    log.debug("Authenticating [{}] @ {}{} with [{}]", tokenId, snHost, snPath,
        request.getPassword());
    List<SnTokenDetails> results = jdbcOps.query(new PreparedStatementCreator() {

      @Override
      public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
        PreparedStatement stmt = con.prepareStatement(authenticateCall, ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY);
        stmt.setString(1, tokenId);
        stmt.setTimestamp(2, new Timestamp(reqDate));
        stmt.setString(3, snHost);
        stmt.setString(4, snPath);
        stmt.setString(5, sig);
        return stmt;
      }
    }, new SnTokenDetailsRowMapper(tokenId));

    if (results == null || results.isEmpty()) {
      return new Response(ResponseStatus.NEXT);
    }

    // verify not expired
    SnTokenDetails details = results.get(0);
    if (details.getPolicy() != null && !details.getPolicy().isValidAt(currentTimeMillis())) {
      return new Response(ResponseStatus.NEXT);
    }

    // request is authenticated
    AUDIT_LOG.info("Authenticated [{}] @ {}{}", tokenId, snHost, snPath);
    if (forceCleanSession
        && (request.getCleanSession() == null || !request.getCleanSession().booleanValue())) {
      return new Response(RegisterModifiers.builder().withCleanSession(true).build());
    }
    return new Response();
  }

  private Actor actorForTokenId(String tokenId) {
    List<Actor> results = jdbcOps.query(new PreparedStatementCreator() {

      @Override
      public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
        PreparedStatement stmt = con.prepareStatement(authorizeCall, ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY);
        stmt.setString(1, tokenId);
        return stmt;
      }
    }, new ActorDetailsRowMapper(tokenId));

    return (results != null && !results.isEmpty() ? results.get(0) : null);
  }

  private Actor actorForNodeId(Long nodeId) {
    List<Actor> results = jdbcOps.query(new PreparedStatementCreator() {

      @Override
      public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
        PreparedStatement stmt = con.prepareStatement(authorizeNodeCall,
            ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setLong(1, nodeId);
        return stmt;
      }
    }, new ActorDetailsRowMapper(null));

    return (results != null && !results.isEmpty() ? results.get(0) : null);
  }

  /**
   * Authorize a publish request.
   * 
   * <p>
   * This implementation assumes the {@code clientId} is the node ID. The {@code username} must be
   * {@literal solarnode} (case insensitive).
   * </p>
   */
  @Override
  public Response authorizeRequest(PublishRequest request) {
    final String username = request.getUsername();
    if (!publishUsername.equalsIgnoreCase(username)) {
      return new Response(ResponseStatus.NEXT);
    }
    final String clientId = request.getClientId();
    final Long nodeId;
    try {
      nodeId = Long.valueOf(clientId);
    } catch (NumberFormatException e) {
      return new Response(ResponseStatus.NEXT);
    }

    log.debug("Authorizing publish request for node {}", request);
    Actor actor = actorForNodeId(nodeId);
    if (actor == null) {
      return new Response(ResponseStatus.NEXT);
    }

    // verify not expired
    if (actor.getPolicy() != null && !actor.getPolicy().isValidAt(currentTimeMillis())) {
      return new Response(ResponseStatus.NEXT);
    }

    Message result = authEvaluator.evaluatePublish(actor, request);
    if (result == null) {
      return new Response(ResponseStatus.NEXT);
    } else if (result == request) {
      return new Response();
    }

    // @formatter:off
    PublishModifiers mods = PublishModifiers.builder()
        .withTopic(result.getTopic())
        .withQos(result.getQos())
        .withPayload(result.getPayload())
        .withRetain(result.getRetain())
        .build();
    // @formatter:on
    return new Response(mods);
  }

  /**
   * Authorize a subscribe request.
   * 
   * <p>
   * This implementation assumes the {@code username} is a SolarNetwork security token ID. Topics
   * will be restricted to only use valid for the associated token.
   * </p>
   */
  @Override
  public Response authorizeRequest(SubscribeRequest request) {
    final String tokenId = request.getUsername();
    if (tokenId == null || tokenId.isEmpty()) {
      return new Response(ResponseStatus.NEXT);
    }

    log.debug("Authorizing subscribe request {}", request);
    Actor actor = actorForTokenId(tokenId);
    if (actor == null) {
      return new Response(ResponseStatus.NEXT);
    }

    // verify not expired
    if (actor.getPolicy() != null && !actor.getPolicy().isValidAt(currentTimeMillis())) {
      return new Response(ResponseStatus.NEXT);
    }

    TopicSettings result = authEvaluator.evaluateSubscribe(actor, request.getTopics());
    if (result == null) {
      return new Response(ResponseStatus.NEXT);
    } else if (result == request.getTopics()) {
      return new Response();
    }

    return new Response(result);
  }

  /**
   * Get the configured authenticate JDBC call.
   * 
   * @return the authenticate JDBC call; defaults to {@link #DEFAULT_AUTHENTICATE_CALL}
   */
  public String getAuthenticateCall() {
    return authenticateCall;
  }

  /**
   * Set the authenticate JDBC call to use.
   * 
   * <p>
   * This JDBC statement is used to authenticate requests. This JDBC statement is expected to take
   * the following parameters:
   * </p>
   * 
   * <ol>
   * <li><b>token_id</b> ({@code String}) - the SolarNetwork security token ID</li>
   * <li><b>req_date</b> ({@link Timestamp}) - the request date</li>
   * <li><b>host</b> ({@code String}) - the SolarNetwork host</li>
   * <li><b>path</b> ({@code String}) - the request path</li>
   * <li><b>signature</b> ({@code String}) - the hex-encoded SolarNetwork token signature</li>
   * </ol>
   * 
   * <p>
   * If the credentials match, a result set with the following columns is expected to be returned:
   * </p>
   * 
   * <ol>
   * <li><b>user_id</b> ({@code Long}) - the SolarNetwork user ID that owns the token</li>
   * <li><b>token_type</b> ({@code String}) - the SolarNetwork token type, e.g.
   * {@literal ReadNodeData}</li>
   * <li><b>jpolicy</b> ({@code String}) - the SolarNetwork security policy associated with the
   * token</li>
   * </ol>
   * 
   * <p>
   * If the credentials do not match, an empty result set is expected.
   * </p>
   * 
   * @param jdbcCall
   *        the JDBC call
   * @throws IllegalArgumentException
   *         if {@code jdbcCall} is {@literal null}
   */
  public void setAuthenticateCall(String jdbcCall) {
    if (jdbcCall == null) {
      throw new IllegalArgumentException("jdbcCall must not be null");
    }
    this.authenticateCall = jdbcCall;
  }

  /**
   * Get the authorization JDBC call to use.
   * 
   * @return the JDBC call; defaults to {@link #DEFAULT_AUTHORIZE_CALL}
   */
  public String getAuthorizeCall() {
    return authorizeCall;
  }

  /**
   * Set the authorization JDBC call to use.
   * 
   * <p>
   * This JDBC statement is used to authorize publish/subscribe requests. This JDBC statement is
   * expected to take the following parameters:
   * </p>
   * 
   * <ol>
   * <li><b>token_id</b> ({@code String}) - the SolarNetwork security token ID</li>
   * </ol>
   * 
   * <p>
   * A result set with the following columns is expected to be returned:
   * </p>
   * 
   * <ol>
   * <li><b>user_id</b> ({@code Long}) - the SolarNetwork user ID that owns the token</li>
   * <li><b>token_type</b> ({@code String}) - the SolarNetwork token type, e.g.
   * {@literal ReadNodeData}</li>
   * <li><b>jpolicy</b> ({@code String}) - the SolarNetwork security policy associated with the
   * token</li>
   * <li><b>node_ids</b> ({@code Long[]}) - an array of SolarNode IDs valid for this actor</li>
   * </ol>
   * 
   * <p>
   * If no token is available for {@code token_id}, an empty result set is expected.
   * </p>
   * 
   * @param jdbcCall
   *        the JDBC call
   * @throws IllegalArgumentException
   *         if {@code jdbcCall} is {@literal null}
   */
  public void setAuthorizeCall(String jdbcCall) {
    if (jdbcCall == null) {
      throw new IllegalArgumentException("jdbcCall must not be null");
    }
    this.authorizeCall = jdbcCall;
  }

  /**
   * Get the configured SolarNetwork host.
   * 
   * @return the host; defaults to {@link #DEFAULT_SN_HOST}
   */
  public String getSnHost() {
    return snHost;
  }

  /**
   * Set the SolarNetwork host to use.
   * 
   * @param snHost
   *        the host
   * @throws IllegalArgumentException
   *         if {@code snHost} is {@literal null}
   */
  public void setSnHost(String snHost) {
    if (snHost == null) {
      throw new IllegalArgumentException("snHost must not be null");
    }
    this.snHost = snHost;
  }

  /**
   * Get the configured SolarNetwork path.
   * 
   * @return the path; defaults to {@link #DEFAULT_SN_PATH}
   */
  public String getSnPath() {
    return snPath;
  }

  /**
   * Set the SolarNetwork path to use.
   * 
   * @param snPath
   *        the path
   * @throws IllegalArgumentException
   *         if {@code snPath} is {@literal null}
   */
  public void setSnPath(String snPath) {
    if (snPath == null) {
      throw new IllegalArgumentException("snPath must not be null");
    }
    this.snPath = snPath;
  }

  /**
   * Get the maximum date skew allowed during authentication.
   * 
   * @return the maximum date skew, in milliseconds; defaults to {@link #DEFAULT_MAX_DATE_SKEW}
   */
  public long getMaxDateSkew() {
    return maxDateSkew;
  }

  /**
   * Set the maximum date skew allowed during authentication.
   * 
   * @param maxDateSkew
   *        the maximum date skew, in milliseconds
   */
  public void setMaxDateSkew(long maxDateSkew) {
    this.maxDateSkew = maxDateSkew;
  }

  /**
   * Get the flag that forces the "clean session" setting on authentication.
   * 
   * @return {@literal true} to force the "clean session" setting; defaults to {@literal false}
   */
  public boolean isForceCleanSession() {
    return forceCleanSession;
  }

  /**
   * Toggle the flag that forces the "clean session" setting on authentication.
   * 
   * @param forceCleanSession
   *        {@literal true} to force the "clean session" setting
   */
  public void setForceCleanSession(boolean forceCleanSession) {
    this.forceCleanSession = forceCleanSession;
  }

  /**
   * Get the configured authorize node JDBC call.
   * 
   * @return the JDBC call; defaults to {@link #DEFAULT_AUTHORIZE_NODE_CALL}
   */
  public String getAuthorizeNodeCall() {
    return authorizeNodeCall;
  }

  /**
   * Set the authorize node JDBC call to use.
   * 
   * <p>
   * This JDBC statement is used to authorize node requests. This JDBC statement is expected to take
   * the following parameters:
   * </p>
   * 
   * <ol>
   * <li><b>node_Id</b> ({@code Long}) - the SolarNetwork node ID</li>
   * </ol>
   * 
   * <p>
   * A result set with the following columns is expected to be returned:
   * </p>
   * 
   * <ol>
   * <li><b>user_id</b> ({@code Long}) - the SolarNetwork user ID that owns the token</li>
   * <li><b>token_type</b> ({@code String}) - the SolarNetwork token type, e.g. {@literal Node}</li>
   * <li><b>jpolicy</b> ({@code String}) - the SolarNetwork security policy associated with the
   * node</li>
   * <li><b>node_ids</b> ({@code Long[]}) - the node ID, as an array
   * </ol>
   * 
   * <p>
   * If the given {@code nodeId} is not found, an empty result set is expected.
   * </p>
   * 
   * @param jdbcCall
   *        the JDBC call
   * @throws IllegalArgumentException
   *         if {@code jdbcCall} is {@literal null}
   */
  public void setAuthorizeNodeCall(String jdbcCall) {
    if (jdbcCall == null) {
      throw new IllegalArgumentException("jdbcCall must not be null");
    }
    this.authorizeNodeCall = jdbcCall;
  }

  /**
   * Get the publish username.
   * 
   * <p>
   * This username is required when publishing. The {@code clientId} must be a SolarNode ID.
   * </p>
   * 
   * @return the username for publishing
   */
  public String getPublishUsername() {
    return publishUsername;
  }

  /**
   * Set the username required for publishing.
   * 
   * @param publishUsername
   *        the publish username
   * @throws IllegalArgumentException
   *         if {@code publishUsername} is {@literal null}
   */
  public void setPublishUsername(String publishUsername) {
    if (publishUsername == null) {
      throw new IllegalArgumentException("publishUsername must not be null");
    }
    this.publishUsername = publishUsername;
  }

}
