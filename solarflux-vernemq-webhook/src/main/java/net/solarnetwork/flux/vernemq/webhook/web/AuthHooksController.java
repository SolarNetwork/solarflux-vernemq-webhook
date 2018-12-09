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

package net.solarnetwork.flux.vernemq.webhook.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.solarnetwork.web.domain.Response;

/**
 * VerneMQ web hooks for MQTT v3 authorization.
 * 
 * @author matt
 */
@RestController
@RequestMapping(path = "/hook", method = RequestMethod.POST)
public class AuthHooksController {

  /**
   * Authenticate on register hook.
   * 
   * @return map of properties
   */
  @RequestMapping(value = "", headers = "vernemq-hook=auth_on_register")
  public Response<Map<String, ?>> authOnRegister() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("allGood", true);
    return Response.response(data);
  }

}
