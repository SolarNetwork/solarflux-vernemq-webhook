
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
          .header(HOOK_HEADER, HookType.AuthenticateOnRegister)
          .content(classResourceAsBytes("auth_on_register-01.json"))
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"success\":true,\"data\":{\"allGood\":true}}"));
    // @formatter:on
  }

}
