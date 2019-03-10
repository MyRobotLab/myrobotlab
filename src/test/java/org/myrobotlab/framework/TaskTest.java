package org.myrobotlab.framework;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.TestCatcher;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class TaskTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(TaskTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // // LoggingFactory.init("WARN"); <- do not do this .. changing log levels
    // should be done in 1 place - in the framework that calls this test
  }

  @Test
  public void addTask() throws InterruptedException, IOException {
    TestCatcher catcher = (TestCatcher) Runtime.start("catcher", "TestCatcher");
    catcher.clear();
    catcher.subscribe("runtime", "getUptime");
    Runtime runtime = Runtime.getInstance();
    runtime.addTask(500, "getUptime");

    BlockingQueue<Message> msgs = catcher.waitForMsgs(2, 1100);

    log.info("responded task events - expecting 2 got {}", msgs.size());
    assertTrue(msgs.size() == 2);

    runtime.purgeTask("getUptime");
    catcher.clear();

    Service.sleep(1000);

    log.info("responded task events - expecting 0 got {}", msgs.size());
    assertTrue(msgs.size() == 0);

    catcher.clear();
  }

  public void onUptime(String data) {
    log.info("uptime {}", data);
  }

}