/**
 * 
 */
package org.myrobotlab.kinematics;

import org.myrobotlab.kinematics.CollisionDectection.CollisionResults;
import org.myrobotlab.service.IntegratedMovement2;
import org.myrobotlab.service.Servo.IKData;
import org.python.jline.internal.Log;

/**
 * @author Christian
 *
 */
public class IMEngine extends Thread {
	
	DHRobotArm arm, computeArm;
	private String name;
	public Point target = null;
	private double maxDistance = 5.0;
	private Matrix inputMatrix = null;
	private IntegratedMovement2 service = null;
	private boolean noUpdatePosition = false;
	private boolean holdTargetEnabled = false;
	private CollisionDectection computeCD;
	
	public IMEngine(String name, IntegratedMovement2 IM) {
		super(name);
		this.name = name;
		arm = new DHRobotArm();
		arm.name = name;
		service  = IM;
		computeCD = service.collisionItems.clones();
		//this.start();
	}
	
	public IMEngine(String name, DHRobotArm arm, IntegratedMovement2 IM) {
		super(name);
		this.arm = arm;
		this.name = name;
		service  = IM;
		computeCD = service.collisionItems.clones();
		//this.start();
	}
	
	public DHRobotArm getDHRobotArm() {
		return arm;
	}

	public void setDHRobotArm(DHRobotArm dhArm) {
		arm = dhArm;
	}
	
