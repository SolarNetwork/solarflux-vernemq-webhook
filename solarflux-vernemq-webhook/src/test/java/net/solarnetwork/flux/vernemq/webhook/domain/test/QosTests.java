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

package net.solarnetwork.flux.vernemq.webhook.domain.test;

import static com.spotify.hamcrest.jackson.IsJsonStringMatching.isJsonStringMatching;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonInt;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;

/**
 * Test cases for the {@link Qos} enum.
 * 
 * @author matt
 */
public class QosTests {

  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @Test
  public void toJson() throws JsonProcessingException {
    for (Qos qos : Qos.values()) {
      String json = objectMapper.writeValueAsString(qos);

      // @formatter:off
      assertThat("Qos " + qos, json, isJsonStringMatching(
          jsonInt(qos.getKey())
      ));
      // @formatter:on
    }
  }

  @Test
  public void fromJson() throws IOException {
    for (Qos qos : Qos.values()) {
      String json = String.valueOf(qos.getKey());

      Qos q = objectMapper.readValue(json, Qos.class);
      assertThat("Qos " + qos, q, equalTo(qos));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void forKeyBadValue() {
    Qos.forKey(-1);
  }

}
