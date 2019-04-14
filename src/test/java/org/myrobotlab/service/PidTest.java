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
    Pid pid = (Pid) Runtime.start("pid", "Pid");

    // pid.init("x");

    pid.setPID("x", 10.0, 0.0, 0.0);
    pid.setMode("x", Pid.MODE_AUTOMATIC);
    pid.setOutputRange("x", 10.0, 50.0);
    pid.setSetpoint("x", 0.5);
    pid.setInput("x", 0.5);
    pid.setDeadBand("x", 0.0);
    // Test that the value is centered
    boolean calculated = pid.compute("x");
    if (calculated) {
      double actualOutput = pid.getOutput("x");
      assertEquals("Incorrect Pid output", 30.0, actualOutput, 3);
    } else {
      assertTrue("No calculation done", calculated);
    }

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // Test the P(roportional value)
    pid.setInput("x", 1.0);
    calculated = pid.compute("x");
    if (calculated) {
      double actualOutput = pid.getOutput("x");
      assertEquals("Incorrect Pid output", 25.0, actualOutput, 3);
    } else {
      assertTrue("No calculation done", calculated);
    }
    
  }

}