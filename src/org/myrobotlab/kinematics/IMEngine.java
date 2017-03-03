/**
 * 
 */
package org.myrobotlab.kinematics;

import java.util.ArrayList;

import org.myrobotlab.genetic.Chromosome;
import org.myrobotlab.genetic.Genetic;
import org.myrobotlab.genetic.GeneticAlgorithm;
import org.myrobotlab.kinematics.CollisionDectection.CollisionResults;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.IntegratedMovement2;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.Servo.IKData;
import org.python.jline.internal.Log;

/**
 * @author Christian
 *
 */
public class IMEngine extends Thread implements Genetic {
	
	DHRobotArm arm, computeArm;
	private String name;
	public Point target = null;
	private double maxDistance = 5.0;
	private Matrix inputMatrix = null;
	private IntegratedMovement2 service = null;
	private boolean noUpdatePosition = false;
	private boolean holdTargetEnabled = false;
	private CollisionDectection computeCD;
  private int tryCount = 0;;
  private Point oldTarget = null;
  private double timeToWait;
  private long lastTimeUpdate;
	
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
	    if (target != null && arm.getPalmPosition().distanceTo(target) < maxDistance && !holdTargetEnabled) {
	      target = null;
	    }
			Point avoidPoint = checkCollision(arm, service.collisionItems);
      if (avoidPoint != null) {
        Point previousTarget = target;
        target = avoidPoint;
        move();
        target = previousTarget;
        lastPosition = arm.getPalmPosition();
      }
			if (target != null && arm.getPalmPosition().distanceTo(target) > maxDistance && System.currentTimeMillis() > lastTimeUpdate + timeToWait) {
				Log.info("distance to target {}", arm.getPalmPosition().distanceTo(target));
				Log.info(arm.getPalmPosition().toString());
				move();
				lastPosition = arm.getPalmPosition();
			}
			try {
				sleep(0);
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
    if (++tryCount > 500 && oldTarget != null) {
      target = oldTarget;
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
    double iterStep = 0.001;
    double errorThreshold = 0.05;
    int maxIterations = 50;
    int geneticPoolSize=100;
    double geneticRecombinationRate = 0.7;
    double geneticMutationRate = 0.01;
    int geneticGeneration = 50;
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
        Log.info(computeArm.getPalmPosition().toString());
        GeneticAlgorithm GA = new GeneticAlgorithm(this, geneticPoolSize, arm.getNumLinks(), 11, geneticRecombinationRate, geneticMutationRate );
        Chromosome bestFit = GA.doGeneration(geneticGeneration); // this is the number of time the chromosome pool will be recombined and mutate
        for (int i = 0; i < computeArm.getNumLinks(); i++) {
          if (bestFit.getDecodedGenome().get(i) != null) {
            DHLink link = computeArm.getLink(i);
            double degrees = link.getPositionValueDeg();
            double deltaDegree = java.lang.Math.abs(degrees - (Double)bestFit.getDecodedGenome().get(i));
            if (degrees > ((Double)bestFit.getDecodedGenome().get(i))) {
              degrees -= deltaDegree;
            }
            else if (degrees < ((Double)bestFit.getDecodedGenome().get(i))) {
              degrees += deltaDegree;
            }
            link.addPositionValue( degrees);
          }
        }
        if (!holdTargetEnabled && goal == target) {
          //target = null;
        }
//        Log.info(computeArm.getPalmPosition().toString());
//        Log.info("Attempted to iterate there, but didn't make it. giving up.");
        // we shouldn't publish if we don't solve!
        return true;
      }
      // TODO: what if its unreachable!
      Point currentPos = computeArm.getPalmPosition();
      Log.debug("Current Position " + currentPos);
      // vector to destination
      Point deltaPoint = goal.subtract(currentPos);
      //iterStep = .9/currentPos.distanceTo(goal);
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
//      Point avoidPoint = checkCollision(computeArm, computeCD);
//      if (avoidPoint != null) {
//      	goal = avoidPoint;
//      }
      
     
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
      Point armPos = arm.getPalmPosition();
      Point newPos = arm.getPalmPosition();
      Point vCollItem = collisionResult.collisionPoints[itemIndex].subtract(collisionResult.collisionPoints[1-itemIndex]);
      //if (vCollItem.magnitude() > 100){ // scale vector so the avoiding point is not too far
        vCollItem = vCollItem.unitVector(100);
      //}
      newPos = newPos.add(vCollItem);
      Point ori=collisionResult.collisionItems[1-itemIndex].getOrigin();
      Point end=collisionResult.collisionItems[1-itemIndex].getEnd();
      Point colPoint = collisionResult.collisionPoints[1-itemIndex];
      if (collisionResult.collisionLocation[1-itemIndex] > 0.0 || collisionResult.collisionLocation[1-itemIndex] < 1.0) { // collision on the side of item
        Point vToEndOfObject;
      	if (collisionResult.collisionLocation[1-itemIndex] < 0.5) { //collision near the origin
      	  vToEndOfObject = ori.subtract(colPoint);
      		//newPos = newPos.add(ori).subtract(colPoint);
      	}
      	else { //collision near the end
      		//newPos = newPos.add(end).subtract(colPoint);
      	  vToEndOfObject = end.subtract(colPoint);
      	}
      	vToEndOfObject = vToEndOfObject.unitVector(100);
      	newPos = newPos.add(vToEndOfObject);
      }
      //move away  of the part
//      double length = collisionResult.collisionItems[1-itemIndex].getLength();
//      double ratio = collisionResult.collisionItems[itemIndex].getRadius() / length;
//      double[] vector = collisionResult.collisionItems[1-itemIndex].getVector();
//      for (int i=0; i<3; i++){
//      	vector[i] *= ratio;
//      }
//      if (collisionResult.collisionLocation[1-itemIndex] < 0.5) { //collision near the origin
//        newPos.setX(newPos.getX() - vector[0]);
//        newPos.setY(newPos.getY() - vector[1]);
//        newPos.setZ(newPos.getZ() - vector[2]);
//      }
//      else {
//        newPos.setX(newPos.getX() + vector[0]);
//        newPos.setY(newPos.getY() + vector[1]);
//        newPos.setZ(newPos.getZ() + vector[2]);
//      }     
      //add a vector end point move toward the collision point
      Point vtocollpoint = armPos.subtract(colPoint);
      vtocollpoint = vtocollpoint.unitVector(100);
      newPos = newPos.add(vtocollpoint);
//      double distance = newPos.distanceTo(arm.getPalmPosition());
//      if (distance > 100) {
//        Point vtonewPos = arm.getPalmPosition().subtract(newPos);
//        vtonewPos = vtonewPos.multiplyXYZ(100/distance);
//        newPos = arm.getPalmPosition().add(vtonewPos);
//      }
      Log.info("Avoiding position toward ",newPos.toString());
      return newPos;
    }
    return null;
	}

	private void publishAngles() {
	  timeToWait = 0;
		for (DHLink link : computeArm.getLinks()) {
		  double timeToMove = 0;
		  if (link.hasServo && link.getVelocity() > 0){
		    timeToMove = Math.abs(link.getCurrentPos() - link.getPositionValueDeg()) / link.getVelocity() * 1000;
		  }
		  if (timeToMove > timeToWait) {
		    timeToWait = timeToMove;
		  }
			service.sendAngles(link.getName(), link.getPositionValueDeg());
			lastTimeUpdate = System.currentTimeMillis();
			//if (link.hasServo) waitForServo ++;
		}
	}


	public void moveTo(Point point) {
		target  = point;
		oldTarget = arm.getPalmPosition();
		tryCount = 0;
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
		    l.addPositionValue(data.pos);
		    l.setState(data.state);
		    l.setVelocity(data.velocity);
		    l.setTargetPos(data.targetPos);
		    l.setCurrentPos(data.pos);
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

  @Override
  public void calcFitness(ArrayList<Chromosome> chromosomes) {
    for (Chromosome chromosome : chromosomes) {
      DHRobotArm newArm = new DHRobotArm();
      double fitnessMult = 1;
      double fitnessTime = 0;
      for (int i = 0; i < computeArm.getNumLinks(); i++){
        //copy the value of the currentArm
        DHLink newLink = new DHLink(computeArm.getLink(i));
        if (chromosome.getDecodedGenome().get(i) != null) {
          newLink.addPositionValue((double)chromosome.getDecodedGenome().get(i));
          Double delta = computeArm.getLink(i).getPositionValueDeg() - (Double)chromosome.getDecodedGenome().get(i);
          double timeOfMove = Math.abs(delta / newLink.getVelocity());
          if (timeOfMove > fitnessTime) {
            fitnessTime = timeOfMove;
          }
        }
        newArm.addLink(newLink);
      }
      Point potLocation = newArm.getPalmPosition();
      Double distance = potLocation.distanceTo(target);
      if (fitnessTime < 0.1) {
        fitnessTime = 0.1;
      }
      //fitness is the score showing how close the results is to the target position
      Double fitness = (fitnessMult/distance*1000);// + (1/fitnessTime*.01);
      if (fitness.isNaN()){
        int i=0;
        i = i;
      }
      if (fitness < 0) fitness *=-1;
      chromosome.setFitness(fitness);
    }
    return;  }

  @Override
  public void decode(ArrayList<Chromosome> chromosomes) {
    for (Chromosome chromosome : chromosomes ){
      int pos=0;
      ArrayList<Object>decodedGenome = new ArrayList<Object>();
      for (DHLink link: computeArm.getLinks()){
        if (!link.hasServo) {
          decodedGenome.add(null);
          continue;
        }
        
        Mapper map = null;
        if(link.servoMin == link.servoMax) {
          decodedGenome.add(link.servoMin);
          continue;
        }
        else {
          map = new Mapper(0,2047,link.servoMin,link.servoMax);
        }
        Double value=0.0;
        for (int i= pos; i< chromosome.getGenome().length() && i < pos+11; i++){
          if(chromosome.getGenome().charAt(i) == '1') value += 1 << i-pos; 
        }
        pos += 11;
        value = map.calcOutput(value);
        if (value.isNaN()) {
          value = link.getPositionValueDeg();
        }
        //if (value < MathUtils.radToDeg(link.getMin()-link.getInitialTheta())) value = link.getPositionValueDeg();
        //if (value > MathUtils.radToDeg(link.getMax()-link.getInitialTheta())) value = link.getPositionValueDeg();
        decodedGenome.add(value);
      }
      chromosome.setDecodedGenome(decodedGenome);
    }
  }

}
