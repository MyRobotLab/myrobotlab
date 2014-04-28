package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;
import org.myrobotlab.inverseKinematics.*;

public class InverseKinematics extends Service {

	protected IKEngine ikEngine;
	
	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(InverseKinematics.class.getCanonicalName());
	
	public InverseKinematics(String n) {
		super(n);	
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}
	
	 public void setDOF(int dof){
		    ikEngine= new IKEngine(dof);
		    ikEngine.setMode(dof);
	 }
	 
	 public void setStructure(int nlink, double length){
		    ikEngine.setLinkLength(nlink,length);
	 }
	 
	 public void compute(){
		    ikEngine.calculate();
		    ikEngine.getBaseAngle();
		    ikEngine.getArmAngles();
		    
	 }
	 public double getBaseAngle(){
		 double b = ikEngine.getBaseAngle();
		 return b;
	 }
	 
	 public double getArmAngles(int i){
		 double[] b = ikEngine.getArmAngles();
		 return b[i];
	 }
	 
	 public void setPoint(double x,double y, double z){
		    ikEngine.setGoal(x,y,z);
	 }

	 public void setPoint(int x,int y, int z){
		    ikEngine.setGoal(x,y,z);
	 }
	 
	 public void setPoint(float x, float y, float z){
		    ikEngine.setGoal(x,y,z);
	 }
	 
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		InverseKinematics inversekinematics = new InverseKinematics("inversekinematics");
		inversekinematics.setDOF(3);
		inversekinematics.setStructure(0,100);
		inversekinematics.setStructure(1,100);
		inversekinematics.setStructure(2,100);
		inversekinematics.setPoint(200,200,200);
		inversekinematics.compute();
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * 
		 */
	}


}
