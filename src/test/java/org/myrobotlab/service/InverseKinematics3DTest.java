package org.myrobotlab.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class InverseKinematics3DTest extends AbstractTest {

  static String arm = "myArm";

  public final static Logger log = LoggerFactory.getLogger(InverseKinematics3DTest.class);

  @Before
  public void setUp() {
    // LoggingFactory.init("WARN");
  }

  @Test
  public void testForwardKinematics() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d", "InverseKinematics3D");
    // InMoovArm ia = new InMoovArm("i01");
    ik3d.setCurrentArm(arm, InMoovArm.getDHRobotArm("i01", "left"));
    ik3d.centerAllJoints(arm);
    log.info("{}",ik3d.getCurrentArm(arm).getPalmPosition());
  }

  @Test
  public void testIK3D() throws Exception {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d", "InverseKinematics3D");
    // InMoovArm ia = new InMoovArm("i01");
    ik3d.setCurrentArm(arm, InMoovArm.getDHRobotArm("i01", "left"));
    // start from a centered joint configuration so we can iterate without
    // loosing rank
    // in our jacobian!
    ik3d.centerAllJoints(arm);
    ik3d.moveTo(arm, 100.0, 0.0, 50.0);
    Point p = ik3d.currentPosition(arm);
    double[][] positions = ik3d.createJointPositionMap(arm);
    int x = positions[0].length;
    int y = positions.length;
    for (int j = 0; j < y; j++) {
      for (int i = 0; i < x; i++) {
        log.info(positions[j][i] + " ");
      }

    }
    // Last point:
    log.warn("Last Point: " + p.toString());
    // TODO: this doesn't actually assert the position was reached! ouch.
    Assert.assertNotNull(p);
  }

}
