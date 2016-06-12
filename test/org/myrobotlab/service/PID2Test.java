package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PID2Test {

  @Test
  public void testCompute() {
    PID2 pid = (PID2) Runtime.createAndStart("PID2", "PID2");
    pid.setPID("x", 10.0, 0.0, 0.0);
    pid.setMode("x", PID2.MODE_AUTOMATIC);
    pid.setOutputRange("x", 10.0, 50.0);
    pid.setSetpoint("x", 0.5);
    pid.setInput("x", 0.5);
    // Test that the value is centered
    boolean calculated = pid.compute("x");
    if (calculated) {
      double actualOutput = pid.getOutput("x");
      assertEquals("Incorrect PID output", 30.0, actualOutput, 3);
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
      assertEquals("Incorrect PID output", 25.0, actualOutput, 3);
    } else {
      assertTrue("No calculation done", calculated);
    }

  }

  @Test
  public void testDirect() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetControllerDirection() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetKd() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetKi() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetKp() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetMode() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetOutput() {
    // fail("Not yet implemented");
  }

  @Test
  public void testSetOutput() {
    // fail("Not yet implemented");
  }

  @Test
  public void testGetSetpoint() {
    // fail("Not yet implemented");
  }

  @Test
  public void testInit() {
    // fail("Not yet implemented");
  }

  @Test
  public void testInvert() {
    // fail("Not yet implemented");
  }

  @Test
  public void testSetControllerDirection() {
    // fail("Not yet implemented");
  }

  @Test
  public void testSetInput() {
    // fail("Not yet implemented");
  }

  @Test
  public void testSetMode() {
    // fail("Not yet implemented");
  }

  @Test
  public void testSetOutputRange() {
    // fail("Not yet implemented");
  }

  @Test
  public void testSetPID() {
    // fail("Not yet implemented");
  }

  @Test
  public void testSetSampleTime() {
    // fail("Not yet implemented");
  }

  @Test
  public void testSetSetpoint() {
    // fail("Not yet implemented");
  }

}
