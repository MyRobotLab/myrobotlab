package org.myrobotlab.service;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.test.AbstractTest;

@Ignore
public class IKInMoovArmTest extends AbstractTest {

  private InMoov2Arm testArm;

  @Before
  public void setUp() throws Exception {
    testArm = (InMoov2Arm) Runtime.start("left", "InMoov2Arm");

  }

  @Test
  public void testOnJointAngles() throws Exception {

    HashMap<String, Double> angleMap = new HashMap<String, Double>();

    // this is the rest position from the DH model of the inmoov arm
    angleMap.put("omoplate", -80.0);
    angleMap.put("shoulder", 60.0);
    angleMap.put("rotate", 180.0);
    angleMap.put("bicep", 90.0);

    // they have some angle offsets that get mapped here
    testArm.onJointAngles(angleMap);

    // the actual angles for the inmoov arm servos.
    System.out.println("OMO: " + testArm.omoplate.getCurrentInputPos());
    System.out.println("SHO: " + testArm.shoulder.getCurrentInputPos());
    System.out.println("ROT: " + testArm.rotate.getCurrentInputPos());
    System.out.println("BIC: " + testArm.bicep.getCurrentInputPos());

    // assertEquals((Integer)0, testArm.bicep.getPos());
    // assertEquals((Integer)90, testArm.rotate.getPos());
    // assertEquals((Integer)30, testArm.shoulder.getPos());
    // assertEquals((Integer)10, testArm.omoplate.getPos());

  }

}
