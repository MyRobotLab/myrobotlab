/**
 * 
 */
package org.myrobotlab.kinematics;

import java.util.ArrayList;

import org.myrobotlab.genetic.Chromosome;
import org.myrobotlab.genetic.Genetic;
import org.myrobotlab.genetic.GeneticAlgorithm;
import org.myrobotlab.genetic.GeneticParameters;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.IntegratedMovement2;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.Servo.IKData;
import org.python.jline.internal.Log;

/**
 * @author Christian
 *
 */
public class IMEngine extends Thread {
	
	DHRobotArm arm;
	private String name;
	private Point target = null;
	private double maxDistance = 5.0;
	private Matrix inputMatrix = null;
	private IntegratedMovement2 service = null;
	private double time;
	private boolean noUpdatePosition = false;
	private boolean waitForServo = false;
	
	public IMEngine(String name, IntegratedMovement2 IM) {
		this.name = name;
		arm = new DHRobotArm();
		arm.name = name;
		service  = IM;
		this.start();
	}
	
	public IMEngine(String name, DHRobotArm arm, IntegratedMovement2 IM) {
		this.arm = arm;
		this.name = name;
		service  = IM;
		this.start();
	}
	
	public DHRobotArm getDHRobotArm() {
		return arm;
	}

	public void setDHRobotArm(DHRobotArm dhArm) {
		arm = dhArm;
	}
	
	public void run() {
		while(true){
			if (target != null && arm.getPalmPosition().distanceTo(target) > maxDistance && !waitForServo) {
				Log.info("distance to target {}", arm.getPalmPosition().distanceTo(target));
				Log.info(arm.getPalmPosition().toString());
				move();
			}
			try {
				sleep(00);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void move() {
		//noUpdatePosition = true;
    if (inputMatrix != null) {
      target = rotateAndTranslate(target);
    }
		boolean success = moveToGoal(target);
    //Log.info("Moving to {}", arm.getPalmPosition());
    //target = null;
    if (success) {
    	publishAngles();
    }
    //noUpdatePosition = false;
	}

	private boolean moveToGoal(Point goal) {
    // we know where we are.. we know where we want to go.
    int numSteps = 0;
    double iterStep = 0.025;
    double errorThreshold = 0.05;
    int maxIterations = 10000;
    double totalTime = 0;
    // what's the current point
    while (true) {
      numSteps++;
      if (numSteps >= maxIterations) {
        Log.info("Attempted to iterate there, but didn't make it. giving up.");
        // we shouldn't publish if we don't solve!
        return true;
      }
      // TODO: what if its unreachable!
      Point currentPos = arm.getPalmPosition();
      Log.debug("Current Position " + currentPos);
      // vector to destination
      Point deltaPoint = goal.subtract(currentPos);
      Matrix dP = new Matrix(3, 1);
      dP.elements[0][0] = deltaPoint.getX();
      dP.elements[1][0] = deltaPoint.getY();
      dP.elements[2][0] = deltaPoint.getZ();
      // scale a vector towards the goal by the increment step.
      dP = dP.multiply(iterStep);
      Matrix dTheta = null;
      //try {
      	Matrix jInverse = arm.getJInverse();
      	dTheta = jInverse.multiply(dP);
      //}
      //catch (NullPointerException e){
      	
      //}
      // why is this zero?
      
      Log.debug("delta Theta + " + dTheta);
      double maxTimeToMove = 0;
      for (int i = 0; i < dTheta.getNumRows(); i++) {
      	DHLink link = arm.getLink(i);
      	if (link.hasServo) {
      		if (link.getState() == Servo.SERVO_EVENT_STOPPED) {
            // update joint positions! move towards the goal!
            double d = dTheta.elements[i][0];
            // incr rotate needs to be min/max aware here!
            arm.getLink(i).incrRotate(d);
            double timeToMove = Math.abs(d / link.getVelocity());
            if (timeToMove > maxTimeToMove) {
            	maxTimeToMove = timeToMove;
            }
      		}
      	}
      }
      for (DHLink link : arm.getLinks()) {
      	if (link.hasServo && link.getState() == Servo.SERVO_EVENT_POSITION_UPDATE) {
      		double d = link.getVelocity() * maxTimeToMove;
      		if (link.getTargetPos() < link.getPositionValueDeg()) {
      			d *= -1;
      		}
      		link.incrRotate(d);
      	}
      }
      totalTime += maxTimeToMove;
      // delta point represents the direction we need to move in order to
      // get there.
      // we should figure out how to scale the steps.

      if (deltaPoint.magnitude() < errorThreshold) {
        // log.debug("Final Position {} Number of Iterations {}" ,
        // getPalmPosition() , numSteps);
      	target = null;
        break;
      }
      //Log.info(totalTime);
      if (totalTime > 0.1) {
      	break;
      }
    }
    return true;

	}

	private void publishAngles() {
		for (DHLink link : arm.getLinks()) {
			service.sendAngles(link.getName(), link.getPositionValueDeg());
		}
		waitForServo  = true;
	}


	public void moveTo(Point point) {
		target  = point;
		
	}

	/**
	 * @return the maxDistance
	 */
	public double getMaxDistance() {
		return maxDistance;
	}

	/**
	 * @param maxDistance the maxDistance to set
	 */
	public void setMaxDistance(double maxDistance) {
		this.maxDistance = maxDistance;
	}

	public void setInputMatrix(Matrix inputMatrix) {
		this.inputMatrix  = inputMatrix;
		
	}
	
  public Point rotateAndTranslate(Point pIn) {

    Matrix m = new Matrix(4, 1);
    m.elements[0][0] = pIn.getX();
    m.elements[1][0] = pIn.getY();
    m.elements[2][0] = pIn.getZ();
    m.elements[3][0] = 1;
    Matrix pOM = inputMatrix.multiply(m);

    // TODO: compute the roll pitch yaw
    double roll = 0;
    double pitch = 0;
    double yaw = 0;

    Point pOut = new Point(pOM.elements[0][0], pOM.elements[1][0], pOM.elements[2][0], roll, pitch, yaw);
    return pOut;
  }

	public void updateLinksPosition(IKData data) {
		if (noUpdatePosition) return;
	  for (DHLink l: arm.getLinks()) {
	  	if (l.getName().equals(data.name)){
		    l.addPositionValue(data.pos.doubleValue());
		    l.setState(data.state);
		    l.setVelocity(data.velocity);
		    l.setTargetPos(data.targetPos);
		    waitForServo = false;
		    //Log.info("{} - {}", l.getName(), data.pos, data.state);
		  }
		}
		
	}

}
