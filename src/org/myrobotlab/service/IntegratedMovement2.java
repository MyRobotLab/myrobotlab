package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.genetic.GeneticParameters;
import org.myrobotlab.kinematics.CollisionDectection;
import org.myrobotlab.kinematics.CollisionItem;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.kinematics.IMEngine;
import org.myrobotlab.kinematics.Map3D;
import org.myrobotlab.kinematics.Map3DPoint;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.kinematics.TestJmeIntegratedMovement;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.openni.OpenNiData;
import org.myrobotlab.service.Servo.IKData;
import org.myrobotlab.service.interfaces.IKJointAnglePublisher;
import org.slf4j.Logger;

/**
 * 
 * IntegratedMovement - This class provides a 3D based inverse kinematics
 * implementation that allows you to specify the robot arm geometry based on DH
 * Parameters. The work is based on InversedKinematics3D by kwatters with different computation and goal,
 * including collision detection and moveToObject
 * 
 * Rotation and Orientation information is not currently supported. (but should
 * be easy to add)
 *
 * @author Christian/Calamity
 * 
 */
public class IntegratedMovement2 extends Service implements IKJointAnglePublisher {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InverseKinematics3D.class.getCanonicalName());

  //private HashMap<String, DHRobotArm> arms = new HashMap<String, DHRobotArm>();
  private HashMap<String, IMEngine> engines = new HashMap<String, IMEngine>();

  private Matrix inputMatrix = null;

  private Point goTo;
  private CollisionDectection collisionItems = new CollisionDectection();
  
  
  GeneticParameters geneticParameters = new GeneticParameters();

  private double time;
  
  private boolean stopMoving = false;		
	
  public enum ObjectPointLocation {		
  	ORIGIN_CENTER (0x01,"Center Origin"),		
  	ORIGIN_SIDE (0x02, "Side Origin"),		
  	END_SIDE(0x04, "Side End"),		
  	END_CENTER(0x05, "Center End"),		
  	CLOSEST_POINT(0x06, "Closest Point"),		
  	CENTER(0x07, "Center"),		
  	CENTER_SIDE(0x08, "Side Center");		
  	public int value;		
  	public String location;
  	private ObjectPointLocation(int value, String location) {		
  		this.value = value;
  		this.location = location;
  	}		
  }		
  		
  private class MoveInfo {		
  	Point offset = null;		
  	CollisionItem targetItem = null;		
  	ObjectPointLocation objectLocation = null;		
  	DHLink lastLink = null;		
  }		
  		
  private MoveInfo moveInfo = null;
	private OpenNi openni = null;
	
	private Map3D map3d = new Map3D();
	private String kinectName = "kinect";
	private boolean ProcessKinectData = false;
	
	private TestJmeIntegratedMovement jmeApp = null;
  
  public IntegratedMovement2(String n) {
    super(n);
  }

  public Point currentPosition(String arm) {
    if (engines.containsKey(arm)) {
      return getArm(arm).getPalmPosition();
    }
    log.info("IK service have no data for {}", arm);
    return new Point(0, 0, 0, 0, 0, 0);
  }
  
  public void moveTo(String arm, double x, double y, double z) {
    moveTo(arm, new Point(x, y, z, 0, 0, 0));
  }

  public void moveTo(String arm, Point point) {
  	if (engines.containsKey(arm)) {
  		engines.get(arm).moveTo(point);
  	}
  	else {
  		log.info("unknow arm {}", arm);
  	}
	}

	/**
   * This create a rotation and translation matrix that will be applied on the
   * "moveTo" call.
   * 
   * @param dx
   *          - x axis translation
   * @param dy
   *          - y axis translation
   * @param dz
   *          - z axis translation
   * @param roll
   *          - rotation about z (in degrees)
   * @param pitch
   *          - rotation about x (in degrees)
   * @param yaw
   *          - rotation about y (in degrees)
   * @return
   */
  public Matrix createInputMatrix(double dx, double dy, double dz, double roll, double pitch, double yaw) {
    roll = MathUtils.degToRad(roll);
    pitch = MathUtils.degToRad(pitch);
    yaw = MathUtils.degToRad(yaw);
    Matrix trMatrix = Matrix.translation(dx, dy, dz);
    Matrix rotMatrix = Matrix.zRotation(roll).multiply(Matrix.yRotation(yaw).multiply(Matrix.xRotation(pitch)));
    inputMatrix = trMatrix.multiply(rotMatrix);
    for (IMEngine engine : engines.values()) {
    	engine.setInputMatrix(inputMatrix);
    }
    return inputMatrix;
  }


  public double[][] createJointPositionMap(String arm) {
    return createJointPositionMap(getArm(arm));
  }
  

  public double[][] createJointPositionMap(DHRobotArm arm) {

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

  public DHRobotArm getArm(String arm) {
    if (engines.containsKey(arm)) {
      return engines.get(arm).getDHRobotArm();
    }
    else {
    	log.error("Unknow DHRobotArm {}", arm);
    	DHRobotArm newArm = new DHRobotArm();
    	newArm.name = arm;
    	return newArm;
    }
  }

  public void addArm(String name, DHRobotArm currentArm) {
  	IMEngine newEngine = new IMEngine(name, currentArm, this);
    engines.put(name, newEngine);
  }

  public static void main(String[] args) throws Exception {
    LoggingFactory.init(Level.INFO);

    Runtime.createAndStart("python", "Python");
    Runtime.createAndStart("gui", "GUIService");
    IntegratedMovement2 ik = (IntegratedMovement2) Runtime.start("ik", "IntegratedMovement2");
    Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
    arduino.connect("COM22");
    arduino.setDebug(true);
    //define and attach servo
    //map is set so servo accept angle as input, output where
    //they need to go so that their part they where attach to
    //move by the input degree
    Servo mtorso = (Servo)Runtime.start("mtorso","Servo");
    mtorso.attach(arduino,26,90);
    mtorso.map(15,165,148,38);
    //#mtorso.setMinMax(35,150);
    mtorso.setVelocity(13);
    mtorso.moveTo(90);
    Servo ttorso = (Servo) Runtime.start("ttorso","Servo");
    ttorso.attach(arduino,7,90);
    ttorso.map(80,100,92,118);
    //ttorso.setInverted(False)
    //#ttorso.setMinMax(85,125)
    ttorso.setVelocity(13);
    ttorso.moveTo(90);
    Servo omoplate = (Servo) Runtime.start("omoplate","Servo");
    omoplate.attach(arduino,11,10);
    omoplate.map(10,70,10,70);
    omoplate.setVelocity(15);
    //#omoplate.setMinMax(10,70)
    omoplate.moveTo(10);
    Servo shoulder = (Servo) Runtime.start("shoulder","Servo");
    shoulder.attach(arduino,6,30);
    shoulder.map(0,180,0,180);
    //#shoulder.setMinMax(0,180)
    shoulder.setVelocity(14);
    shoulder.moveTo(20);
    Servo rotate = (Servo) Runtime.start("rotate","Servo");
    rotate.attach(arduino,9,90);
    rotate.map(46,160,46,160);
    //#rotate.setMinMax(46,180)
    rotate.setVelocity(18);
    rotate.moveTo(90);
    Servo bicep = (Servo) Runtime.start("bicep","Servo");
    bicep.attach(arduino,8,10);
    bicep.map(5,60,5,80);
    bicep.setVelocity(26);
    //#bicep.setMinMax(5,90)
    bicep.moveTo(10);
    Servo wrist = (Servo) Runtime.start("wrist","Servo");
    wrist.attach(arduino,7,90);
    //#wrist.map(45,135,45,135)
    wrist.map(90,91,90,91);
    wrist.setVelocity(26);
    //#bicep.setMinMax(5,90)
    wrist.moveTo(90);
    Servo finger = (Servo) Runtime.start("finger","Servo");
    finger.attach(arduino,8,90);
    finger.map(90,91,90,91);
    finger.setVelocity(26);
    //#bicep.setMinMax(5,90)
    finger.moveTo(90);

    //#define the DH parameters for the ik service
    ik.setNewDHRobotArm("leftArm");
    ik.setDHLink("leftArm",mtorso,113,90,0,-90);
    ik.setDHLink("leftArm",ttorso,0,90+65.6,346,0);
    ik.setDHLink("leftArm",omoplate,0,-5.6+24.4+180,55,-90);
    ik.setDHLink("leftArm",shoulder,77,-20+90,0,90);
    ik.setDHLink("leftArm",rotate,284,90,40,90);
    ik.setDHLink("leftArm",bicep,0,-7+24.4+90,300,90);
    //#ik.setDHLink(wrist,00,-90,200,0)
    ik.setDHLink("leftArm",wrist,00,-90,100,-90);
    //print ik.currentPosition();

    ik.setDHLink("leftArm",finger,00,00,300,0);

    ik.setNewDHRobotArm("kinect");
    ik.setDHLink("kinect",mtorso,113,90,0,-90);
    ik.setDHLink("kinect",ttorso,0,90+90,110,-90);
    ik.setDHLink("kinect","camera",0,90,10,90);

    //#define object, each dh link are set as an object, but the
    //#start point and end point will be update by the ik service, but still need
    //#a name and a radius
    //#static object need a start point, an end point, a name and a radius 
    ik.clearObject();
    ik.addObject(0.0, 0.0, 0.0, 0.0, 0.0, -150.0, "base", 150.0);
    ik.addObject("mtorso", 150.0);
    ik.addObject("ttorso", 10.0);
    ik.addObject("omoplate", 10.0);
    ik.addObject("shoulder", 50.0);
    ik.addObject("rotate", 50.0);
    ik.addObject("bicep", 60.0);
    ik.addObject("wrist", 70.0);
    ik.addObject("finger",10.0);
    //#ik.addObject(-1000.0, 300, 0, 1000, 300, 00, "obstacle",40)
    //#ik.addObject(360,540,117,360, 550,107,"cymbal",200)
    //#ik.addObject(90,530,-180,300,545,-181,"bell", 25)
    //#ik.addObject(-170,640,-70,-170,720,-250,"tom",150)


    //print ik.currentPosition();



    //#setting ik parameters for the computing

    //#move to a position
    ik.moveTo("leftArm",260,410,-120);
    //ik.moveTo(280,190,-345);
    //#ik.moveTo("cymbal",ik.ObjectPointLocation.ORIGIN_SIDE, 0,0,5)
    //#mtorso.moveTo(45)
    log.info(ik.currentPosition("leftArm").toString());
//    shoulder.moveTo(90);
//    sleep(1000);
//    log.info(ik.currentPosition("leftArm").toString());

    //print "kinect Position" + str(ik.currentPosition("kinect"));

    //ik.startOpenNI();
    
    //ik.processKinectData();

  }

  @Override
  public Map<String, Double> publishJointAngles(HashMap<String, Double> angleMap) {
    return angleMap;
  }

  public double[][] publishJointPositions(double[][] jointPositionMap) {
    return jointPositionMap;
  }


  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(InverseKinematics3D.class.getCanonicalName());
    meta.addDescription("a 3D kinematics service supporting D-H parameters");
    meta.addCategory("robot", "control");
    meta.addPeer("openni", "OpenNi", "Kinect service");

    return meta;
  }

  public void setDHLink (String arm, String name, double d, double theta, double r, double alpha) {
  	if (engines.containsKey(arm)) {
	    DHLink dhLink = new DHLink(name, d, r, MathUtils.degToRad(theta), MathUtils.degToRad(alpha));
	    IMEngine engine = engines.get(arm);
	    DHRobotArm dhArm = engine.getDHRobotArm();
	    dhArm.addLink(dhLink);
	    engine.setDHRobotArm(dhArm);
	    engines.put(arm, engine);
  	}
  	else {
  		log.error("Unknow DH arm {}", arm);
  	}
  }
  
  public void setDHLink (String arm, Servo servo, double d, double theta, double r, double alpha) {
  	if (engines.containsKey(arm)) {
  		IMEngine engine = engines.get(arm);
  		DHLink dhLink = new DHLink(servo.getName(), d, r, MathUtils.degToRad(theta), MathUtils.degToRad(alpha));
  		servo.addIKServoEventListener(this);
  		dhLink.addPositionValue(servo.getPos());
  		dhLink.setMin(MathUtils.degToRad(theta + servo.getMinInput()));
  		dhLink.setMax(MathUtils.degToRad(theta + servo.getMaxInput()));
  		dhLink.setState(Servo.SERVO_EVENT_STOPPED);
  		dhLink.setVelocity(servo.getVelocity());
  		dhLink.setTargetPos(servo.targetPos);
  		dhLink.servoMin = servo.getMinInput();
  		dhLink.servoMax = servo.getMaxInput();
  		dhLink.hasServo = true;
  		DHRobotArm dhArm = engine.getDHRobotArm();
  		dhArm.addLink(dhLink);
  		engine.setDHRobotArm(dhArm);
  		engines.put(arm, engine);
  		servo.subscribe(getName(), "publishAngles", servo.getName(), "onIMAngles");
  	}
  	else {
  		log.error("Unknow DH arm {}", arm);
  	}
  }
  
  public void setNewDHRobotArm(String name) {
  	IMEngine engine = new IMEngine(name, this);
  	engine.setInputMatrix(inputMatrix);
  	engines.put(name, engine);
  	
  }
  
  
  public String addObject(double oX, double oY, double oZ, double eX, double eY, double eZ, String name, double radius) {
    return addObject(new Point(oX, oY, oZ, 0, 0, 0), new Point(eX, eY, eZ, 0, 0, 0), name, radius);
  }

  public String addObject(Point origin, Point end, String name, double radius) {
    CollisionItem item = new CollisionItem(origin, end, name, radius);
    collisionItems.addItem(item);
    return item.getName();
  }
  public String addObject(String name, double radius) {
    return addObject(new Point(0, 0, 0, 0, 0, 0), new Point(0, 0, 0, 0, 0, 0), name, radius);
  }
  
  public String addObject(HashMap<Integer[],Map3DPoint> cloudMap) {
  	CollisionItem item = new CollisionItem(cloudMap);
  	collisionItems.addItem(item);
  	return item.getName();
  }
  
  public void clearObject(){
    collisionItems.clearItem();
  }
  
  
  public void objectAddIgnore(String object1, String object2) {
    collisionItems.addIgnore(object1, object2);
  }
  
  public void onIKServoEvent(IKData data) {
  	for (IMEngine e : engines.values()) {
  		e.updateLinksPosition(data);
//	    for (DHLink l: e.getDHRobotArm().getLinks()) {
//	      if (l.getName().equals(data.name)){
//	        l.addPositionValue(data.pos.doubleValue());
//	        l.setState(data.state);
//	        l.setVelocity(data.velocity);
//	        l.setTargetPos(data.targetPos);
//	      }
//	    }
    }
    if (openni != null) {
    	map3d.updateKinectPosition(currentPosition(kinectName));
    }
    if (jmeApp != null) {
    	jmeApp.updateObjects(collisionItems.getItems());
    }
  }
  
  public void moveTo(String name, ObjectPointLocation location, int xoffset, int yoffset, int zoffset) {		
//  	stopMoving = false;
//  	moveInfo = new MoveInfo();		
//  	moveInfo.offset = new Point(xoffset, yoffset, zoffset, 0, 0, 0);		
//  	moveInfo.targetItem = collisionItems.getItem(name);		
//  	moveInfo.objectLocation = location;		
//  	if (moveInfo.targetItem == null){		
//  		log.info("no items named {} found",name);		
//  		moveInfo = null;		
//  		return;		
//  	}		
//  	moveTo(moveToObject());		
  }		
  		
  private Point moveToObject() {
		return goTo;		
//  	Point[] point = new Point[2];		
//  	moveInfo.lastLink = currentArm.getLink(currentArm.getNumLinks()-1);		
//  	CollisionItem lastLinkItem = collisionItems.getItem(moveInfo.lastLink.getName());		
//		Double[] vector = new Double[3];		
//		boolean addRadius=false;		
//  	switch (moveInfo.objectLocation) {		
//  		case ORIGIN_CENTER: {		
//  			point[0] = moveInfo.targetItem.getOrigin();		
//  			break;		
//  		}		
//  		case END_CENTER: {		
//  			point[0] = moveInfo.targetItem.getEnd();		
//  			break;		
//  		}		
//  		case CLOSEST_POINT: {		
//  			point = collisionItems.getClosestPoint(moveInfo.targetItem, lastLinkItem, new Double[2], vector);		
//  			addRadius = true;		
//  			break;		
//  		}		
//  		case ORIGIN_SIDE: {		
//  			point[0] = moveInfo.targetItem.getOrigin();		
//  			addRadius = true;		
//  			break;		
//  		}		
//  		case END_SIDE: {		
//  			point[0] = moveInfo.targetItem.getEnd();		
//  			addRadius = true;		
//  			break;		
//  		}		
//  		case CENTER_SIDE: {		
//  			point = collisionItems.getClosestPoint(moveInfo.targetItem, lastLinkItem, new Double[]{0.5, 0.5}, vector);		
//  			addRadius = true;		
//  		}		
//  		case CENTER: {		
//  			point = collisionItems.getClosestPoint(moveInfo.targetItem, lastLinkItem, new Double[]{0.5, 0.5}, vector);		
//  		}		
//  	}		
//  	if(addRadius) {		
//  		double[] vectori = moveInfo.targetItem.getVector();		
//  		double[] vectorT = moveInfo.targetItem.getVectorT();		
//  		Point side0 = new Point(point[0].getX()+vectorT[0], point[0].getY()+vectorT[1], point[0].getZ()+vectorT[2], 0, 0, 0);		
//  		Point pointF = side0;		
//  		Point curPos = currentPosition();		
//  		double d = Math.pow((side0.getX() - curPos.getX()),2) + Math.pow((side0.getY() - curPos.getY()),2) + Math.pow((side0.getZ() - curPos.getZ()),2);		
//  		for (int i = 0; i < 360; i+=10) {		
//  			double L = vectori[0]*vectori[0] + vectori[1]*vectori[1] + vectori[2]*vectori[2];		
//  			double x = ((moveInfo.targetItem.getOrigin().getX()*(Math.pow(vectori[1],2)+Math.pow(vectori[2], 2)) - vectori[0] * (moveInfo.targetItem.getOrigin().getY()*vectori[1] + moveInfo.targetItem.getOrigin().getZ()*vectori[2] - vectori[0]*side0.getX() - vectori[1]*side0.getY() - vectori[2]*side0.getZ())) * (1 - Math.cos(MathUtils.degToRad(i))) + L * side0.getX() * Math.cos(MathUtils.degToRad(i)) + Math.sqrt(L) * (-moveInfo.targetItem.getOrigin().getZ()*vectori[1] + moveInfo.targetItem.getOrigin().getY()*vectori[2] - vectori[2]*side0.getY() + vectori[1]*side0.getZ()) * Math.sin(MathUtils.degToRad(i))) / L;		
//  			double y = ((moveInfo.targetItem.getOrigin().getY()*(Math.pow(vectori[0],2)+Math.pow(vectori[2], 2)) - vectori[1] * (moveInfo.targetItem.getOrigin().getX()*vectori[0] + moveInfo.targetItem.getOrigin().getZ()*vectori[2] - vectori[0]*side0.getX() - vectori[1]*side0.getY() - vectori[2]*side0.getZ())) * (1 - Math.cos(MathUtils.degToRad(i))) + L * side0.getY() * Math.cos(MathUtils.degToRad(i)) + Math.sqrt(L) * ( moveInfo.targetItem.getOrigin().getZ()*vectori[0] - moveInfo.targetItem.getOrigin().getX()*vectori[2] + vectori[2]*side0.getX() - vectori[0]*side0.getZ()) * Math.sin(MathUtils.degToRad(i))) / L;		
//  			double z = ((moveInfo.targetItem.getOrigin().getZ()*(Math.pow(vectori[0],2)+Math.pow(vectori[1], 2)) - vectori[2] * (moveInfo.targetItem.getOrigin().getX()*vectori[0] + moveInfo.targetItem.getOrigin().getY()*vectori[1] - vectori[0]*side0.getX() - vectori[1]*side0.getY() - vectori[2]*side0.getZ())) * (1 - Math.cos(MathUtils.degToRad(i))) + L * side0.getZ() * Math.cos(MathUtils.degToRad(i)) + Math.sqrt(L) * (-moveInfo.targetItem.getOrigin().getY()*vectori[0] + moveInfo.targetItem.getOrigin().getX()*vectori[1] - vectori[1]*side0.getX() + vectori[0]*side0.getY()) * Math.sin(MathUtils.degToRad(i))) / L;		
//  			Point check = new Point(x,y,z,0,0,0);		
//    		double dt = Math.pow((check.getX() - curPos.getX()),2) + Math.pow((check.getY() - curPos.getY()),2) + Math.pow((check.getZ() - curPos.getZ()),2);		
//    		if (dt < d) {		
//    			pointF = check;		
//    			d = dt;		
//    		}		
//  		}		
//  		point[0] = pointF;		
//  	}		
//  	Point moveToPoint = point[0].add(moveInfo.offset);		
//  	log.info("Moving to point {}", moveToPoint);		
//  	return moveToPoint;		
  }		
  
	public void stopMoving() {		
  	stopMoving = true;		
  }		
  		
  public OpenNi startOpenNI() throws Exception {
    if (openni == null) {
      openni = (OpenNi) startPeer("openni");
      openni.start3DData();
      map3d.updateKinectPosition(currentPosition(kinectName));
      //this.subscribe(openni.getName(), "publishOpenNIData", this.getName(), "onOpenNiData");
    }
    return openni;
  }
  
  public void onOpenNiData(OpenNiData data){
		if (ProcessKinectData) {
			ProcessKinectData = false;
  		long a = System.currentTimeMillis();
  		log.info("start {}",a);
  		map3d.processDepthMap(data);
  		removeKinectObject();
  		ArrayList<HashMap<Integer[],Map3DPoint>> object = map3d.getObject();
  		for (int i = 0; i < object.size(); i++) {
  			addObject(object.get(i));
  		}
  		long b = System.currentTimeMillis();
  		log.info("end {} - {} - {}",b, b-a, this.inbox.size());
  		broadcastState();
		}
  }

  private void removeKinectObject() {
		collisionItems.removeKinectObject();
		
	}


	public void processKinectData(){
  	ProcessKinectData = true;
  	onOpenNiData(openni.get3DData());
  }
  
  public void setKinectName(String kinectName) {
  	this.kinectName = kinectName;
  }


	public HashMap<String, CollisionItem> getCollisionObject() {
		return collisionItems.getItems();
	}


	public ObjectPointLocation[] getEnumLocationValue() {
		return ObjectPointLocation.values();
	}
	
	public Collection<DHRobotArm> getArms() {
		return null;
		//return this.arms.values();
	}
	
	public void visualize() {
		jmeApp = new TestJmeIntegratedMovement();
		jmeApp.setObjects(getCollisionObject());
		jmeApp.start();

	}

	public synchronized void sendAngles(String name, double positionValueDeg) {
		invoke("publishAngles", name, positionValueDeg);
	}
	
	public Object[] publishAngles(String name, double positionValueDeg) {
		Object[] retval = new Object[]{(Object)name, (Object)positionValueDeg};
		return retval;
	}
}

