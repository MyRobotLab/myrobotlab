package org.myrobotlab.jme3;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class Jme3UtilAngleTest {

  @Test
  public void signedAngleFollowsTheRequestedAxis() {
    Quaternion bind = new Quaternion();
    Quaternion local = new Quaternion().fromAngleAxis(30f * FastMath.DEG_TO_RAD, Vector3f.UNIT_Z);
    assertEquals(30, Jme3Util.signedDegreesAboutAxis(bind, local, Vector3f.UNIT_Z), 0.05);
    assertEquals(0, Jme3Util.signedDegreesAboutAxis(bind, local, Vector3f.UNIT_X), 0.05);
    assertEquals(-30, Jme3Util.signedDegreesAboutAxis(bind, local, Vector3f.UNIT_Z.mult(-1f)), 0.05);
  }
}
