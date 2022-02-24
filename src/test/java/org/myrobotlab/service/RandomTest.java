package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.myrobotlab.framework.Service;

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

    sleep(200);

    assertTrue("should have method", random.getKeySet().contains("clock.setInterval"));
    
    assertTrue("random method should be => 5000 values", 5000 <= clock.getInterval());
    assertTrue("random method should be <= 10000 values", clock.getInterval() <= 10000);
    
    random.remove("clock", "setInterval");
    
    assertTrue("should not have method", !random.getKeySet().contains("clock.setInterval"));

    random.addRandom(0, 200, "clock", "setInterval", 5000, 10000);
    random.addRandom(0, 200, "clock", "startClock");
    // random.addRandom(0, 200, "clock", "stopClock");
    
    sleep(200);
    assertTrue("clock should be started", clock.isClockRunning());
    
    // disable all of a services random events
    random.disable("clock");
    clock.stopClock();
    sleep(200);
    assertTrue("clock should not be started", !clock.isClockRunning());
    
    // enable all of a service's random events
    random.enable("clock");
    sleep(200);
    assertTrue("clock should be started", clock.isClockRunning());
    
    // disable one method - leave other enabled
    random.disable("clock.startClock");
    clock.stopClock();
    clock.setInterval(999999);
    sleep(200);
    assertTrue("clock should not be started", !clock.isClockRunning());
    assertTrue("random method should be => 5000 values", 5000 <= clock.getInterval());
    assertTrue("random method should be <= 10000 values", clock.getInterval() <= 10000);

    // disable all
    random.disable();
    clock.setInterval(999999);
    sleep(200);
    assertTrue("clock should not be started", !clock.isClockRunning());   
    assertEquals(999999, (long)clock.getInterval());

    // re-enable all that were previously enabled but not explicitly disabled ones
    random.enable();
    sleep(200);
    assertTrue("clock should not be started", !clock.isClockRunning());
    assertTrue("random method should be => 5000 values", 5000 <= clock.getInterval());
    assertTrue("random method should be <= 10000 values", clock.getInterval() <= 10000);
        
    clock.releaseService();
    random.releaseService();

  }

}
