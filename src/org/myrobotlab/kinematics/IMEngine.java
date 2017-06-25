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
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.IntegratedMovement;
import org.myrobotlab.service.IntegratedMovement.ObjectPointLocation;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.Servo.IKData;
import org.python.jline.internal.Log;

/**
 * @author Christian
 *
 */
public class IMEngine extends Thread implements Genetic {
	
	DHRobotArm arm, computeArm;
	public Point target = null;
	private double maxDistance = 5.0;
	private Matrix inputMatrix = null;
	private transient IntegratedMovement service = null;
	private boolean noUpdatePosition = false;
	private boolean holdTargetEnabled = false;
  private int tryCount = 0;;
  private Point oldTarget = null;
  private double timeToWait;
  private long lastTimeUpdate;
  private int Ai = IntegratedMovement.Ai.AVOID_COLLISION.value;
  
  public class MoveInfo {    
    Point offset = null;    
    CollisionItem targetItem = null;    
    ObjectPointLocation objectLocation = null;    
    DHLink lastLink = null;
    public String arm;
    public String lastLinkName;   
  }   

  MoveInfo moveInfo = null;
  private CalcFitnessType calcFitnessType;
  private int cogRetry;
  private String lastDHLink;
  
  private enum CalcFitnessType {
    POSITION,
    COG;
  }
	
	public IMEngine(String name, IntegratedMovement IM) {
		super(name);
		arm = new DHRobotArm();
		arm.name = name;
		service  = IM;
	}
	
	public IMEngine(String name, DHRobotArm arm, IntegratedMovement integratedMovement) {
		super(name);
		this.arm = arm;
		service  = integratedMovement;
	}
	
	public DHRobotArm getDHRobotArm() {
		return arm;
	}

	public void setDHRobotArm(DHRobotArm dhArm) {
		arm = dhArm;
	}
	
  public void run() {
		while(true){
      Point currentPosition = arm.getPalmPosition(lastDHLink);
      if (AiActive(IntegratedMovement.Ai.AVOID_COLLISION)) {
  			Point avoidPoint = checkCollision(arm, service.collisionItems);
        if (avoidPoint != null) {
          Point previousTarget = target;
          target = avoidPoint;
          move();
          cogRetry = 0;
          target = previousTarget;
        }
      }
      if (target == null && !isWaitingForServo() && AiActive(IntegratedMovement.Ai.KEEP_BALANCE)) {
        target = checkCoG();
        if(target != null){
          //move();
          target = null;
        }
      }
			if (target != null && currentPosition.distanceTo(target) > maxDistance /**&& !isWaitingForServo()**/) {
				Log.info("distance to target {}", currentPosition.distanceTo(target));
				Log.info(currentPosition.toString());
				move(lastDHLink);
				cogRetry = 0;
				continue;
			}
			if (target != null && currentPosition.distanceTo(target) < maxDistance && !AiActive(IntegratedMovement.Ai.HOLD_POSITION) && !isWaitingForServo()) {
			  Point cog = service.cog.computeCoG(null);
			  if (AiActive(IntegratedMovement.Ai.KEEP_BALANCE) && cog.distanceTo(service.cog.getCoGTarget()) > service.cog.getMaxDistanceToCog()) {
			    
			  }
			  else {
  			  target = null;
  			  moveInfo = null;
			  }
			}
		}
	}
  
