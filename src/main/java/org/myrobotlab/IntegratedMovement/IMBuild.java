/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.myrobotlab.genetic.Chromosome;
import org.myrobotlab.genetic.Genetic;
import org.myrobotlab.genetic.GeneticAlgorithm;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.IntegratedMovement;
import org.myrobotlab.service.interfaces.ServoData.ServoStatus;

/**
 * @author Calamity
 *
 */
public class IMBuild extends Thread implements Genetic {
	
	transient private IntegratedMovement service;
	transient Node<IMArm> arms = new Node<IMArm>(new IMArm("root"));
	protected Queue<IMMsg> msgQueue = new ConcurrentLinkedQueue<IMMsg>();
	private IMArm reversedArm = null;
	private HashMap<String, IMControl> controls;
	private double maxDistance = 0.005;
	private CalcFitnessType calcFitnessType = CalcFitnessType.POSITION;
	private LinkedList<IMPart> links;
	private ArmConfig currentArmConfig;
	private Point currentTarget;
	private Matrix currentOrigin;
	private long startUpdateTs;
	
	private enum CalcFitnessType {
		POSITION, COG;
	}
	  
	public IMBuild(String name, IntegratedMovement service, Matrix origin){
		super(name);
		this.service = service;
		arms.getData().setInputMatrix(origin);
	}

	public void addArm(IMArm arm){
		addArm(arm, null, ArmConfig.DEFAULT);
	}
	
	public void addArm(IMArm arm, ArmConfig armConfig){
		addArm(arm, null, armConfig);
	}
	
	public void addArm(IMArm arm, IMArm parent) {
		addArm(arm, parent, ArmConfig.DEFAULT);
	}
	
	public void addArm(IMArm arm, IMArm parent, ArmConfig armConfig){
		Node<IMArm> parentNode = arms.find(parent);
		if (parentNode == null) parentNode = arms;
		parentNode.addchild(new Node<IMArm>(arm));
	}

