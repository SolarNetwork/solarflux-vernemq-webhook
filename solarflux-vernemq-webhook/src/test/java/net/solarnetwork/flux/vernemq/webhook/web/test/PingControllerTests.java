
package net.solarnetwork.flux.vernemq.webhook.web.test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;
import net.solarnetwork.flux.vernemq.webhook.web.PingController;

@RunWith(SpringRunner.class)
@WebMvcTest(PingController.class)
public class PingControllerTests extends TestSupport {

  @Autowired
  private MockMvc mvc;

  @Test
  public void getPing() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/api/v1/ping").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"success\":true,\"data\":{\"allGood\":true}}"));
  }

}
