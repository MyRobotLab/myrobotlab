package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.myrobotlab.framework.Service;
import org.myrobotlab.test.AbstractTest;

public class ClockTest extends AbstractTest {

  // FIXME - before test
  // release any clock
  // release catcher

  @Test
  public void testService() throws Exception {

    Python python = (Python) Runtime.start("python", "Python");
    TestCatcher catcher = (TestCatcher) Runtime.start("catcher", "TestCatcher");
    catcher.clear();

    // check service script
//    python.execResource("Clock/Clock.py");

    // TODO release clock
    // TODO some verification in python

    // basic service functions
    Clock clock = (Clock) Runtime.start("clockTest", "Clock");
    Integer interval = 1000;
    assertNotNull(clock);
    clock.setInterval(interval);
    assertEquals(interval, clock.getInterval());

    clock.startClock();
    Service.sleep(100);
    assertTrue(clock.isClockRunning());

    clock.stopClock();
    Service.sleep(10);
    assertTrue(!clock.isClockRunning());

    // set subscription
    clock.addListener("publishEpoch", "catcher", "onLong");

    // watchdog - by default it starts with the "wait" when started vs the event
    // must not have generated a pulse
    catcher.longs.clear();
    clock.startClock();
    Service.sleep(500);
    // starting clock should not immediately fire pulse
    assertEquals("start sleep 500ms", 0, catcher.longs.size());
    Service.sleep(800);
    assertEquals(1, catcher.longs.size());

    // resetting watchdog
    clock.stopClock();
    Service.sleep(10);
    assertTrue(!clock.isClockRunning());
    catcher.longs.clear();

    clock.startClock();
    Service.sleep(100);
    assertEquals("after 100ms", 0, catcher.longs.size());
    clock.restartClock();

    Service.sleep(100);
    assertEquals("restart 1", 0, catcher.longs.size());
    clock.restartClock();

    Service.sleep(100);
    assertEquals("restart 2", 0, catcher.longs.size());
    clock.restartClock();

    Service.sleep(100);
    assertEquals("restart 3", 0, catcher.longs.size());
    clock.restartClock();

    Service.sleep(100);
    assertEquals("restart 4", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 5", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 6", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 7", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 8", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 9", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 10", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 11", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 12", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 13", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 14", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 15", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 16", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 17", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 18", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 19", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 20", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 21", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 22", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 23", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 24", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 25", 0, catcher.longs.size());
    clock.restartClock();
    Service.sleep(100);
    assertEquals("restart 26", 0, catcher.longs.size());
    clock.restartClock();
    
    
    // wait now for the event
    Service.sleep(1100);
    log.info("size {}",catcher.longs.size());
    assertEquals(1, catcher.longs.size());
    clock.restartClock();
    Service.sleep(500);
    assertTrue(clock.isClockRunning());

    // starting a watchdog with a new event
    clock.stopClock();
    Service.sleep(10);
    assertTrue(!clock.isClockRunning());
    catcher.longs.clear();

    clock.addClockEvent("catcher", "onString", "hello!");
    clock.startClock();

    // reset
    Service.sleep(500);
    assertEquals("after adding clock event start", 0, catcher.longs.size());
    clock.restartClock();

    // reset
    Service.sleep(500);
    assertEquals("after restart with event", 0, catcher.longs.size());
    clock.restartClock();

    String hello = catcher.strings.poll(1500, TimeUnit.MILLISECONDS);
    assertEquals("hello!", hello);

    Runtime.release("clockTest");

  }

}