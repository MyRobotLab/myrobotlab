package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.interfaces.IKJointAnglePublisher;
import org.slf4j.Logger;

/**
 * 
 * InverseKinematics3D - This class provides a 3D based inverse kinematics implementation
 * that allows you to specify the robot arm geometry based on DH Parameters.
 * This will use a pseudo-inverse jacobian gradient descent approach to 
 * move the end affector to the desired x,y,z postions in space with 
 * respect to the base frame.
 * 
 * Rotation and Orientation information is not currently supported. (but should be easy to add)
 *
 * @author kwatters
 * 
 */
public class InverseKinematics3D extends Service implements IKJointAnglePublisher {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(InverseKinematics3D.class.getCanonicalName());

	private DHRobotArm currentArm = null;
	
	
	
	public InverseKinematics3D(String n) {
		super(n);
		// TODO: init
	}

	public Point currentPosition() {
		return currentArm.getPalmPosition();		
	}
	
	public void moveTo(double x, double y, double z) {
		moveTo(new Point(x,y,z));
	}
	
	public void moveTo(Point p) {
		currentArm.moveToGoal(p);
		HashMap<String, Float> angleMap = new HashMap<String, Float>();
		for (DHLink l : currentArm.getLinks()) {
			String jointName = l.getName();
			double theta = l.getTheta();
			angleMap.put(jointName, (float)MathUtils.radToDeg(theta));
		}
		invoke("publishJointAngles", angleMap);
		
		// we want to publish the joint positions 
		// this way we can render on the web gui..
		double[][] jointPositionMap = createJointPositionMap();
		// TODO: pass a better datastructure?
		invoke("publishJointPositions", (Object)jointPositionMap);
	}

	public double[][] createJointPositionMap() {
		
		double[][] jointPositionMap = new double[currentArm.getNumLinks()+1][3];
		
		// first position is the origin...  second is the end of the first link
		jointPositionMap[0][0] = 0;
		jointPositionMap[0][1] = 0;
		jointPositionMap[0][2] = 0;
		
		for (int i = 1 ; i <= currentArm.getNumLinks() ; i++) {
			Point jp = currentArm.getJointPosition(i-1);
			jointPositionMap[i][0] = jp.getX();
			jointPositionMap[i][1] = jp.getY();
			jointPositionMap[i][2] = jp.getZ();
		}
		return jointPositionMap;
	}
	
	@Override
	public String[] getCategories() {
		return new String[] { "robot", "control" };
	}

	@Override
	public String getDescription() {
		return "a 3D kinematics service supporting D-H parameters";
	}

	public DHRobotArm getCurrentArm() {
		return currentArm;
	}

	public void setCurrentArm(DHRobotArm currentArm) {
		this.currentArm = currentArm;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		InverseKinematics3D inversekinematics = new InverseKinematics3D("iksvc");

		// Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 */
	}

	@Override
	public Map<String, Float> publishJointAngles(Map<String, Float> angleMap) {
		// TODO Auto-generated method stub
		return angleMap;
	}

	public double[][] publishJointPositions(double[][] jointPositionMap) {
		return jointPositionMap;
	}
	
}
