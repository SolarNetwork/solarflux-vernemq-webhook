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

package net.solarnetwork.flux.vernemq.webhook.web.test;

import static net.solarnetwork.flux.vernemq.webhook.domain.HookType.HOOK_HEADER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import net.solarnetwork.flux.vernemq.webhook.domain.HookType;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;
import net.solarnetwork.flux.vernemq.webhook.web.AuthHooksController;

@RunWith(SpringRunner.class)
@WebMvcTest(AuthHooksController.class)
public class AuthHooksControllerTests extends TestSupport {

  @Autowired
  private MockMvc mvc;

  @Test
  public void authOnRegister() throws Exception {
    // @formatter:off
    mvc.perform(post("/hook")
          .contentType(MediaType.APPLICATION_JSON_UTF8)
          .header(HOOK_HEADER, HookType.AuthenticateOnRegister.getKey())
          .content(classResourceAsBytes("auth_on_register-01.json"))
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"success\":true,\"data\":{\"allGood\":true}}"));
    // @formatter:on
  }

}