  private Point checkCoG() {
    Point cog = service.cog.computeCoG(null);
    double deltaDegree = 0.1;
    if (cogRetry > 10) {
      return null;
    }
    cog.setZ(0.0);
    //cog.setY(0.0);
    //if (cog.getZ()==0.0) return null;
    Point cogTarget = service.cog.getCoGTarget();
    if (cog.distanceTo(cogTarget) > service.cog.getMaxDistanceToCog()) {
      int maxIter = 100;
      computeArm = new DHRobotArm(arm);
      CollisionDectection cd = new CollisionDectection(service.collisionItems);
      for (int i = 0; i < maxIter; i++) {
        for (DHLink link:computeArm.getLinks()){
          if (link.getState()!=Servo.SERVO_EVENT_STOPPED) {
            link.addPositionValue(link.getTargetPos());
            continue;
          }
        }
        for (int j = computeArm.getNumLinks()-1; j >=0 ; j--) {
        //for (int j = 0; j < computeArm.getNumLinks(); j++) {
          if (computeArm.getLink(j).getState()!=Servo.SERVO_EVENT_STOPPED) {
            continue;
          }
          
          Point cogIni = service.cog.computeCoG(cd);
          cogIni.setZ(0.0);
          //cogIni.setY(0.0);
          computeArm.getLink(j).incrRotate(MathUtils.degToRad(deltaDegree));
          double[][] jp = computeArm.createJointPositionMap();
          for (int k = computeArm.getNumLinks()-1; k >= j; k--) {
          //for (int k = j; k < computeArm.getNumLinks(); k++) {
            CollisionItem ci = new CollisionItem(new Point(jp[k][0], jp[k][1], jp[k][2], 0 , 0, 0), new Point(jp[k+1][0], jp[k+1][1], jp[k+1][2], 0, 0, 0), computeArm.getLink(k).getName());
            if (k != computeArm.getNumLinks()-1) {
              ci.addIgnore(computeArm.getLink(k+1).getName());
            }
            cd.addItem(ci);
          }
          Point deltaCoG = service.cog.computeCoG(cd);
          deltaCoG.setZ(0.0);
          //deltaCoG.setY(0.0);
          if (cogTarget.distanceTo(cogIni) > cogTarget.distanceTo(deltaCoG)) {
            if (cogTarget.distanceTo(deltaCoG) < service.cog.getMaxDistanceToCog()*0.8) {
              publishAngles();
              cogRetry=0;
              return computeArm.getPalmPosition();
            }
            continue;
          }
          computeArm.getLink(j).incrRotate(MathUtils.degToRad(-2*deltaDegree));
          jp = computeArm.createJointPositionMap();
          for (int k = computeArm.getNumLinks()-1; k >= j; k--) {
            CollisionItem ci = new CollisionItem(new Point(jp[k][0], jp[k][1], jp[k][2], 0 , 0, 0), new Point(jp[k+1][0], jp[k+1][1], jp[k+1][2], 0, 0, 0), computeArm.getLink(k).getName());
            if (k != computeArm.getNumLinks()-1) {
              ci.addIgnore(computeArm.getLink(k+1).getName());
            }
            cd.addItem(ci);
          }
          deltaCoG = service.cog.computeCoG(cd);
          deltaCoG.setZ(0.0);
          //deltaCoG.setY(0.0);
          if (cogTarget.distanceTo(cogIni) > cogTarget.distanceTo(deltaCoG)) {
            if (cogTarget.distanceTo(deltaCoG) < service.cog.getMaxDistanceToCog()*0.8) {
              publishAngles();
              cogRetry=0;
              return computeArm.getPalmPosition();
            }
            continue;
          }
          computeArm.getLink(j).incrRotate(MathUtils.degToRad(deltaDegree));
        }
      }
//      int geneticPoolSize=100;
//      double geneticRecombinationRate = 0.7;
//      double geneticMutationRate = 0.01;
//      int geneticGeneration = 50;
//      calcFitnessType = CalcFitnessType.POSITION;
//      GeneticAlgorithm GA = new GeneticAlgorithm(this, geneticPoolSize, arm.getNumLinks(), 12, geneticRecombinationRate, geneticMutationRate );
//      Chromosome bestFit = GA.doGeneration(geneticGeneration); // this is the number of time the chromosome pool will be recombined and mutate
//      for (int i = 0; i < computeArm.getNumLinks(); i++) {
//        if (bestFit.getDecodedGenome().get(i) != null) {
//          DHLink link = computeArm.getLink(i);
//          double degrees = link.getPositionValueDeg();
//          double deltaDegree = java.lang.Math.abs(degrees - (double)bestFit.getDecodedGenome().get(i));
//          if (degrees > ((double)bestFit.getDecodedGenome().get(i))) {
//            degrees -= deltaDegree;
//          }
//          else if (degrees < ((double)bestFit.getDecodedGenome().get(i))) {
//            degrees += deltaDegree;
//          }
//          link.addPositionValue( degrees);
//        }
//      }
//      double[][] jp = computeArm.createJointPositionMap();
//      for (int k = 0; k < computeArm.getNumLinks(); k++) {
//        CollisionItem ci = new CollisionItem(new Point(jp[k][0], jp[k][1], jp[k][2], 0 , 0, 0), new Point(jp[k+1][0], jp[k+1][1], jp[k+1][2], 0, 0, 0), computeArm.getLink(k).getName());
//        if (k != computeArm.getNumLinks()-1) {
//          ci.addIgnore(computeArm.getLink(k+1).getName());
//        }
//        cd.addItem(ci);
//      }
//      Point deltaCoG = service.cog.computeCoG(cd);
//      deltaCoG.setZ(0.0);
//      //deltaCoG.setY(0.0);
//      if (deltaCoG.distanceTo(service.cog.getCoGTarget()) < service.cog.getMaxDistanceToCog()) {
//        Point pos = computeArm.getPalmPosition();
//        Log.info("Moving to " + getName() + pos.toString());
//        //publishAngles();
//        return computeArm.getPalmPosition();
//      }
      return computeArm.getPalmPosition();
      //cogRetry++;
    }
    return null;
  }

