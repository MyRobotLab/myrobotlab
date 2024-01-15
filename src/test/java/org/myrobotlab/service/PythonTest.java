package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.myrobotlab.framework.Service;
import org.python.core.PyInteger;

public class PythonTest extends AbstractServiceTest {

  @Override
  public Service createService() {
    return (Service) Runtime.start("python", "Python");
  }

  @Override
  public void testService() throws Exception {
    Python python = (Python) Runtime.start("python", "Python");

    // checking basic setting of a value
    python.set("test", "we love testing!");

    // basic value checking - getting values from jython into java
    // python.exec("test = 'yay!'");
    String test00 = (String) python.get("test");
    String expected00 = "we love testing!";
    assertEquals(expected00, test00);

    python.exec("test = 12");
    Integer test01 = (Integer) python.get("test");
    Integer expected01 = 12;
    assertEquals(expected01, test01);

    python.exec("test = 12.4321");
    Double test02 = (Double) python.get("test");
    Double expected02 = 12.4321;
    assertEquals(expected02, test02);

    python.exec("test = [0, 1, 2, 3, 4, 5, 6]");
    Object[] test03 = (Object[]) python.get("test");
    PyInteger expected03 = new PyInteger(3);
    assertEquals(expected03, test03[3]);

    python.exec("test = {'foo':True, 'bar':False}");
    Map test04 = (Map) python.get("test");
    assertEquals(true, test04.get("foo"));
    log.info("executing sleep");

    python.exec("sleep(1)\ntest = 7");
    test01 = (Integer) python.get("test");
    expected01 = 7;
    assertEquals(expected01, test01);

    // FIXME - because the scripts and "finishedExecutingScript" events are not
    // bound to each other with unique ids, you can waitFor
    // finishedExecutingScript
    // which MAY NOT be the script you started - the following sleep is lame,
    // but its intent is to clear all in-process executing scripts so that we
    // "waitFor" the script we are executing
    // this should be fixed by using a unique id for each script which is
    // returned
    // by exec - then you can wait for the script you want to finish
    Service.sleep(1000); // lame
    boolean blocking = false;
    long start = System.currentTimeMillis();
    python.exec("import time\ntime.sleep(1)", blocking);
    log.info("stated sleeping script - waiting for result in 1s");
    python.waitFor("python", "finishedExecutingScript", 2000);
    log.info("done with sleep time {} ms", System.currentTimeMillis() - start);

    // verifying callbacks from subscriptions can call python methods
    python.exec("count = 0\ndef onPulse(clock_date):\n\tprint('successs !', clock_date)\n\tglobal count\n\tcount = count + 1");
    Clock clockp01 = (Clock) Runtime.start("clockp01", "Clock");
    python.subscribe("clockp01", "pulse");
    clockp01.startClock();
    sleep(2000);
    Integer count = (Integer) python.get("count");
    assertTrue(count > 0);

    python.exec("clockp01.stopClock()");
    sleep(500);

    assert (!clockp01.isClockRunning());

  }

}
