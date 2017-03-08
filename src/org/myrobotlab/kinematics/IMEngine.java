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
import org.myrobotlab.service.IntegratedMovement2;
import org.myrobotlab.service.IntegratedMovement2.ObjectPointLocation;
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
  
  public class MoveInfo {    
    Point offset = null;    
    CollisionItem targetItem = null;    
    ObjectPointLocation objectLocation = null;    
    DHLink lastLink = null;
    public String arm;   
  }   

  MoveInfo moveInfo = null;
	
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
//	    if (target != null && arm.getPalmPosition().distanceTo(target) < maxDistance && !holdTargetEnabled) {
//	      target = null;
//	    }
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
				if (moveInfo != null) {
				  //target = moveToObject();
				}
				move();
				lastPosition = arm.getPalmPosition();
			}
			if (target != null && arm.getPalmPosition().distanceTo(target) < maxDistance) {
			  target = null;
			  moveInfo = null;
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
    double iterStep = 0.01;
    double errorThreshold = 0.5;
    int maxIterations = 10000;
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
        //if (numSteps >= maxIterations) return true;
        Log.info(computeArm.getPalmPosition().toString() + "genetic");
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
      		//target = null;
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
	  DHRobotArm checkArm = arm.clones();
	  double time = 0.0;
	  double timePerLoop = 0.1;
	  CollisionResults collisionResult = null;
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
	      cd.addItem(ci);
	    }
	    collisionResult = cd.runTest();
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

  public void moveTo(CollisionItem item, ObjectPointLocation location) {
    moveInfo = new MoveInfo();    
    moveInfo.targetItem = item;   
    moveInfo.objectLocation = location;   
    if (moveInfo.targetItem == null){   
      Log.info("no items named ", item.getName(), "found");   
      moveInfo = null;    
      return;   
    }   
    target = moveToObject();
    service.getJmeApp().addPoint(target);
  }

  private Point moveToObject() {
    double safety = 20.0;
    Point[] point = new Point[2]; 
    moveInfo.lastLink = arm.getLink(arm.getNumLinks()-1);   
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
      case CENTER_SIDE: {   
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
      Point curPos = arm.getPalmPosition();   
      double d = Math.pow((side0.getX() - curPos.getX()),2) + Math.pow((side0.getY() - curPos.getY()),2) + Math.pow((side0.getZ() - curPos.getZ()),2);    
      for (int i = 0; i < 360; i+=10) {   
        double L = vectori[0]*vectori[0] + vectori[1]*vectori[1] + vectori[2]*vectori[2];   
        double x = ((moveInfo.targetItem.getOrigin().getX()*(Math.pow(vectori[1],2)+Math.pow(vectori[2], 2)) - vectori[0] * (moveInfo.targetItem.getOrigin().getY()*vectori[1] + moveInfo.targetItem.getOrigin().getZ()*vectori[2] - vectori[0]*side0.getX() - vectori[1]*side0.getY() - vectori[2]*side0.getZ())) * (1 - Math.cos(MathUtils.degToRad(i))) + L * side0.getX() * Math.cos(MathUtils.degToRad(i)) + Math.sqrt(L) * (-moveInfo.targetItem.getOrigin().getZ()*vectori[1] + moveInfo.targetItem.getOrigin().getY()*vectori[2] - vectori[2]*side0.getY() + vectori[1]*side0.getZ()) * Math.sin(MathUtils.degToRad(i))) / L;   
        double y = ((moveInfo.targetItem.getOrigin().getY()*(Math.pow(vectori[0],2)+Math.pow(vectori[2], 2)) - vectori[1] * (moveInfo.targetItem.getOrigin().getX()*vectori[0] + moveInfo.targetItem.getOrigin().getZ()*vectori[2] - vectori[0]*side0.getX() - vectori[1]*side0.getY() - vectori[2]*side0.getZ())) * (1 - Math.cos(MathUtils.degToRad(i))) + L * side0.getY() * Math.cos(MathUtils.degToRad(i)) + Math.sqrt(L) * ( moveInfo.targetItem.getOrigin().getZ()*vectori[0] - moveInfo.targetItem.getOrigin().getX()*vectori[2] + vectori[2]*side0.getX() - vectori[0]*side0.getZ()) * Math.sin(MathUtils.degToRad(i))) / L;   
        double z = ((moveInfo.targetItem.getOrigin().getZ()*(Math.pow(vectori[0],2)+Math.pow(vectori[1], 2)) - vectori[2] * (moveInfo.targetItem.getOrigin().getX()*vectori[0] + moveInfo.targetItem.getOrigin().getY()*vectori[1] - vectori[0]*side0.getX() - vectori[1]*side0.getY() - vectori[2]*side0.getZ())) * (1 - Math.cos(MathUtils.degToRad(i))) + L * side0.getZ() * Math.cos(MathUtils.degToRad(i)) + Math.sqrt(L) * (-moveInfo.targetItem.getOrigin().getY()*vectori[0] + moveInfo.targetItem.getOrigin().getX()*vectori[1] - vectori[1]*side0.getX() + vectori[0]*side0.getY()) * Math.sin(MathUtils.degToRad(i))) / L;   
        Point check = new Point(x,y,z,0,0,0);   
        double dt = Math.pow((check.getX() - curPos.getX()),2) + Math.pow((check.getY() - curPos.getY()),2) + Math.pow((check.getZ() - curPos.getZ()),2);   
        if (dt < d) {   
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

}
