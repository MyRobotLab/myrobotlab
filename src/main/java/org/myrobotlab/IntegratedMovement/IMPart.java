/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import java.util.HashMap;
import java.util.HashSet;

import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHLinkType;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.interfaces.ServoData.ServoStatus;

/**
 * Contain Info about a part or object that can be used by IntegratedMovement
 * @author calamity
 *
 */
public class IMPart {

	
	private String name;
	transient private HashMap<ArmConfig,String> controls = new HashMap<ArmConfig, String>();
	transient private HashMap<ArmConfig,DHLink> DHLinks = new HashMap<ArmConfig, DHLink>();
	private Double radius = 0.01;
	private String modelPath;
	private float scale = 1;
	transient private Point initialTranslateRotate = new Point(0,0,0,0,0,0);
	transient private Matrix origin = IMUtil.getIdentityMatrix();
	transient private Matrix end = IMUtil.getIdentityMatrix().multiply(Matrix.translation(0.01, 0, 0));
	private boolean visible = true;
	private Matrix internTransform = IMUtil.getIdentityMatrix();
	private double theta=0;
	private double alpha=0;
	private double initialTheta=0;
	private double r=0;
	transient private HashSet<String> reverseControl = new HashSet<String>();
	private ServoStatus state = ServoStatus.SERVO_STOPPED;
	private double targetPos = 0;
	private ArmConfig currentArmConfig = ArmConfig.DEFAULT;

	public IMPart(String partName){
		name = partName;
	}

	

	public String getName() {
		return name;
	}



	public void setControl(ArmConfig armConfig, String control) {
		controls.put(armConfig, control);
	}



	public void setDHParameters(ArmConfig armConfig, double d, double theta, double r, double alpha) {
		setDHParameters(armConfig, d, theta, r, alpha, DHLinkType.REVOLUTE);
	}



	public String getControl(ArmConfig conf) {
		return controls.get(conf);
	}



	public HashMap<ArmConfig, String> getControls() {
		return controls;
	}



	public DHLink getDHLink(ArmConfig armConfig) {
		return DHLinks.get(armConfig);
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



	public double getRadius() {
		return radius;
	}
	
	public Point getOriginPoint(){
		return IMUtil.matrixToPoint(origin);
	}



	public Point getEndPoint() {
		return IMUtil.matrixToPoint(end);
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



	public void setDHParameters(ArmConfig armConfig, double d, double theta, double r, double alpha, DHLinkType dhLinkType) {
		DHLink link = new DHLink(name, d, r, MathUtils.degToRad(theta), MathUtils.degToRad(alpha), dhLinkType);
		DHLinks.put(armConfig, link);
	}



	/**
	 * @return the internTransform
	 */
	public Matrix getInternTransform() {
		return internTransform;
	}



	/**
	 * @param internTransform the internTransform to set
	 */
	public void setInternTransform(Matrix internTransform) {
		this.internTransform = internTransform;
	}



	public void setTheta(double theta) {
		this.theta = theta;
	}



	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}
	
	public double getTheta(){
		return theta;
	}
	
	public double getAlpha(){
		return alpha;
	}



	public void setInitialTheta(double rad) {
		initialTheta = rad;
	}

	public double getInitialTheta(){
		return initialTheta;
	}



	public double getR() {
		return r;
	}



	public void setR(double r) {
		this.r = r;
		
	}



	public Matrix transform(ArmConfig armConfig) {
		return DHLinks.get(armConfig).resolveMatrix();
	}



	public void update() {
	}



	public void setControl(ArmConfig armConfig, String controlName, boolean reverse) {
		setControl(armConfig, controlName);
		if (reverse){
			reverseControl.add(controlName);
		}
	}



	public double addPositionToLink(double d) {
		DHLink link = DHLinks.get(currentArmConfig);
		if (reverseControl.contains(getControl())) d = -d;
		link.addPositionValue(d);
		return link.getTheta();
	}



	public ServoStatus getState() {
		return state ;
	}



	public void setState(ServoStatus state) {
		this.state = state;
	}



	/**
	 * @return the targetPos
	 */
	public double getTargetPos() {
		return targetPos;
	}



	/**
	 * @param targetPos the targetPos to set
	 */
	public void setTargetPos(double targetPos) {
		this.targetPos = targetPos;
	}



	public void incrRotate(double d) {
		DHLink link = DHLinks.get(currentArmConfig);
		if (currentArmConfig == ArmConfig.REVERSE) d = -d;
		if (!reverseControl.contains(getControl(currentArmConfig))) d = -d;
		link.incrRotate(d);
	}



	/**
	 * @return the currentArmConfig
	 */
	public ArmConfig getCurrentArmConfig() {
		return currentArmConfig;
	}



	/**
	 * @param currentArmConfig the currentArmConfig to set
	 */
	public void setCurrentArmConfig(ArmConfig currentArmConfig) {
		this.currentArmConfig = currentArmConfig;
	}



	public DHLink getDHLink() {
		return DHLinks.get(currentArmConfig);
	}



	public String getControl() {
		return controls.get(currentArmConfig);
	}



	public boolean isReversedControled() {
		if (reverseControl.contains(getControl())) return true;
		return false;
	}
}
