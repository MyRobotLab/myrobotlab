package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.Random.RandomMessage;

public class RandomTest extends AbstractServiceTest {

  @Override /*
             * FIXME - this assumes a single service is in the test - which
             * rarely happens - seems not useful and silly
             */
  public Service createService() throws Exception {
    return (Service) Runtime.start("random", "Random");
  }

  @Override
  public void testService() throws Exception {
    Clock clock = (Clock) Runtime.start("clock", "Clock");
    Random random = (Random) Runtime.start("random", "Random");

    clock.stopClock();
    clock.setInterval(1000);
    assertTrue("set interval 1000 base value", 1000 == clock.getInterval());

    random.addRandom(0, 200, "clock", "setInterval", 5000, 10000);
    random.enable();

    sleep(500);

    assertTrue("should have method", random.getKeySet().contains("clock.setInterval"));
    
    assertTrue(String.format("random method 1 should be %d => 5000 values", clock.getInterval()), 5000 <= clock.getInterval());
    assertTrue(String.format("random method 1 should be %d <= 10000 values",clock.getInterval()) , clock.getInterval() <= 10000);
    
    random.remove("clock", "setInterval");
    
    assertTrue("should not have method", !random.getKeySet().contains("clock.setInterval"));

    random.addRandom(0, 200, "clock", "setInterval", 5000, 10000);
    random.addRandom(0, 200, "clock", "startClock");
    // random.addRandom(0, 200, "clock", "stopClock");
    
    sleep(500);
    assertTrue("clock should be started 1", clock.isClockRunning());
    
    // disable all of a services random events
    random.disable("clock.startClock");
    clock.stopClock();
    sleep(200);
    assertTrue("clock should not be started 1", !clock.isClockRunning());
    
    // enable all of a service's random events
    random.enable("clock.startClock");
    sleep(200);
    assertTrue("clock should be started 2", clock.isClockRunning());
    
    // disable one method - leave other enabled
    random.disable("clock.startClock");
    clock.stopClock();
    clock.setInterval(999999);
    sleep(200);
    assertTrue("clock should not be started 3", !clock.isClockRunning());
    assertTrue(String.format("random method 2 should be %d => 5000 values", clock.getInterval()), 5000 <= clock.getInterval());
    assertTrue(String.format("random method 2 should be %d <= 10000 values",clock.getInterval()) , clock.getInterval() <= 10000);

    // disable all
    random.disable();
    sleep(200);
    clock.setInterval(999999);
    assertTrue("clock should not be started 4", !clock.isClockRunning());   
    assertEquals(999999, (long)clock.getInterval());

    // re-enable all that were previously enabled but not explicitly disabled ones
    random.enable();
    sleep(1000);
    assertTrue("clock should not be started 5", !clock.isClockRunning());
    assertTrue(String.format("random method 3 should be %d => 5000 values", clock.getInterval()), 5000 <= clock.getInterval());
    assertTrue(String.format("random method 3 should be %d <= 10000 values",clock.getInterval()) , clock.getInterval() <= 10000);

    clock.stopClock();
    random.purge();
        
    Map<String, RandomMessage> events = random.getRandomEvents();
    assertTrue(events.size() == 0);
    
    random.addRandom("named task", 200, 500, "clock", "setInterval", 100, 1000, 10);
    
    clock.releaseService();
    random.releaseService();

  }

}
