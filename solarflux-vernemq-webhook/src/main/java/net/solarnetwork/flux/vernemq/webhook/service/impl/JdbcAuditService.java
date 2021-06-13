/* ========================================================================
 * Copyright 2021 SolarNetwork Foundation
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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcOperations;

import net.solarnetwork.flux.vernemq.webhook.domain.Actor;
import net.solarnetwork.flux.vernemq.webhook.domain.Message;
import net.solarnetwork.flux.vernemq.webhook.service.AuditService;

/**
 * A JDBC implementation of {@link AuditService}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcAuditService implements AuditService {

  // CHECKSTYLE OFF: LineLength

  /**
   * The default value for the {@code authenticateCall} property.
   * 
   * <p>
   * This call must accept the following parameters:
   * </p>
   * 
   * <ol>
   * <li>service name (string)</li>
   * <li>node ID (long)</li>
   * <li>source ID (string)</li>
   * <li>timestamp</li>
   * <li>byte count (int)</li>
   * </ol>
   */
  public static final String DEFAULT_AUDIT_NODE_PUBLISH_MESSAGE_CALL = "{audit_mqtt_publish(?,?,?,?,?)}";

  /**
   * The default value for the {@link mqttServiceName} property.
   */
  public static final String DEFAULT_AUDIT_MQTT_SERVICE_NAME = "solarflux";

  // CHECKSTYLE ON: LineLength

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final JdbcOperations jdbcOps;
  private String mqttServiceName = DEFAULT_AUDIT_MQTT_SERVICE_NAME;
  private String nodePublishMessageCall = DEFAULT_AUDIT_NODE_PUBLISH_MESSAGE_CALL;
  private final Executor executor;

  /**
   * Constructor.
   * 
   * @param jdbcOps
   *        the JDBC API
   * @param executor
   *        the executor
   */
  public JdbcAuditService(JdbcOperations jdbcOps, Executor executor) {
    super();
    this.jdbcOps = jdbcOps;
    this.executor = executor;
  }

  @Override
  public void auditPublishMessage(Actor actor, Long nodeId, String sourceId, Message message) {
    final long ts = System.currentTimeMillis();
    final int byteCount = (message.getPayload() != null ? message.getPayload().length : 0);
    executor.execute(new Runnable() {

      @Override
      public void run() {
        try {
          submitNodePublishAudit(nodeId, sourceId, ts, byteCount);
        } catch (Exception e) {
          log.error("Error submitting node {} publish message [{}] audit: {}", nodeId,
              message.getTopic(), e.toString());
        }
      }
    });

  }

  private void submitNodePublishAudit(final Long nodeId, final String sourceId,
      final long timestamp, final int byteCount) throws SQLException {
    log.debug("Auditing node {} publish message bytes {} @ {}", nodeId, byteCount, timestamp);
    jdbcOps.execute(new CallableStatementCreator() {

      @Override
      public CallableStatement createCallableStatement(Connection con) throws SQLException {
        CallableStatement stmt = con.prepareCall(nodePublishMessageCall);
        stmt.setString(1, mqttServiceName);
        stmt.setLong(2, nodeId);
        stmt.setString(3, sourceId);
        stmt.setTimestamp(4, new java.sql.Timestamp(timestamp));
        stmt.setInt(5, byteCount);
        return stmt;
      }
    }, new CallableStatementCallback<Void>() {

      @Override
      public Void doInCallableStatement(CallableStatement cs)
          throws SQLException, DataAccessException {
        cs.execute();
        return null;
      }
    });

  }

  /**
   * Set the MQTT audit service name to use.
   * 
   * @param mqttServiceName
   *        the service to set; defaults to {@link #DEFAULT_AUDIT_MQTT_SERVICE_NAME}
   */
  public void setMqttServiceName(String mqttServiceName) {
    this.mqttServiceName = mqttServiceName;
  }

  /**
   * Set the JDBC call to make to audit node publish messages.
   * 
   * @param nodePublishMessageCall
   *        the nodePublishMessageCall to set; defaults to
   *        {@link #DEFAULT_AUDIT_NODE_PUBLISH_MESSAGE_CALL}
   */
  public void setNodePublishMessageCall(String nodePublishMessageCall) {
    this.nodePublishMessageCall = nodePublishMessageCall;
  }

}
