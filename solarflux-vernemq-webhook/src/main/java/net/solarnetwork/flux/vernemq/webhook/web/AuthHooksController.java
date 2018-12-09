
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
