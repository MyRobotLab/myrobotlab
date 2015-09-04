package org.myrobotlab.service;

import org.junit.Test;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.service.interfaces.SpeechSynthesis;

public class InverseKinematics3DTest {

	@Test
	public void testIK3D() throws Exception {
		
		InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d", "InverseKinematics3D");
		
		InMoovArm ia = new InMoovArm("foo");
		ik3d.setCurrentArm(ia.getDHRobotArm());
		
		ik3d.moveTo(50,50,50);
		
		Point p = ik3d.currentPosition();
		
		double[][]  positions = ik3d.createJointPositionMap();
	
		int x = positions[0].length;
		int y = positions.length;
		
		for (int j = 0; j < y; j++) {
			for (int i = 0; i < x; i++) {
				System.out.print( positions[j][i] + " ");
			}
			System.out.println();
		}

		// Last point:
		System.out.println("Last Point: " + p.toString());
		
		
	}
}
