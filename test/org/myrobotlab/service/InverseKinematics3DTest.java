package org.myrobotlab.service;

import org.junit.Test;
import org.myrobotlab.kinematics.Point;

// @Ignore
public class InverseKinematics3DTest {

  @Test
  public void testForwardKinematics() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d", "InverseKinematics3D");
    // InMoovArm ia = new InMoovArm("i01");
    ik3d.setCurrentArm(InMoovArm.getDHRobotArm());
    ik3d.centerAllJoints();
    System.out.println(ik3d.getCurrentArm().getPalmPosition());
  }

  @Test
  public void testIK3D() throws Exception {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d", "InverseKinematics3D");
    // InMoovArm ia = new InMoovArm("i01");
    ik3d.setCurrentArm(InMoovArm.getDHRobotArm());
    // start from a centered joint configuration so we can iterate without
    // loosing rank
    // in our jacobian!
    ik3d.centerAllJoints();
    ik3d.moveTo(100.0, 0.0, 50.0);
    Point p = ik3d.currentPosition();
    double[][] positions = ik3d.createJointPositionMap();
    int x = positions[0].length;
    int y = positions.length;
    for (int j = 0; j < y; j++) {
      for (int i = 0; i < x; i++) {
        System.out.print(positions[j][i] + " ");
      }
      System.out.println();
    }

    // Last point:
    System.out.println("Last Point: " + p.toString());

  }
}
