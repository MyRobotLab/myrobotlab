package org.myrobotlab.framework;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.TestCatcher;
import org.myrobotlab.service.TestThrower;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class AttachTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(AttachTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // // LoggingFactory.init("WARN"); <- do not do this .. changing log levels
    // should be done in 1 place - in the framework that calls this test
  }

  @Test
  public void attachTest() throws Exception {
    TestCatcher catcher01 = (TestCatcher) Runtime.start("catcher01", "TestCatcher");
    TestCatcher catcher02 = (TestCatcher) Runtime.start("catcher02", "TestCatcher");
    TestThrower thrower = (TestThrower) Runtime.start("thower", "TestThrower");

    assertFalse(thrower.isAttached(catcher01));

    thrower.attach(catcher01);

    // data flow is from the thrower to the catcher
    // definition of attached is to have a subscriber
    assertTrue(thrower.isAttached(catcher01));
    assertFalse(thrower.isAttached(catcher02));

    thrower.detach(catcher01);

    assertFalse(thrower.isAttached(catcher01));

    thrower.attach(catcher01);
    thrower.attach(catcher02);

    assertTrue(thrower.isAttached(catcher01));
    assertTrue(thrower.isAttached(catcher02));

    // default implementation of being attache is
    // subscription based for data flow - if data flow
    // goes from thrower to catcher - the catchers are not 'attached'
    assertFalse(catcher01.isAttached(thrower));
    assertFalse(catcher02.isAttached(thrower));

    thrower.detach();

    assertFalse(thrower.isAttached(catcher01));
    assertFalse(thrower.isAttached(catcher02));

  }

}