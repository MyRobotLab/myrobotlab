package org.myrobotlab.kinematics;

import static org.junit.Assert.*;

import org.junit.Test;

public class DHRobotArmTest {

	@Test
	public void testDHArm() {

		double d=1;
		double r=1;
		double theta=0.0 * Math.PI / 180.0;
		double alpha=-90.0 * Math.PI / 180.0;
		
		DHLink link1 = new DHLink(d, r, theta, alpha);

		double d1=1;
		double r1=1;
		double theta1=0.0 * Math.PI / 180.0;
		double alpha1=90 * Math.PI / 180.0;
		
		DHLink link2 = new DHLink(d1, r1, theta1, alpha1);
		
		DHRobotArm arm = new DHRobotArm();
		arm.addLink(link1);
		arm.addLink(link2);
		
		// TODO: validate forward kinematcis
		double angle = 1.2;
		//link1.moveToAngle(angle);
		
		// you want to know where the hand is.
		Point coord = arm.getPalmPosition();
		
		System.out.println(coord);
		assertEquals(coord.toString(),"(2.0, 1.0, 1.0)" );
		
	}
}