	public void run() {
		Point lastPosition = arm.getPalmPosition();
		while(true){
			Point avoidPoint = checkCollision(arm, service.collisionItems);
			if (target != null && arm.getPalmPosition().distanceTo(target) > maxDistance /** && arm.armMovementEnds()**/) {
				Log.info("distance to target {}", arm.getPalmPosition().distanceTo(target));
				Log.info(arm.getPalmPosition().toString());
				move();
				lastPosition = arm.getPalmPosition();
			}
			else if (lastPosition != arm.getPalmPosition() ){
//				Point avoidPoint = checkCollision(arm, service.collisionItems);
				//if (this.name.equals("leftArm")) Log.info(arm.getPalmPosition().toString());
				if (avoidPoint != null) {
					Point previousTarget = target;
					target = avoidPoint;
					move();
					target = previousTarget;
					lastPosition = arm.getPalmPosition();
				}
			}
			try {
				sleep(50);
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
    double iterStep = 0.01;
    double errorThreshold = 0.05;
    int maxIterations = 500;
    try {
      sleep(0);
    }
    catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
      computeArm = (DHRobotArm) this.arm.clones();
    // what's the current point
    while (true) {
    	//checkCollision(arm,service.collisionItems);
      computeCD = service.collisionItems.clones();
      numSteps++;
      if (numSteps >= maxIterations) {
        Log.info("Attempted to iterate there, but didn't make it. giving up.");
        // we shouldn't publish if we don't solve!
        return true;
      }
      // TODO: what if its unreachable!
      Point currentPos = computeArm.getPalmPosition();
      Log.debug("Current Position " + currentPos);
      // vector to destination
      Point deltaPoint = goal.subtract(currentPos);
      iterStep = .8/currentPos.distanceTo(goal);
      Matrix dP = new Matrix(3, 1);
      dP.elements[0][0] = deltaPoint.getX();
      dP.elements[1][0] = deltaPoint.getY();
      dP.elements[2][0] = deltaPoint.getZ();
      // scale a vector towards the goal by the increment step.
      dP = dP.multiply(iterStep);
      Matrix dTheta = null;
      //try {
      	Matrix jInverse = computeArm.getJInverse();
      	dTheta = jInverse.multiply(dP);
      //}
      //catch (NullPointerException e){
    	if (dTheta == null){
        dTheta = new Matrix(computeArm.getNumLinks(), 1);
        for (int i = 0; i < computeArm.getNumLinks(); i++) {
          dTheta.elements[i][0] = 0.000001;
        }
      }
      // why is this zero?
      
      //Log.info("delta Theta + " + dTheta);
      double maxTimeToMove = 0;
      for (int i = 0; i < dTheta.getNumRows(); i++) {
//      	if (dTheta.elements[i][0] > 0.0003){
//      		Log.info("big increment");
//      	}
      	DHLink link = computeArm.getLink(i);
      	if (link.hasServo) {
            // update joint positions! move towards the goal!
            double d = dTheta.elements[i][0];
            // incr rotate needs to be min/max aware here!
            computeArm.getLink(i).incrRotate(d);
            double timeToMove = Math.abs(d / link.getVelocity());
            if (timeToMove > maxTimeToMove) {
            	maxTimeToMove = timeToMove;
            }
      	}
      }
      Point avoidPoint = checkCollision(computeArm, computeCD);
      if (avoidPoint != null) {
      	goal = avoidPoint;
      }
      
     
      // delta point represents the direction we need to move in order to
      // get there.
      // we should figure out how to scale the steps.

      if (deltaPoint.magnitude() < errorThreshold) {
        // log.debug("Final Position {} Number of Iterations {}" ,
        // getPalmPosition() , numSteps);
      	if (!holdTargetEnabled && goal == target) {
      		target = null;
      	}
        break;
      }
      //Log.info(totalTime);
//      if (totalTime > 0.05) {
//      	break;
//      }
    }
    return true;

	}

	private Point checkCollision(DHRobotArm arm, CollisionDectection cd) {
    double[][] jp = createJointPositionMap();
    //send data to the collision detector class
    for (int i = 0; i < arm.getNumLinks(); i++) {
      CollisionItem ci = new CollisionItem(new Point(jp[i][0], jp[i][1], jp[i][2], 0 , 0, 0), new Point(jp[i+1][0], jp[i+1][1], jp[i+1][2], 0, 0, 0), arm.getLink(i).getName());
      if (i != arm.getNumLinks()-1) {
        ci.addIgnore(arm.getLink(i+1).getName());
      }
      cd.addItem(ci);
    }
    CollisionResults collisionResult = cd.runTest();
    if (collisionResult.haveCollision) {
    	//Log.info("collision detected");
      CollisionItem ci = null;
      int itemIndex = 0;
      for (DHLink l : arm.getLinks()) {
    	boolean foundIt = false;
        for (itemIndex = 0; itemIndex < 2; itemIndex++) {
          if (l.getName().equals(collisionResult.collisionItems[itemIndex].getName())) {
            ci = collisionResult.collisionItems[itemIndex];
            foundIt = true;
            break;
          }
        }
        if (foundIt) break; //we have the item to watch
      }
      if (ci == null) {
        //Log.info("Collision between static item {} and {} detected", collisionResult.collisionItems[0].getName(), collisionResult.collisionItems[1].getName());
        return null;
      }
      Point newPos = arm.getPalmPosition();
      newPos = newPos.add(collisionResult.collisionPoints[itemIndex].subtract(collisionResult.collisionPoints[1-itemIndex]));//not sure this is ok
      Point ori=collisionResult.collisionItems[1-itemIndex].getOrigin();
      Point end=collisionResult.collisionItems[1-itemIndex].getEnd();
      Point colPoint = collisionResult.collisionPoints[1-itemIndex];
      if (collisionResult.collisionLocation[1-itemIndex] > 0.0 || collisionResult.collisionLocation[1-itemIndex] < 1.0) { // collision on the side of item
      	if (collisionResult.collisionLocation[1-itemIndex] < 0.5) { //collision near the origin
      		newPos = newPos.add(ori).subtract(colPoint);
//          newPos.setX(newPos.getX()+ori.getX()-colPoint.getX());
//          newPos.setY(newPos.getY()+ori.getY()-colPoint.getY());
//          newPos.setZ(newPos.getZ()+ori.getZ()-colPoint.getZ());
      	}
      	else { //collision near the end
      		newPos = newPos.add(end).subtract(colPoint);
      	}
      }
      //move away  of the part
      double length = collisionResult.collisionItems[1-itemIndex].getLength();
      double ratio = collisionResult.collisionItems[itemIndex].getRadius() / length;
      double[] vector = collisionResult.collisionItems[1-itemIndex].getVector();
      for (int i=0; i<3; i++){
      	vector[i] *= ratio;
      }
      if (collisionResult.collisionLocation[1-itemIndex] < 0.5) { //collision near the origin
        newPos.setX(newPos.getX() - vector[0]);
        newPos.setY(newPos.getY() - vector[1]);
        newPos.setZ(newPos.getZ() - vector[2]);
      }
      else {
        newPos.setX(newPos.getX() + vector[0]);
        newPos.setY(newPos.getY() + vector[1]);
        newPos.setZ(newPos.getZ() + vector[2]);
      }     
      //add a vector end point move toward the collision point
      Point vtocollpoint = arm.getPalmPosition().subtract(colPoint);
      newPos = newPos.add(vtocollpoint);
      Log.info("Avoiding position toward ",newPos.toString());
      return newPos;
    }
    return null;
	}

	private void publishAngles() {
		for (DHLink link : computeArm.getLinks()) {
			service.sendAngles(link.getName(), link.getPositionValueDeg());
			//if (link.hasServo) waitForServo ++;
		}
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
		    //if (l.hasServo && l.getState() == Servo.SERVO_EVENT_STOPPED) waitForServo--;
		    //Log.info("{} - {}", l.getName(), data.pos, data.state);
		  }
		}
		
	}

  public double[][] createJointPositionMap() {

    double[][] jointPositionMap = new double[arm.getNumLinks() + 1][3];

    // first position is the origin... second is the end of the first link
    jointPositionMap[0][0] = 0;
    jointPositionMap[0][1] = 0;
    jointPositionMap[0][2] = 0;

    for (int i = 1; i <= arm.getNumLinks(); i++) {
      Point jp = arm.getJointPosition(i - 1);
      jointPositionMap[i][0] = jp.getX();
      jointPositionMap[i][1] = jp.getY();
      jointPositionMap[i][2] = jp.getZ();
    }
    return jointPositionMap;
  }

	public void holdTarget(boolean holdEnabled) {
		this.holdTargetEnabled  = holdEnabled;
		
	}

}
