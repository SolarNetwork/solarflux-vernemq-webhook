
package net.solarnetwork.flux.vernemq.webhook.test;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Base class for running tests within a full Spring container.
 * 
 * @author matt
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public abstract class SpringTestSupport extends TestSupport {

}
