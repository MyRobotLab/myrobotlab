/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import java.util.HashMap;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHLinkType;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.interfaces.ServoData.ServoStatus;

import com.jme3.math.Vector3f;

/**
 * Contain Info about a part or object that can be used by IntegratedMovement
 * @author calamity
 *
 */
public class IMPart {

	
	private String name;
	private HashMap<String,String> controls = new HashMap<String, String>();
	private HashMap<String,DHLink> DHLinks = new HashMap<String, DHLink>();
	private HashMap<String,String> nextLinks = new HashMap<String, String>();
	private Double radius = 0.1;
	private String modelPath;
	private float scale = 1;
	private Point initialTranslateRotate = new Point(0,0,0,0,0,0);
	private Matrix origin = Util.getIdentityMatrix();
	private Matrix end = Util.getIdentityMatrix();
	private boolean visible = true;

	public IMPart(String partName){
		name = partName;
	}

	

	public String getName() {
		return name;
	}



	public void setControl(String armModel, String control) {
		controls.put(armModel, control);
	}



	public void setDHParameters(String armModel, double d, double theta, double r, double alpha) {
		setDHParameters(armModel, d, theta, r, alpha, DHLinkType.REVOLUTE);
	}



	public String getControl(String armName) {
		return controls.get(armName);
	}



	public HashMap<String, String> getControls() {
		return controls;
	}



	public DHLink getDHLink(String armName) {
		return DHLinks.get(armName);
	}



	public String getNextLink(String armName) {
		return nextLinks.get(armName);
	}



	public void setRadius(Double radius) {
		this.radius = radius;
	}



	public void set3DModel(String modelPath, float scale, Point initialTranslateRotate) {
		this.modelPath = modelPath;
		this.scale = scale;		
		this.initialTranslateRotate = initialTranslateRotate;
	}



	public void setOrigin(Matrix m) {
		origin  = m;	
	}



	public void setEnd(Matrix m) {
		end  = m;
	}



	public String get3DModelPath() {
		return modelPath;
	}



	public float getScale() {
		return scale;
	}



	public Point getInitialTranslateRotate() {
		return initialTranslateRotate;
	}



	/**
	 * @return the origin
	 */
	public Matrix getOrigin() {
		return origin;
	}



	public Matrix getEnd() {
		return end;
	}



	public void setDHType(String arm, DHLinkType dhLinkType) {
		DHLinks.get(arm).setType(dhLinkType);
	}



	public void linkTo(String arm, String nextLink) {
		nextLinks.put(arm, nextLink);
	}



	public double getRadius() {
		return radius;
	}
	
	public Point getOriginPoint(){
		return Util.matrixToPoint(origin);
	}



	public Point getEndPoint() {
		return Util.matrixToPoint(end);
	}
	
	public double getLength() {
		Point origin = getOriginPoint();
		Point end = getEndPoint();
		return (Math.sqrt(Math.pow(origin.getX() - end.getX(), 2) + Math.pow(origin.getY() - end.getY(), 2) + Math.pow(origin.getZ() - end.getZ(), 2)));
	}



	public void setVisible(boolean b) {
		visible  = b;
	}



	public boolean isVisible() {
		return visible;
	}



	public void setDHParameters(String armName, double d, double theta, double r, double alpha, DHLinkType dhLinkType) {
		DHLink link = new DHLink(name, d, r, MathUtils.degToRad(theta), MathUtils.degToRad(alpha), dhLinkType);
		DHLinks.put(armModel, link);
	}

}
