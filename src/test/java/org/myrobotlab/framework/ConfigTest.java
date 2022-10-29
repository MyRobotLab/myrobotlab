package org.myrobotlab.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.ClockConfig;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class ConfigTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(ConfigTest.class);

  @Test
  public void serviceLifeCycleTest() throws Exception {

    // clear plan
    Runtime.clearConfig();

    // load a simple plan
    Runtime.load("c1", "Clock");
    Plan plan = Runtime.getPlan();
    assertEquals(1, plan.size());
    for (String s : plan.keySet()) {
      assertEquals("c1", s);
      ClockConfig cc = (ClockConfig)plan.get("c1");
      assertNotNull(cc);
      assertEquals("Clock", cc.type);
    }
    
    Runtime runtime = Runtime.getInstance();
    Runtime.setConfig("blah");
    Runtime.saveConfig("clock-test");

  }

}