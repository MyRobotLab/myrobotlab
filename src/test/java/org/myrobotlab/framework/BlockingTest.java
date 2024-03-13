package org.myrobotlab.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.TestThrower;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class BlockingTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(BlockingTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // // LoggingFactory.init("WARN"); <- do not do this .. changing log levels
    // should be done in 1 place - in the framework that calls this test
  }

  @Test
  public void blockingTest() throws Exception {
    Runtime.start("catcher07", "TestCatcher");
    TestThrower thower07 = (TestThrower) Runtime.start("thower07", "TestThrower");

    Message msg = Message.createMessage("thower07", "catcher07", "onInt", 3);
    Integer ret = (Integer)thower07.sendBlocking(msg, null);
    assertEquals(3, (int)ret);

    long startTime = System.currentTimeMillis();
    msg = Message.createMessage("thower07", "catcher07", "waitForThis", new Object[] {7, 1000});
    ret = (Integer)thower07.sendBlocking(msg, null);
    assertTrue("1s process", System.currentTimeMillis() - startTime > 500);
    assertEquals(7, (int)ret);

    Runtime.release("catcher07");
    Runtime.release("thower07");
  }

}