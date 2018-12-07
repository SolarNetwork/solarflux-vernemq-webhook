
package net.solarnetwork.flux.vernemq.webhook.web.test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import net.solarnetwork.flux.vernemq.webhook.test.SpringTestSupport;

@AutoConfigureMockMvc
public class PingControllerTests extends SpringTestSupport {

  @Autowired
  private MockMvc mvc;

  @Test
  public void getPing() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/api/v1/ping").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"success\":true,\"data\":{\"allGood\":true}}"));
  }

}
