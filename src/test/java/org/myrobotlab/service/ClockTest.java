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
    python.execResource("Clock/Clock.py");

    // TODO release clock
    // TODO some verification in python
    
    // basic service functions
    Clock clock = (Clock) Runtime.start("clock", "Clock");
    Integer interval = 1000;
    assertNotNull(clock);
    clock.setInterval(interval);
    assertEquals(interval, clock.getInterval());
    
    clock.startClock();
    Service.sleep(10);
    assertTrue(clock.isClockRunning());
    
    clock.stopClock();
    Service.sleep(10);
    assertTrue(!clock.isClockRunning());

    // set subscription
    clock.addListener("publishEpoch", "catcher", "onLong");
    
    // watchdog - by default it starts with the "wait" when started vs the event
    // must not have generated a pulse
    clock.startClock();
    Service.sleep(500);
    // starting clock should not immediately fire pulse
    assertEquals(0, catcher.longs.size());
    Service.sleep(800);
    assertEquals(1, catcher.longs.size());

    // resetting watchdog
    clock.stopClock();
    Service.sleep(10);
    assertTrue(!clock.isClockRunning());
    catcher.longs.clear();

    clock.startClock();
    Service.sleep(500);
    assertEquals(0, catcher.longs.size());
    clock.restartClock();

    Service.sleep(500);
    assertEquals(0, catcher.longs.size());
    clock.restartClock();

    Service.sleep(500);
    assertEquals(0, catcher.longs.size());
    clock.restartClock();

    Service.sleep(500);
    assertEquals(0, catcher.longs.size());
    clock.restartClock();
    
    // wait now for the event
    Service.sleep(1100);
    assertEquals(1, catcher.longs.size());
    clock.restartClock();
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
    assertEquals(0, catcher.longs.size());
    clock.restartClock();

    // reset
    Service.sleep(500);
    assertEquals(0, catcher.longs.size());
    clock.restartClock();

    String hello = catcher.strings.poll(1500, TimeUnit.MILLISECONDS);
    assertEquals("hello!", hello);
    
    Runtime.release("clock");
    
  }

}