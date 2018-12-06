
package net.solarnetwork.flux.vernemq.webhook.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.solarnetwork.web.domain.Response;

/**
 * Web controller for "are you there" type requests.
 * 
 * @author matt
 */
@RestController
@RequestMapping(path = "/api/v1", method = RequestMethod.GET)
public class PingController {

  /**
   * Get a simple {@literal allGood} assessment.
   * 
   * @return map of properties
   */
  @RequestMapping("/ping")
  public Response<Map<String, ?>> ping() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("allGood", true);
    return Response.response(data);
  }

}
