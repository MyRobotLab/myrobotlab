package org.myrobotlab.service;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

public class IKInMoovArmTest {

  private InMoovArm testArm;

  @Before
  public void setUp() throws Exception {
    testArm = new InMoovArm("left");

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
    System.out.println("OMO: " + testArm.omoplate.getPos());
    System.out.println("SHO: " + testArm.shoulder.getPos());
    System.out.println("ROT: " + testArm.rotate.getPos());
    System.out.println("BIC: " + testArm.bicep.getPos());

    // assertEquals((Integer)0, testArm.bicep.getPos());
    // assertEquals((Integer)90, testArm.rotate.getPos());
    // assertEquals((Integer)30, testArm.shoulder.getPos());
    // assertEquals((Integer)10, testArm.omoplate.getPos());

  }

  /*
   * @Test public final void testBroadcastState() { //fail("Not yet implemented"
   * ); }
   * 
   * @Test public final void testGetCategories() { //fail("Not yet implemented"
   * ); }
   * 
   * @Test public final void testGetDescription() { //fail("Not yet implemented"
   * ); }
   * 
   * @Test public final void testSave() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testStartService() { //fail("Not yet implemented");
   * }
   * 
   * @Test public final void testGetPeers() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testInMoovArm() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testAttach() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testConnect() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testDetach() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testGetArduino() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testGetBicep() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testGetLastActivityTime() { //fail(
   * "Not yet implemented"); }
   * 
   * @Test public final void testGetOmoplate() { //fail("Not yet implemented");
   * }
   * 
   * @Test public final void testGetRotate() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testGetScript() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testGetShoulder() { //fail("Not yet implemented");
   * }
   * 
   * @Test public final void testGetSide() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testIsAttached() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testMoveTo() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testRelease() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testRest() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testSetArduino() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testSetBicep() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testSetLimits() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testSetOmoplate() { //fail("Not yet implemented");
   * }
   * 
   * @Test public final void testSetpins() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testSetRotate() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testSetShoulder() { //fail("Not yet implemented");
   * }
   * 
   * @Test public final void testSetSide() { //fail("Not yet implemented"); }
   * 
   * @Test public final void testSetSpeed() { //fail("Not yet implemented"); }
   * 
   */

}