  private boolean isWaitingForServo() {
    if (System.currentTimeMillis() > lastTimeUpdate + timeToWait) {
      return false;
    }
    //return true;
    return false;
  }
  
  private boolean AiActive(IntegratedMovement.Ai ai) {
    if ((Ai & ai.value) > 0) {
      return true;
    }
    return false;
  }
  
  private void move() {
    move(null);
  }

	private void move(String lastDHLink) {
		//noUpdatePosition = true;
    if (inputMatrix != null) {
      target = rotateAndTranslate(target);
    }
    if (++tryCount > 500 && oldTarget != null) {
      target = oldTarget;
    }
		boolean success = moveToGoal(target, lastDHLink);
    //Log.info("Moving to {}", arm.getPalmPosition());
    //target = null;
    if (success) {
    	publishAngles();
    }
    //noUpdatePosition = false;
	}

	private boolean moveToGoal(Point goal) {
	  return moveToGoal(goal, null);
	}
	
	private boolean moveToGoal(Point goal, String lastDHLink) {
    // we know where we are.. we know where we want to go.
    int numSteps = 0;
    double iterStep = 0.01;
    double errorThreshold = 0.5;
    int maxIterations = 10000;
    int geneticPoolSize=100;
    double geneticRecombinationRate = 0.7;
    double geneticMutationRate = 0.01;
    int geneticGeneration = 50;
    computeArm = new DHRobotArm(arm);
    // what's the current point
    while (true) {
    	//checkCollision(arm,service.collisionItems);
      numSteps++;
      if (numSteps >= maxIterations) {
        //if (numSteps >= maxIterations) return true;
        //Log.info(computeArm.getPalmPosition().toString() + "genetic");
        calcFitnessType = CalcFitnessType.POSITION;
        GeneticAlgorithm GA = new GeneticAlgorithm(this, geneticPoolSize, arm.getNumLinks(), 12, geneticRecombinationRate, geneticMutationRate );
        Chromosome bestFit = GA.doGeneration(geneticGeneration); // this is the number of time the chromosome pool will be recombined and mutate
        for (int i = 0; i < computeArm.getNumLinks(); i++) {
          if (bestFit.getDecodedGenome().get(i) != null) {
            DHLink link = computeArm.getLink(i);
            double degrees = link.getPositionValueDeg();
            double deltaDegree = java.lang.Math.abs(degrees - (double)bestFit.getDecodedGenome().get(i));
            if (degrees > ((double)bestFit.getDecodedGenome().get(i))) {
              degrees -= deltaDegree;
            }
            else if (degrees < ((double)bestFit.getDecodedGenome().get(i))) {
              degrees += deltaDegree;
            }
            link.addPositionValue( degrees);
          }
          if (computeArm.getLink(i).getName().equals(lastDHLink)) {
            break;
          }
        }
        return true;
      }
      Point currentPos = computeArm.getPalmPosition(lastDHLink);
      //Log.debug("Current Position " + currentPos);
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
      
      //Log.info("delta Theta + " + dTheta);
      double maxTimeToMove = 0;
      for (int i = 0; i < dTheta.getNumRows(); i++) {
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
        if (computeArm.getLink(i).getName().equals(lastDHLink)) {
          break;
        }
      }
      // delta point represents the direction we need to move in order to
      // get there.
      // we should figure out how to scale the steps.

      if (deltaPoint.magnitude() < errorThreshold) {
        break;
      }
    }
    return true;

	}

