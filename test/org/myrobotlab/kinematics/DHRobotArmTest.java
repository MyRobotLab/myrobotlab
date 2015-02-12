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

	@Test
	public void testJacobian() {

		DHRobotArm testArm = createArm();
		Matrix jInverse = testArm.getJInverse();
		System.out.println(jInverse);

		// now, the deltaPosition array has the delta x,y,z coordinates 
		// what's the instantaneous rate of change for each of those
		// compute the rate of change for this

		// ok.		
		testArm.moveToGoal(new Point(0,1,0));
		

	}



	public DHRobotArm createArm() {
		DHRobotArm arm = new DHRobotArm();
		// d , r, theta , alpha
		
		DHLink link1 = new DHLink(0, 1, 45*Math.PI/180, 0);
		arm.addLink(link1);
		DHLink link2 = new DHLink(0.0, 0.2, 45*Math.PI/180, 90*Math.PI/180);
		arm.addLink(link2);

		return arm;
	}
}
