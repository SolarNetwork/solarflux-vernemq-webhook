
package net.solarnetwork.flux.vernemq.webhook.domain.v311.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterRequest;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link RegisterRequest} class.
 * 
 * @author matt
 */
public class RegisterRequestTests extends TestSupport {

  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @Test
  public void parseFull() throws IOException {
    RegisterRequest req = objectMapper.readValue(classResourceAsBytes("auth_on_register-01.json"),
        RegisterRequest.class);
    assertThat("clean_session", req.getCleanSession(), equalTo(false));
    assertThat("client_id", req.getClientId(), equalTo("clientid"));
    assertThat("mountpoint", req.getMountpoint(), equalTo(""));
    assertThat("password", req.getPassword(), equalTo("password"));
    assertThat("peerAddress", req.getPeerAddress(), equalTo("127.0.0.1"));
    assertThat("peerPort", req.getPeerPort(), equalTo(8888));
    assertThat("username", req.getUsername(), equalTo("username"));
  }

}
