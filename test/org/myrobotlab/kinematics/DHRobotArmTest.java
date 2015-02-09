package org.myrobotlab.kinematics;

import static org.junit.Assert.*;

import org.junit.Test;

public class DHRobotArmTest {

	@Test
	public void testDHArm() {

		// distance to common normal
		double d=0.4;
		// comon normal lenght (sometimes called "a"
		double r=0.2;
		// angle between X and X-1 axis
		double alpha=90.0 * Math.PI / 180.0;
		// angle between Z and Z-1 axis
		double theta=45.0 * Math.PI / 180.0;
		DHLink link1 = new DHLink(d, r, theta, alpha);

	//	double d1=0;
	//	double r1=0;
	//	double theta1=0.0 * Math.PI / 180.0;
	//	double alpha1=0.0 * Math.PI / 180.0;
//		
 	//	DHLink link2 = new DHLink(d1, r1, theta1, alpha1);
		
		DHRobotArm arm = new DHRobotArm();
		arm.addLink(link1);
  	//	arm.addLink(link2);
		
		// TODO: validate forward kinematcis
		Point coord = arm.getPalmPosition();
		//System.out.println("Theta = " + theta);
		//System.out.println(coord);
		//
		//double angle = 90;
		//link1.moveToAngle(angle);
		
		// you want to know where the hand is.
		//coord = arm.getPalmPosition();
		
		//System.out.println(coord);
		// assertEquals(coord.toString(),"(2.0, 1.0, 1.0)" );
		assertEquals("(x=0.141, y=0.141, z=0.400)", coord.toString());
		
	}
}
