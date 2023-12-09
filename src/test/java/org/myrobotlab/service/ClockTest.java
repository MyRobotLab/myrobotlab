package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.test.AbstractTest;

public class ClockTest extends AbstractTest {

  // FIXME - before test
  // release any clock
  // release catcher

  @Test
  public void testService() throws Exception {

    Python python = (Python) Runtime.start("python", "Python");
        
    MockGateway gateway = (MockGateway)Runtime.start("gateway", "MockGateway");
    gateway.clear();
    
//    Runtime.start("webgui", "WebGui");

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
    Service.sleep(10);
    assertTrue(clock.isClockRunning());

    clock.stopClock();
    Service.sleep(10);
    assertTrue(!clock.isClockRunning());

    // set subscription
    clock.addListener("publishEpoch", "catcher", "onLong");
    clock.addListener("publishEpoch", "mocker@mockId");

    // watchdog - by default it starts with the "wait" when started vs the event
    // must not have generated a pulse
    clock.startClock();
    Service.sleep(500);
    // starting clock should not immediately fire pulse
    assertNull("start sleep 500ms", gateway.getMsg("mocker", "onEpoch"));
    
    Service.sleep(800);
    assertNotNull("should have an epoch msg", gateway.getMsg("mocker", "onEpoch"));

    // resetting watchdog
    clock.stopClock();
    Service.sleep(10);
    assertTrue(!clock.isClockRunning());
    gateway.clear();

    clock.startClock();
    Service.sleep(100);
    assertNull("no msg yet", gateway.getMsg("mocker", "onEpoch"));
    clock.restartClock();

    Service.sleep(600);
    assertNull("restart 1 no msg yet", gateway.getMsg("mocker", "onEpoch"));
    clock.restartClock();

    Service.sleep(600);
    assertNull("restart 2 no msg yet", gateway.getMsg("mocker", "onEpoch"));
    clock.restartClock();

    Service.sleep(600);
    assertNull("restart 3 no msg yet", gateway.getMsg("mocker", "onEpoch"));
    clock.restartClock();

    Service.sleep(600);
    assertNull("restart 4 no msg yet", gateway.getMsg("mocker", "onEpoch"));
    clock.restartClock();
    
    // wait now for the event
    Service.sleep(1100);
    assertNotNull("should have event now", gateway.getMsg("mocker", "onEpoch"));
    clock.restartClock();
    Service.sleep(500);
    assertTrue(clock.isClockRunning());

    // starting a watchdog with a new event
    clock.stopClock();
    Service.sleep(10);
    assertTrue(!clock.isClockRunning());
    gateway.clear();

    clock.addClockEvent("catcher", "onString", "hello!");
    clock.addClockEvent("mocker@mockId", "onString", "hello!");
    clock.startClock();

    // reset
    Message msg = gateway.getMsg("mocker", "onString");
    Service.sleep(500);
    assertNull("should not have msg yet", msg);
    clock.restartClock();

    // reset
    Service.sleep(500);
    msg = gateway.getMsg("mocker", "onString");
    assertNull("should not have msg yet after restart", msg);
    clock.restartClock();

    msg = gateway.waitForMsg("mocker", "onString", 5000);
    assertEquals("hello!", msg.data[0]);

    Runtime.release("clockTest");

  }

}