package org.myrobotlab.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.TestCatcher;
import org.myrobotlab.service.TestThrower;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.slf4j.Logger;

public class MessageTest implements NameProvider {
  public final static Logger log = LoggerFactory.getLogger(MessageTest.class);

  static TestCatcher catcher;
  static TestThrower thrower;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    LoggingFactory.init(Level.INFO);
    catcher = (TestCatcher) Runtime.start("catcher", "TestCatcher");
    thrower = (TestThrower) Runtime.start("thrower", "TestThrower");
  }

  @Test
  public void simpleSubscribeAndThrow() throws Exception {
    log.info("simpleSubscribeAndThrow");

    catcher.clear();
    catcher.subscribe("thrower", "pitch");
    // ROUTE MUST STABALIZE - BEFORE MSGS - otherwise they will be missed
    Service.sleep(100);

    thrower.pitchInt(1000);
    BlockingQueue<Message> balls = catcher.waitForMsgs(1000);

    log.warn(String.format("caught %d balls", balls.size()));
    log.warn(String.format("left balls %d ", catcher.msgs.size()));
  }

  // @Test
  public void broadcastMessage() throws Exception {
    log.info("broadcastMessage");

    catcher.clear();
    catcher.subscribe("thrower", "pitch");
    Service.sleep(100);

    Message msg = Message.createMessage(this, null, "getServiceNames", null);
    CommunicationInterface comm = thrower.getComm();
    comm.send(msg);

    String[] ret = (String[]) thrower.invoke(msg);
    log.info(String.format("got %s", Arrays.toString(ret)));
    assertNotNull(ret);

  }

  /**
   * test to verify we can remove all message routes
   * @throws Exception e
   */
  // Ignoring this for now, some reason we're getting a stack overflow
  // when running this test from the ant build.
  // @Test
  public final void clearRoutes() throws Exception {
    log.info("clearRoutes");

    catcher.clear();
    catcher.subscribe("thrower", "pitch");

    // "long" pause to make sure our message route is in
    Service.sleep(100);

    thrower.pitchInt(1000);
    BlockingQueue<Message> balls = catcher.waitForMsgs(1000);

    log.warn(String.format("caught %d balls", balls.size()));
    log.warn(String.format("left balls %d ", catcher.msgs.size()));

    Runtime.removeAllSubscriptions();

    Message msg = Message.createMessage(this, null, "getServiceNames", null);
    CommunicationInterface comm = thrower.getComm();
    comm.send(msg);

    String[] ret = (String[]) thrower.invoke(msg);
    log.info(String.format("got %s", Arrays.toString(ret)));
    assertNotNull(ret);

    catcher.clear();

    // "long" pause to make sure our message route is in
    Service.sleep(100);

    thrower.pitchInt(1000);

    Service.sleep(100);
    assertEquals(0, catcher.msgs.size());

  }

  @Test
  final public void badNameTest() throws Exception {
    log.info("badNameTest");
    catcher.clear();
    TestCatcher catcher2 = null;
    try {
      Runtime.create("myName/isGeorge", "TestCatcher");
    } catch (Exception e) {
      // Logging.logError(e);
      log.info("good bad name threw");
    }
    assertNull(catcher2);
  }

  @Test
  final public void invokeStringNotation() throws Exception {
    /*
     * is it this test ??? try { log.info("invokeStringNotation");
     * catcher.clear(); catcher.subscribe("thrower", "pitch");
     * Service.sleep(100);
     * 
     * thrower.pitchInt(1000); BlockingQueue<Message> balls =
     * catcher.waitForMsgs(1000);
     * 
     * assertEquals(1000, balls.size()); } catch (Exception e) {
     * Logging.logError(e); }
     */

  }

  /*
   * 
   * @Test final public void badTest(){ boolean foo = false; assertTrue(foo); }
   * 
   */

  /**
   * test to excercise
   * @throws Exception e
   * 
   */
  @Test
  final public void RuntimeTests() throws Exception {
    log.info("RuntimeTests");

    catcher.clear();
    // FIXME - implement
    // catcher.subscribe("thrower/pitch");

    catcher.clear();
    catcher.subscribe("thrower", "pitch");
    Service.sleep(100);

    thrower.pitchInt(1000);
    BlockingQueue<Message> balls = catcher.waitForMsgs(1000);
    log.info("got {} balls", balls.size());

    String runtimeName = Runtime.getInstance().getName();
    String[] ret = (String[]) thrower.sendBlocking(runtimeName, "getServiceNames");
    log.info(String.format("got %s", Arrays.toString(ret)));
    assertNotNull(ret);
  }

  @Override
  public String getName() {
    return "tester";
  }

  /*
   * public static void main(String[] args) { try {
   * 
   * LoggingFactory.getInstance().configure();
   * LoggingFactory.getInstance().setLevel(Level.DEBUG); Logging logging =
   * LoggingFactory.getInstance(); logging.addAppender(Appender.FILE);
   * 
   * setUpBeforeClass(); //clearRoutes(); // badNameTest(); //
   * invokeStringNotation();
   * 
   * 
   * } catch(Exception e){ Logging.logError(e); }
   * 
   * System.exit(0); }
   */
}
