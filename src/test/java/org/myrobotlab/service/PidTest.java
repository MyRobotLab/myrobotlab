package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.myrobotlab.framework.Service;

public class PidTest extends AbstractServiceTest {

  @Before
  public void setUp() throws Exception {
    // LoggingFactory.init("WARN");
  }

  @Override
  public Service createService() {
    return (Service) Runtime.start("pid", "Pid");
  }

  @Override
  public void testService() throws Exception {
    String key = "x";
    float target = 240;

    Pid pid = (Pid) Runtime.start("pid", "Pid");
    pid.setPid(key, 0.1, 1.0, 0.0);
    pid.setSetpoint(key, target);
    pid.setOutputRange(key, -400.0, 400.0);
    
    // MiniPID mpid = new MiniPID(0.1, 1.0, 0);

    float[] series = new float[] { 210 };

    
    //boolean done = false;
    for (Float in : series) {
      // double mout = mpid.getOutput(in, target);
      double pout = pid.compute(key, in);
      log.warn("in {} pout {}", in, pout);
    }

    pid.setMode(key, Pid.MODE_AUTOMATIC);
    pid.setOutputRange(key, 10.0, 50.0);
    pid.setSetpoint(key, 0.5);
    pid.setDeadBand(key, 0.0);
    // Test that the value is centered
    Double calculated = pid.compute(key, 0.5);
    if (calculated != null) {
      assertEquals("Incorrect Pid output", 30.0, calculated, 3);
    } else {
      assertTrue("No calculation done", calculated == null);
    }

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // Test the P(roportional value)
    calculated = pid.compute(key, 0.5);
    if (calculated != null) {
      assertEquals("Incorrect Pid output", 33.0, calculated, 3);
    } else {
      assertTrue("No calculation done", calculated == null);
    }

  }

}