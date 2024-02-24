package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.service.Random.RandomMessage;
import org.myrobotlab.test.AbstractTest;

public class RandomTest extends AbstractTest {

  @Before /* before each test */
  public void setUp() throws IOException {
    // remove all services - also resets config name to DEFAULT effectively
    Runtime.releaseAll(true, true);
    // clean our config directory
    // Runtime.removeConfig("RandomTest");
    // set our config
    Runtime.setConfig("RandomTest");
    Runtime.start("randomTest", "Random");
  }

  @Test
  public void testService() throws Exception {
    Clock clock = (Clock) Runtime.start("clock", "Clock");
    Random random = (Random) Runtime.start("randomTest", "Random");

    clock.stopClock();
    clock.setInterval(1000);
    assertTrue("set interval 1000 base value", 1000 == clock.getInterval());

    random.addRandom(0, 200, "clock", "setInterval", 5000, 10000);
    random.enable();

    sleep(1000);

    assertTrue("should have method", random.getKeySet().contains("clock.setInterval"));

    assertTrue(String.format("random method 1 should be %d => 5000 values", clock.getInterval()), 5000 <= clock.getInterval());
    assertTrue(String.format("random method 1 should be %d <= 10000 values", clock.getInterval()), clock.getInterval() <= 10000);

    random.remove("clock.setInterval");

    assertTrue("should not have method", !random.getKeySet().contains("clock.setInterval"));

    random.addRandom(0, 200, "clock", "setInterval", 5000, 10000);
    random.addRandom(0, 100, "clock", "startClock");

    sleep(500);
    assertTrue("clock should be started 1", clock.isClockRunning());

    // disable all of a services random events
    random.disable("clock.startClock");
    sleep(250);
    clock.stopClock();
    assertTrue("clock should not be started 1", !clock.isClockRunning());

    // enable all of a service's random events
    random.enable("clock.startClock");
    sleep(250);
    assertTrue("clock should be started 2", clock.isClockRunning());

    // disable one method - leave other enabled
    random.disable("clock.startClock");
    clock.stopClock();
    sleep(200);
    clock.setInterval(9999);
    assertTrue("clock should not be started 3", !clock.isClockRunning());
    assertTrue(String.format("random method 2 should be %d => 5000 values", clock.getInterval()), 5000 <= clock.getInterval());
    assertTrue(String.format("random method 2 should be %d <= 10000 values", clock.getInterval()), clock.getInterval() <= 10000);

    // disable all
    random.disable();
    sleep(200);
    clock.setInterval(9999);
    assertTrue("clock should not be started 4", !clock.isClockRunning());
    assertEquals(9999, (long) clock.getInterval());

    // re-enable all that were previously enabled but not explicitly disabled
    // ones
    random.enable();
    sleep(1000);
    assertTrue("clock should not be started 5", !clock.isClockRunning());
    assertTrue(String.format("random method 3 should be %d => 5000 values", clock.getInterval()), 5000 <= clock.getInterval());
    assertTrue(String.format("random method 3 should be %d <= 10000 values", clock.getInterval()), clock.getInterval() <= 10000);

    clock.stopClock();
    random.purge();

    Map<String, RandomMessage> events = random.getRandomEvents();
    assertTrue(events.size() == 0);

    random.addRandom("named task", 200, 500, "clock", "setInterval", 100, 1000, 10);

    clock.releaseService();
    random.releaseService();

  }

}