	private Point checkCollision(DHRobotArm arm, CollisionDectection cd) {
	  DHRobotArm checkArm = new DHRobotArm(arm);
	  double time = 0.0;
	  double timePerLoop = 0.1;
	  CollisionResults collisionResult = null;
	  CollisionDectection ccd = new CollisionDectection(cd);
	  while (time <= 2.0) {
	    //rotate the checkArm by timePerLoop
	    for (DHLink link : checkArm.getLinks()){
	      if (link.hasServo) {
  	      double delta = link.getVelocity() * timePerLoop;
  	      double maxDelta = Math.abs(link.getTargetPos() - link.getPositionValueDeg());
  	      delta = Math.toRadians(Math.min(delta, maxDelta));
  	      if (link.getTargetPos() < link.getCurrentPos()) {
  	        delta *= -1;
  	      }
  	      link.incrRotate(delta);
	      }
	    }
	    double[][] jp = checkArm.createJointPositionMap();
	    //send data to the collision detector class
	    for (int i = 0; i < checkArm.getNumLinks(); i++) {
	      CollisionItem ci = new CollisionItem(new Point(jp[i][0], jp[i][1], jp[i][2], 0 , 0, 0), new Point(jp[i+1][0], jp[i+1][1], jp[i+1][2], 0, 0, 0), checkArm.getLink(i).getName());
	      if (i != checkArm.getNumLinks()-1) {
	        ci.addIgnore(checkArm.getLink(i+1).getName());
	      }
	      ccd.addItem(ci);
	      if(time == 0.0){
	        cd.addItem(ci);
	      }
	    }
	    collisionResult = ccd.runTest();
	    if (collisionResult.haveCollision) {
	      break;
	    }
	    time += timePerLoop;
	  }
    if (collisionResult.haveCollision) {
    	//Log.info("collision detected");
      CollisionItem ci = null;
      int itemIndex = 0;
      for (DHLink l : checkArm.getLinks()) {
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
      Point armPos = checkArm.getPalmPosition();
      Point newPos = checkArm.getPalmPosition();
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
    moveTo(point, null);
  }
  
	public void moveTo(Point point, String lastDHLink) {
		target  = point;
		this.lastDHLink = lastDHLink;
		oldTarget = arm.getPalmPosition(lastDHLink);
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
          double delta = computeArm.getLink(i).getPositionValueDeg() - (double)chromosome.getDecodedGenome().get(i);
          double timeOfMove = Math.abs(delta / newLink.getVelocity());
          if (timeOfMove > fitnessTime) {
            fitnessTime = timeOfMove;
          }
        }
        newArm.addLink(newLink);
      }
      Point potLocation = newArm.getPalmPosition();
      if (calcFitnessType == CalcFitnessType.POSITION) {
        if (target == null) return;
        Double distance = potLocation.distanceTo(target);
        if (fitnessTime < 0.1) {
          fitnessTime = 0.1;
        }
        //fitness is the score showing how close the results is to the target position
        Double fitness = (fitnessMult/distance*1000);// + (1/fitnessTime*.01);
        if (fitness < 0) fitness *=-1;
        chromosome.setFitness(fitness);
      }
      else if (calcFitnessType == CalcFitnessType.COG) {
        //compute the COG of this potiental arm
        CollisionDectection cd = new CollisionDectection(service.collisionItems);
        DHRobotArm checkArm = new DHRobotArm(arm);
        for (int i = 0; i < checkArm.getNumLinks(); i++) {
          if (chromosome.getDecodedGenome().get(i) != null) {
            DHLink link = checkArm.getLink(i);
            double degrees = link.getPositionValueDeg();
            double deltaDegree = java.lang.Math.abs(degrees - (double)chromosome.getDecodedGenome().get(i));
            if (degrees > ((double)chromosome.getDecodedGenome().get(i))) {
              degrees -= deltaDegree;
            }
            else if (degrees < ((double)chromosome.getDecodedGenome().get(i))) {
              degrees += deltaDegree;
            }
            link.addPositionValue( degrees);
          }
        }
        double[][] jp = checkArm.createJointPositionMap();
        //send data to the collision detector class
        for (int i = 0; i < checkArm.getNumLinks(); i++) {
          CollisionItem ci = new CollisionItem(new Point(jp[i][0], jp[i][1], jp[i][2], 0 , 0, 0), new Point(jp[i+1][0], jp[i+1][1], jp[i+1][2], 0, 0, 0), checkArm.getLink(i).getName());
          if (i != checkArm.getNumLinks()-1) {
            ci.addIgnore(checkArm.getLink(i+1).getName());
          }
          cd.addItem(ci);
        }
        Point cog = service.cog.computeCoG(cd);
        //project the COG point to the X/Y plane
        cog.setZ(0.0);
        //cog.setY(0.0);
        //find a value that put the COG into the target area while minimizing deltaCoG
        Double fitness = 0.0;
        double deltaCog = cog.distanceTo(service.cog.getCoGTarget());
        if (deltaCog == 0) {
          fitness = 999999999.0;
        }
        else {
          fitness = 1/potLocation.distanceTo(cog);
          fitness = 1/deltaCog;
          if (deltaCog <= service.cog.getMaxDistanceToCog()) {
            fitness *= 100;
          }
        }
        if (arm.getPalmPosition().distanceTo(potLocation) > 10) {
          fitness = 0.01;
        }
        fitness = Math.abs(fitness)*100;
        chromosome.setFitness(fitness);
      }
    }
    return;
  }

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
        if (link.getState() == Servo.SERVO_EVENT_POSITION_UPDATE) {
          decodedGenome.add(link.getTargetPos());
          continue;
        }
        Mapper map = null;
        if(link.servoMin == link.servoMax) {
          decodedGenome.add(link.servoMin);
          continue;
        }
        else {
          map = new Mapper(0,8191,link.servoMin,link.servoMax);
        }
        Double value=0.0;
        for (int i= pos; i< chromosome.getGenome().length() && i < pos+13; i++){
          if(chromosome.getGenome().charAt(i) == '1') value += 1 << i-pos; 
        }
        pos += 13;
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

  public double[][] createJointPositionMap() {
    return arm.createJointPositionMap();
  }

  public void moveTo(CollisionItem item,ObjectPointLocation location, String lastDHLink) {
    moveInfo = new MoveInfo();    
    moveInfo.targetItem = item;   
    moveInfo.objectLocation = location; 
    moveInfo.lastLinkName = lastDHLink;
    this.lastDHLink = lastDHLink;
    if (moveInfo.targetItem == null){   
      Log.info("no items named ", item.getName(), "found");   
      moveInfo = null;    
      return;   
    }   
    target = moveToObject();
    service.getJmeApp().addPoint(target);
  }

  private Point moveToObject() {
    double safety = 10.0;
    Point[] point = new Point[2]; 
    if (moveInfo.lastLinkName == null) {
      moveInfo.lastLink = arm.getLink(arm.getNumLinks()-1);
    }
    else {
      for (DHLink link:arm.getLinks()) {
        if (link.getName() == moveInfo.lastLinkName) {
          moveInfo.lastLink = link;
          break;
        }
      }
    }
    CollisionItem lastLinkItem = service.collisionItems.getItem(moveInfo.lastLink.getName());   
    service.collisionItems.addIgnore(moveInfo.targetItem.name, lastLinkItem.name);
    Double[] vector = new Double[3];    
    boolean addRadius=false;    
    switch (moveInfo.objectLocation) {    
      case ORIGIN_CENTER: {   
        point[0] = moveInfo.targetItem.getOrigin();  
        Point v = moveInfo.targetItem.getOrigin().subtract(moveInfo.targetItem.getEnd());
        v = v.unitVector(safety);
        point[0] = point[0].add(v);
        break;    
      }   
      case END_CENTER: {    
        point[0] = moveInfo.targetItem.getEnd();    
        Point v = moveInfo.targetItem.getEnd().subtract(moveInfo.targetItem.getOrigin());
        v = v.unitVector(safety);
        point[0] = point[0].add(v);
        break;    
      }   
      case CLOSEST_POINT: {   
        point = service.collisionItems.getClosestPoint(moveInfo.targetItem, lastLinkItem, new Double[2], vector);   
        addRadius = true;   
        break;    
      }   
      case ORIGIN_SIDE: {   
        point[0] = moveInfo.targetItem.getOrigin();   
        Point v = moveInfo.targetItem.getOrigin().subtract(moveInfo.targetItem.getEnd());
        v = v.unitVector(safety);
        point[0] = point[0].add(v);
        addRadius = true;   
        break;    
      }   
      case END_SIDE: {    
        point[0] = moveInfo.targetItem.getEnd();    
        Point v = moveInfo.targetItem.getEnd().subtract(moveInfo.targetItem.getOrigin());
        v = v.unitVector(safety);
        point[0] = point[0].add(v);
        addRadius = true;   
        break;    
      }   
      case CENTER_SIDE:
      case LEFT_SIDE:
      case RIGHT_SIDE: {   
        point = service.collisionItems.getClosestPoint(moveInfo.targetItem, lastLinkItem, new Double[]{0.5, 0.5}, vector);    
        addRadius = true;   
      }   
      case CENTER: {    
        point = service.collisionItems.getClosestPoint(moveInfo.targetItem, lastLinkItem, new Double[]{0.5, 0.5}, vector);    
      }
    }   
    if(addRadius) {   
      double[] vectori = moveInfo.targetItem.getVector();   
      double[] vectorT = moveInfo.targetItem.getVectorT();    
      Point side0 = new Point(point[0].getX()+vectorT[0], point[0].getY()+vectorT[1], point[0].getZ()+vectorT[2], 0, 0, 0);
      Point v = new Point(vectorT[0], vectorT[1], vectorT[2], 0, 0, 0);
      v = v.unitVector(safety);
      side0 = side0.add(v);
      Point pointF = side0;   
      Point curPos = arm.getPalmPosition(moveInfo.lastLinkName);   
      double d = Math.pow((side0.getX() - curPos.getX()),2) + Math.pow((side0.getY() - curPos.getY()),2) + Math.pow((side0.getZ() - curPos.getZ()),2);
      double currentx = side0.getX();
      for (int i = 0; i < 360; i+=10) {   
        double L = vectori[0]*vectori[0] + vectori[1]*vectori[1] + vectori[2]*vectori[2];   
        double x = ((moveInfo.targetItem.getOrigin().getX()*(Math.pow(vectori[1],2)+Math.pow(vectori[2], 2)) - vectori[0] * (moveInfo.targetItem.getOrigin().getY()*vectori[1] + moveInfo.targetItem.getOrigin().getZ()*vectori[2] - vectori[0]*side0.getX() - vectori[1]*side0.getY() - vectori[2]*side0.getZ())) * (1 - Math.cos(MathUtils.degToRad(i))) + L * side0.getX() * Math.cos(MathUtils.degToRad(i)) + Math.sqrt(L) * (-moveInfo.targetItem.getOrigin().getZ()*vectori[1] + moveInfo.targetItem.getOrigin().getY()*vectori[2] - vectori[2]*side0.getY() + vectori[1]*side0.getZ()) * Math.sin(MathUtils.degToRad(i))) / L;   
        double y = ((moveInfo.targetItem.getOrigin().getY()*(Math.pow(vectori[0],2)+Math.pow(vectori[2], 2)) - vectori[1] * (moveInfo.targetItem.getOrigin().getX()*vectori[0] + moveInfo.targetItem.getOrigin().getZ()*vectori[2] - vectori[0]*side0.getX() - vectori[1]*side0.getY() - vectori[2]*side0.getZ())) * (1 - Math.cos(MathUtils.degToRad(i))) + L * side0.getY() * Math.cos(MathUtils.degToRad(i)) + Math.sqrt(L) * ( moveInfo.targetItem.getOrigin().getZ()*vectori[0] - moveInfo.targetItem.getOrigin().getX()*vectori[2] + vectori[2]*side0.getX() - vectori[0]*side0.getZ()) * Math.sin(MathUtils.degToRad(i))) / L;   
        double z = ((moveInfo.targetItem.getOrigin().getZ()*(Math.pow(vectori[0],2)+Math.pow(vectori[1], 2)) - vectori[2] * (moveInfo.targetItem.getOrigin().getX()*vectori[0] + moveInfo.targetItem.getOrigin().getY()*vectori[1] - vectori[0]*side0.getX() - vectori[1]*side0.getY() - vectori[2]*side0.getZ())) * (1 - Math.cos(MathUtils.degToRad(i))) + L * side0.getZ() * Math.cos(MathUtils.degToRad(i)) + Math.sqrt(L) * (-moveInfo.targetItem.getOrigin().getY()*vectori[0] + moveInfo.targetItem.getOrigin().getX()*vectori[1] - vectori[1]*side0.getX() + vectori[0]*side0.getY()) * Math.sin(MathUtils.degToRad(i))) / L;   
        Point check = new Point(x,y,z,0,0,0);   
        double dt = Math.pow((check.getX() - curPos.getX()),2) + Math.pow((check.getY() - curPos.getY()),2) + Math.pow((check.getZ() - curPos.getZ()),2);
        if (moveInfo.objectLocation.equals(ObjectPointLocation.RIGHT_SIDE)) {
          if (check.getX() < currentx){
            pointF = check;
            currentx = check.getX();
          }
        }
        else if (moveInfo.objectLocation.equals(ObjectPointLocation.LEFT_SIDE)) {
          if (check.getX() > currentx){
            pointF = check;
            currentx = check.getX();
          }
        }
        else if (dt < d) {   
          pointF = check;   
          d = dt;   
        }   
      }   
      point[0] = pointF;    
    }
    
    Point moveToPoint = point[0];    
    Log.info("Moving to point ", moveToPoint);    
    return moveToPoint;   
  }
  
  public void setAi(IntegratedMovement.Ai ai) {
    this.Ai |= ai.value;
  }

  public void removeAi(IntegratedMovement.Ai ai) {
    if((Ai & ai.value) > 0 ) {
      Ai -= ai.value;
    }
  }

}
