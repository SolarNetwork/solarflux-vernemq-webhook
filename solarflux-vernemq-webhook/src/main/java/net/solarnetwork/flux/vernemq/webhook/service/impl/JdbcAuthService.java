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

import net.solarnetwork.flux.vernemq.webhook.domain.Response;
import net.solarnetwork.flux.vernemq.webhook.domain.ResponseStatus;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterRequest;
import net.solarnetwork.flux.vernemq.webhook.service.AuthService;

/**
 * {@link AuthService} implementation that uses JDBC to authenticate and authorize requests.
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
   * The default value for the {@code jdbcCall} property.
   */
  // CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINE
  public static final String DEFAULT_AUTHENTICATE_CALL = "SELECT * FROM solaruser.snws2_find_verified_token_details(?,?,?,?,?)";

  private static final Logger log = LoggerFactory.getLogger(JdbcAuthService.class);

  private final JdbcOperations jdbcOps;
  private String authenticateCall = DEFAULT_AUTHENTICATE_CALL;
  private String snHost = DEFAULT_SN_HOST;
  private String snPath = DEFAULT_SN_PATH;

  /**
   * Constructor.
   * 
   * @param jdbcOps
   *        the JDBC API
   */
  public JdbcAuthService(JdbcOperations jdbcOps) {
    super();
    this.jdbcOps = jdbcOps;
  }

  @Override
  public Response authenticateRequest(RegisterRequest request) {
    final String tokenId = request.getUsername();
    if (tokenId == null || tokenId.isEmpty()) {
      return new Response(ResponseStatus.NEXT);
    }
    final Map<String, String> pwTokens = delimitedStringToMap(request.getPassword(), ";", "=");
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
    return new Response();
  }

  /**
   * Get the configured authenticate JDBC call.
   * 
   * @return the authenticate JDBC call
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
      throw new IllegalArgumentException("snHost must not be null");
    }
    this.authenticateCall = jdbcCall;
  }

  /**
   * Get the configured SolarNetwork host.
   * 
   * @return the host
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
   * @return the path
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

}