	public void run(){
		//service.error("test");
		while(true){
			startUpdateTs = System.currentTimeMillis();

			copyControl();
			updatePartsPosition();
			
			checkMsg();
			try {
				sleep(0);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			checkMove(arms);

			long deltaMs = System.currentTimeMillis() - startUpdateTs;
			long sleepMs = 33 - deltaMs;

			if (sleepMs < 0) {
				sleepMs = 0;
			}
			try {
				sleep(sleepMs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			
		}
	}
	
	
	private void checkMove(Node<IMArm> arm) {
		IMArm a = arm.getData();
		if (a.getTarget() != null && a.getTarget().distanceTo(a.getPosition(a.getLastPartToUse())) > maxDistance){
	        IntegratedMovement.log.info("distance to target {}", a.getPosition(a.getLastPartToUse()).distanceTo(a.getTarget()));
	        IntegratedMovement.log.info(a.getPosition(a.getLastPartToUse()).toString());
	        move(arm);
	        //cogRetry = 0;
		}
		else if (a.getTarget() != null){
			a.setTarget(null);
			a.setLastPartToUse(null);
		}
		for (Node<IMArm> child : arm.getChildren()){
			checkMove(child);
		}
	}

	private void move(Node<IMArm> arm) {
		IMArm a = arm.getData();
		a.increaseTryCount();
		if (a.getTryCount() > 500 && a.getPreviousTarget() != null){
			a.setTarget(a.getPreviousTarget());
		}
		moveToGoal(arm);
		publishAngles(arm);
	}

	private void publishAngles(Node<IMArm> arm) {
		Iterator<IMPart> it = links.iterator();
		while(it.hasNext()){
			IMPart part = it.next();
			if (part.getState() != ServoStatus.SERVO_POSITION_UPDATE) continue;
			String control = part.getControl(arm.getData().getArmConfig());
			DHLink link = part.getDHLink(arm.getData().getArmConfig());
			if (control == null || link == null) continue;
			service.sendAngles(control, link.getPositionValueDeg());
			part.setState(ServoStatus.SERVO_STOPPED);
		}
	}

//	private void publishAngles(Node<IMArm> arm) {
//		IMArm a = arm.getData();
//		LinkedList<IMPart> parts = a.getParts();
//		Iterator<IMPart> it = parts.iterator();
//		while (it.hasNext()){
//			IMPart part = it.next();
//			if (part.getState() != ServoStatus.SERVO_POSITION_UPDATE) continue;
//			String control = part.getControl(a.getArmConfig());
//			DHLink link = part.getDHLink(a.getArmConfig());
//			if (control == null || link == null) continue;
//			service.sendAngles(control, link.getPositionValueDeg());
//			part.setState(ServoStatus.SERVO_STOPPED);
//		}
//		for (Node<IMArm> child : arm.getChildren()){
//			publishAngles(child);
//		}
//		
//	}

	private boolean moveToGoal(Node<IMArm> arm) {
		int numSteps = 0;
	    double iterStep = 0.05;
	    double errorThreshold = 0.001;
	    int maxIterations = 5000;
	    int geneticPoolSize = 50;
	    double geneticRecombinationRate = 0.7;
	    double geneticMutationRate = 0.02;
	    int geneticGeneration = 10;
	    links = new LinkedList<IMPart>();
	    getParts(arm, links);
	    while (true){
	    	numSteps++;
	    	if (numSteps > maxIterations){
	    		break;
	    	}
	    	if (numSteps > maxIterations){
	            calcFitnessType = CalcFitnessType.POSITION;
	            currentArmConfig = arm.getData().getArmConfig();
	            currentTarget = arm.getData().getTarget();
	            currentOrigin = arm.getData().getInputMatrix();
	            GeneticAlgorithm GA = new GeneticAlgorithm(this, geneticPoolSize, links.size(), 12, geneticRecombinationRate, geneticMutationRate);
	            Chromosome bestFit = GA.doGeneration(geneticGeneration); // this is the
	            Iterator<IMPart> it = links.iterator();
	            int i = 0;
	            while (it.hasNext()){
	            	IMPart part = it.next();
	                if (bestFit.getDecodedGenome().get(i) != null) {
	                  DHLink link = links.get(i).getDHLink(arm.getData().getArmConfig());//part.getDHLink(arm.getData().getArmConfig());
	                  double degrees = link.getPositionValueDeg();
	                  double deltaDegree = java.lang.Math.abs(degrees - (double) bestFit.getDecodedGenome().get(i));
	                  if (degrees > ((double) bestFit.getDecodedGenome().get(i))) {
	                    degrees -= deltaDegree;
	                  } else if (degrees < ((double) bestFit.getDecodedGenome().get(i))) {
	                    degrees += deltaDegree;
	                  }
	                  link.addPositionValue(degrees);
	                  i++;
	                }
	                if (part.getName().equals(arm.getData().getLastPartToUse())){
	                  break;
	                }
	            }
	            return true;
	    	}
	    	Point currentPos = getPosition(links, arm.getData().getArmConfig());//arm.getData().getPosition(arms.getData().getLastPartToUse());
	        // vector to destination
	    	Point deltaPoint = arm.getData().getTarget().subtract(currentPos);
	        Matrix dP = new Matrix(3, 1);
	        dP.elements[0][0] = deltaPoint.getX();
	        dP.elements[1][0] = deltaPoint.getY();
	        dP.elements[2][0] = deltaPoint.getZ();
	        // scale a vector towards the goal by the increment step.
	        dP = dP.multiply(iterStep);
	        Matrix dTheta = null;
	        Matrix jInverse = getJInverse(arm, links);
	        dTheta = jInverse.multiply(dP);
	        if (dTheta == null) {
	            dTheta = new Matrix(links.size(), 1);
	            for (int i = 0; i < links.size(); i++) {
	            	dTheta.elements[i][0] = 0.000001;
	            }
	        }
	        for (int i = 0; i < dTheta.getNumRows(); i++) {
	            DHLink link = links.get(i).getDHLink(arm.getData().getArmConfig());
	            if (links.get(i).getControl(arm.getData().getArmConfig()) != null) {
	              // update joint positions! move towards the goal!
	              double d = dTheta.elements[i][0];
	              // incr rotate needs to be min/max aware here!
	              link.incrRotate(d);
	              links.get(i).setState(ServoStatus.SERVO_POSITION_UPDATE);
	            }
	            if (links.get(i).getName().equals(arm.getData().getLastPartToUse())) {
	              break;
	            }
	          }
	        // delta point represents the direction we need to move in order to
	        // get there.
	        // we should figure out how to scale the steps.

	        if (deltaPoint.magnitude() < errorThreshold) {
	        	int x=0;
	          break;
	        }
	    	long deltaMs = System.currentTimeMillis() - startUpdateTs;
	    	if (deltaMs > 30){
	    		break;
	    	}

	    }
		return true;
	}
	
	private Matrix getJInverse(Node<IMArm> arm, LinkedList<IMPart> parts){
		double delta = 0.0001;
	    // we need a jacobian matrix that is 6 x numLinks
	    // for now we'll only deal with x,y,z we can add rotation later. so only 3
	    // We can add rotation information into slots 4,5,6 when we add it to the
	    // algorithm.
	    Matrix jacobian = new Matrix(3, parts.size());
	    // compute the gradient of x,y,z based on the joint movement.
	    Point basePosition = arm.getData().getPosition(arm.getData().getLastPartToUse());
	    // for each servo, we'll rotate it forward by delta (and back), and get
	    // the new positions
	    Iterator<IMPart> it = parts.iterator();
	    int j = 0;
	    while (it.hasNext()){
	    	IMPart part = it.next();
	    	if (part.getControl(arm.getData().getArmConfig()) == null){
	    		//that link is not moving
	    		j++;
	    		continue;
	    	}
	    	DHLink link = part.getDHLink(arm.getData().getArmConfig());
	    	//TODO: fix link != part.getlink
	    	link.incrRotate(delta);
	    	Point curPos = getPosition(links, arm.getData().getArmConfig());//arm.getData().getPosition(arm.getData().getLastPartToUse());
	    	Point deltaPoint = curPos.subtract(basePosition);
	    	link.incrRotate(-delta);
	        // delta position / base position gives us the slope / rate of
	        // change
	        // this is an approx of the gradient of P
	        double dXdj = deltaPoint.getX() / delta;
	        double dYdj = deltaPoint.getY() / delta;
	        double dZdj = deltaPoint.getZ() / delta;
	        jacobian.elements[0][j] = dXdj;
	        jacobian.elements[1][j] = dYdj;
	        jacobian.elements[2][j] = dZdj;
	        // TODO: get orientation roll/pitch/yaw
	        j++;
	    }
	    // This is the MAGIC! the pseudo inverse should map
	    // deltaTheta[i] to delta[x,y,z]
	    Matrix jInverse = jacobian.pseudoInverse();
	    if (jInverse == null) {
	        jInverse = new Matrix(3, parts.size());
	      }
	      return jInverse;
	}

	private Point getPosition(LinkedList<IMPart> parts, ArmConfig armConfig) {
		// TODO Auto-generated method stub
		Matrix m = parts.getFirst().getOrigin();
		Iterator<IMPart> it = parts.iterator();
		while (it.hasNext()){
			IMPart part = it.next();
			DHLink link = part.getDHLink(armConfig);
			m = m.multiply(link.resolveMatrix());
		}
		return IMUtil.matrixToPoint(m);
	}

	private void getParts(Node<IMArm> arm, LinkedList<IMPart> links) {
		if (arm.getData().getArmConfig() != ArmConfig.REVERSE){
			if (arm.getParent() != null){
				getParts(arm.getParent(), links);
			}
			else return;
			links.addAll(arm.getData().getParts());
			return;
		}
		if (arm.getParent() != null){
			Iterator<IMPart> it = arm.getData().getParts().descendingIterator();
			while (it.hasNext()){
				links.add(it.next());
			}
			getParts(arm.getParent(), links);
		}
	}

	private void copyControl() {
		controls = new HashMap<String, IMControl>();
		for (IMControl c: service.getData().getControls().values()){
			controls.put(c.getName(), new IMControl(c));
		}
	}

	private void checkMsg() {
	    while (msgQueue.size() > 0) {
	        IMMsg msg = null;
	        try {
	          msg = msgQueue.remove();
	          invoke(msg);
	        } catch (Exception e) {
	          service.error("checkMsg failed for {} - targetName", msg, e);
	        }
	      }
	}

	public Object invoke(IMMsg msg) {
		return service.invokeOn(this, msg.method, msg.data);
	}

	public void reverseArm(String armName){
		IMArm arm = service.getData().getArm(armName);
		if (arm.getArmConfig() == ArmConfig.REVERSE){
			Node<IMArm> armNode = arms.find(arm);
			while (armNode.getParent() != null){
				arm.setArmConfig(ArmConfig.DEFAULT);
				armNode = armNode.getParent();
			}
			reversedArm  = null;
		}
		else {
			Node<IMArm> armNode = arms.find(arm);
			reversedArm = arm;
			while (armNode.getParent() != null){
				arm.setArmConfig(ArmConfig.REVERSE);
				Matrix im = arm.getLastPart().getEnd();
				((IMArm)(armNode.getParent().getData())).setInputMatrix(arm.getTransformMatrix(ArmConfig.REVERSE, im));
				armNode = armNode.getParent();
			}
		}
	}

	private void updatePartsPosition() {
		updateReverseArm();
		updatePartsPosition(arms);
	}
	
	private void updateReverseArm() {
		if (reversedArm != null){
			Node<IMArm> arm = arms.find(reversedArm);
			while (arm.getParent() != null) {
				Matrix im = arm.getData().getLastPart().getEnd();
				arm.getData().updatePosition(service.getData().getControls());
				((IMArm)(arm.getParent().getData())).setInputMatrix(arm.getData().getTransformMatrix(ArmConfig.REVERSE, im));
				arm = arm.getParent();
			}
		}
	}

	private void updatePartsPosition(Node<IMArm> armNode){
		Matrix m = armNode.getData().updatePosition(service.getData().getControls());
		for (Node<IMArm> arm : armNode.getChildren()){
			arm.getData().setInputMatrix(m);
			updatePartsPosition(arm);
		}
	}
	
	public Point currentPosition(String arm){
		Node<IMArm> node = arms.find(service.getData().getArm(arm));
		if (node == null){
			service.error("Couldn't find arm {}", arm);
			return new Point(0,0,0,0,0,0);
		}
		Matrix m = currentPosition(node);
		return IMUtil.matrixToPoint(m);
	}
	
	private Matrix currentPosition(Node<IMArm> arm){
		Node<IMArm> parent = arm.getParent();
		Matrix retVal = new Matrix(4,4).loadIdentity();
		if (parent != null) {
			retVal = currentPosition(parent);
		}
		retVal = retVal.multiply(arm.getData().getTransformMatrix());
		return retVal;
	}
	
	public void addMsg(String method, Object... params) {
		msgQueue.add(new IMMsg(method, params));
	}
	
	public void moveTo(String armName, String partName, Point point){
		IMArm arm = service.getArm(armName);
		arm.setTarget(point);
		arm.setLastPartToUse(partName);
		arm.setPreviousTarget(partName);
		arm.setTryCount(0);
	}

	@Override
	public void calcFitness(ArrayList<Chromosome> chromosomes) {
	    for (Chromosome chromosome : chromosomes) {
	        double fitnessMult = 1;
	        int i = 0;
	        Iterator<IMPart> it = links.iterator();
	        ArrayList<DHLink> newLinks = new ArrayList<DHLink>();
	        Matrix tm = currentOrigin;
	        while (it.hasNext()){
	        	IMPart part = it.next();
	        	DHLink link = new DHLink(part.getDHLink(currentArmConfig));
	        	if (chromosome.getDecodedGenome().get(i) != null){
	        		link.addPositionValue((double)chromosome.getDecodedGenome().get(i));
	        	}
	        	newLinks.add(link);
	        	tm = tm.multiply(link.resolveMatrix());
	        	i++;
	        }
	        Point potLocation = IMUtil.matrixToPoint(tm);
	        if (calcFitnessType == CalcFitnessType.POSITION){
	        	if (currentTarget == null) return;
	        	double distance = potLocation.distanceTo(currentTarget);
	        	double fitness = Math.abs(fitnessMult / distance * 1000);
	        	chromosome.setFitness(fitness);
	        }
	        //TODO: do calcFitnessType == COG
	    }
	    
	}

	@Override
	public void decode(ArrayList<Chromosome> chromosomes) {
		for (Chromosome chromosome : chromosomes){
			int pos = 0;
			ArrayList<Object> decodedGenome = new ArrayList<Object>();
			Iterator<IMPart> it = links.iterator();
			while (it.hasNext()){
				IMPart part = it.next();
				if (part.getControl(currentArmConfig) == null){
					decodedGenome.add(null);
					continue;
				}
				if (controls.get(part.getControl(currentArmConfig)).getState() != ServoStatus.SERVO_STOPPED){
					decodedGenome.add(controls.get(part.getControl(currentArmConfig)).getTargetPos());
					continue;
				}
				if (part.getState() != ServoStatus.SERVO_STOPPED){
					decodedGenome.add(part.getTargetPos());
					continue;
				}
				DHLink link = part.getDHLink(currentArmConfig);
				if (link.getMin() == link.getMax()){
					decodedGenome.add(link.getMin());
					continue;
				}
				Mapper map = new Mapper(0,8191, link.getMinDegree(), link.getMaxDegree());
		        Double value = 0.0;
		        for (int i = pos; i < chromosome.getGenome().length() && i < pos + 13; i++) {
		          if (chromosome.getGenome().charAt(i) == '1')
		            value += 1 << i - pos;
		        }
		        pos += 13;
		        value = map.calcOutput(value);
		        if (value.isNaN()) {
		          value = link.getPositionValueDeg();
		        }
		        decodedGenome.add(value);
			}
			chromosome.setDecodedGenome(decodedGenome);
		}
	}

	public IMArm getRoot() {
		return arms.getData();
	}
}
