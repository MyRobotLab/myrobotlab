package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.myrobotlab.kinematics.TestJmeIMModel;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.openni.OpenNiData;
import org.myrobotlab.service.Servo.IKData;
import org.myrobotlab.service.interfaces.IKJointAnglePublisher;
import org.slf4j.Logger;

import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;

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
  public final static Logger log = LoggerFactory.getLogger(IntegratedMovement2.class.getCanonicalName());

  //private HashMap<String, DHRobotArm> arms = new HashMap<String, DHRobotArm>();
  private HashMap<String, IMEngine> engines = new HashMap<String, IMEngine>();

  private Matrix inputMatrix = null;

  private Point goTo;
  public transient CollisionDectection collisionItems = new CollisionDectection();
  
  
  GeneticParameters geneticParameters = new GeneticParameters();

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
      
      
  private OpenNi openni = null;
  
  private Map3D map3d = new Map3D();
  private String kinectName = "kinect";
  private boolean ProcessKinectData = false;
  
  private TestJmeIMModel jmeApp = null;
  
  /**
   * @return the jmeApp
   */
  public TestJmeIMModel getJmeApp() {
    return jmeApp;
  }

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
    jmeApp.addPoint(point);
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
    return engines.get(arm).createJointPositionMap();
    //return createJointPositionMap(getArm(arm));
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
    Runtime.createAndStart("gui", "SwingGui");
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
    Servo Romoplate = (Servo) Runtime.start("Romoplate","Servo");
    Romoplate.attach(arduino,31,10);
    Romoplate.map(10,70,10,70);
    Romoplate.setVelocity(15);
    //#omoplate.setMinMax(10,70)
    Romoplate.moveTo(10);
    Servo shoulder = (Servo) Runtime.start("shoulder","Servo");
    shoulder.attach(arduino,26,30);
    shoulder.map(0,180,0,180);
    //#shoulder.setMinMax(0,180)
    shoulder.setVelocity(14);
    shoulder.moveTo(30);
    Servo Rshoulder = (Servo) Runtime.start("Rshoulder","Servo");
    Rshoulder.attach(arduino,6,30);
    Rshoulder.map(0,180,0,180);
    //#shoulder.setMinMax(0,180)
    Rshoulder.setVelocity(14);
    Rshoulder.moveTo(30);
    Servo rotate = (Servo) Runtime.start("rotate","Servo");
    rotate.attach(arduino,9,90);
    rotate.map(46,160,46,160);
    //#rotate.setMinMax(46,180)
    rotate.setVelocity(18);
    rotate.moveTo(90);
    Servo Rrotate = (Servo) Runtime.start("Rrotate","Servo");
    Rrotate.attach(arduino,29,90);
    Rrotate.map(46,160,46,160);
    //#rotate.setMinMax(46,180)
    Rrotate.setVelocity(18);
    Rrotate.moveTo(90);
    Servo bicep = (Servo) Runtime.start("bicep","Servo");
    bicep.attach(arduino,8,10);
    bicep.map(5,60,5,80);
    bicep.setVelocity(26);
    //#bicep.setMinMax(5,90)
    bicep.moveTo(10);
    Servo Rbicep = (Servo) Runtime.start("Rbicep","Servo");
    Rbicep.attach(arduino,28,10);
    Rbicep.map(5,60,5,80);
    Rbicep.setVelocity(26);
    //#bicep.setMinMax(5,90)
    Rbicep.moveTo(10);
    Servo wrist = (Servo) Runtime.start("wrist","Servo");
    wrist.attach(arduino,7,90);
    //#wrist.map(45,135,45,135)
    wrist.map(0,180,0,180);
    wrist.setVelocity(26);
    //#bicep.setMinMax(5,90)
    wrist.moveTo(90);
    Servo Rwrist = (Servo) Runtime.start("Rwrist","Servo");
    Rwrist.attach(arduino,27,90);
    //#wrist.map(45,135,45,135)
    wrist.map(0,180,0,180);
    Rwrist.setVelocity(26);
    //#bicep.setMinMax(5,90)
    Rwrist.moveTo(90);
    Servo finger = (Servo) Runtime.start("finger","Servo");
    finger.attach(arduino,18,90);
    finger.map(89.999,90.001,89.999,90.001);
    finger.setVelocity(26);
    //#bicep.setMinMax(5,90)
    finger.moveTo(90);
    Servo Rfinger = (Servo) Runtime.start("Rfinger","Servo");
    Rfinger.attach(arduino,38,90);
    Rfinger.map(89.999,90.001,89.999,90.001);
    Rfinger.setVelocity(26);
    //#bicep.setMinMax(5,90)
    Rfinger.moveTo(90);

    //#define the DH parameters for the ik service
    ik.setNewDHRobotArm("leftArm");
    ik.setDHLink("leftArm",mtorso,113,90,0,-90);
    //ik.setDHLink("rightArm",ttorso,0,90+65.6,346,0);
    ik.setDHLink("leftArm",ttorso,0,180,292,90);
    ik.setDHLink("leftArm", "rightS", 143, 180, 0, 90);
    ik.setDHLink("leftArm",omoplate,0,-5.6,45,-90);
    ik.setDHLink("leftArm",shoulder,77,-30+90,0,90);
    ik.setDHLink("leftArm",rotate,284,90,40,90);
    ik.setDHLink("leftArm",bicep,0,-7+24.4+90,300,90);
////////////    //#ik.setDHLink(wrist,00,-90,200,0)
    ik.setDHLink("leftArm",wrist,00,-90,100,-90);
//////////    //print ik.currentPosition();
//////////
    ik.setDHLink("leftArm",finger,-20,-90,120,0);

    ik.startEngine("leftArm");
    
    ik.setNewDHRobotArm("rightArm");
    ik.setDHLink("rightArm",mtorso,113,90,0,-90);
    ik.setDHLink("rightArm",ttorso,0,180,292,90);
    //ik.setDHLink("leftArm",ttorso,0,180,297.5,90);
    ik.setDHLink("rightArm", "leftS", -143, 180, 0, -90);
    ik.setDHLink("rightArm",Romoplate,0,-5.6,45,90);
    ik.setDHLink("rightArm",Rshoulder,-77,-30+90,0,-90);
    ik.setDHLink("rightArm",Rrotate,-284,90,40,-90);
    ik.setDHLink("rightArm",Rbicep,0,-7+24.4+90,300,90);
////////////    //#ik.setDHLink(wrist,00,-90,200,0)
    ik.setDHLink("rightArm",Rwrist,00,-90,100,-90);
//////////    //print ik.currentPosition();
//////////
    ik.setDHLink("rightArm",Rfinger,20,-90,120,0);
    ik.startEngine("rightArm");
    
    ik.setNewDHRobotArm("kinect");
    ik.setDHLink("kinect",mtorso,113,90,0,-90);
    ik.setDHLink("kinect",ttorso,0,90+90,110,-90);
    ik.setDHLink("kinect","camera",0,90,10,90);
//
    ik.startEngine("kinect");
    
    //#define object, each dh link are set as an object, but the
    //#start point and end point will be update by the ik service, but still need
    //#a name and a radius
    //#static object need a start point, an end point, a name and a radius 
    ik.clearObject();
    ik.addObject(0.0, 0.0, 0.0, 0.0, 0.0, -150.0, "base", 150.0, false);
    ik.addObject("mtorso", 150.0);
    ik.addObject("ttorso", 10.0);
    ik.addObject("omoplate", 10.0);
    ik.addObject("Romoplate", 10.0);
    ik.addObject("shoulder", 50.0);
    ik.addObject("Rshoulder", 50.0);
    ik.addObject("rotate", 50.0);
    ik.addObject("Rrotate", 50.0);
    ik.addObject("bicep", 60.0);
    ik.addObject("Rbicep", 60.0);
    ik.addObject("wrist", 70.0);
    ik.addObject("Rwrist", 70.0);
    ik.addObject("leftS", 10);
    ik.addObject("rightS", 10);
    ik.objectAddIgnore("leftS", "rightS");
    ik.objectAddIgnore("omoplate", "rotate");
    ik.objectAddIgnore("Romoplate", "Rrotate");
    ik.objectAddIgnore("rightS", "shoulder");
    ik.objectAddIgnore("leftS", "Rshoulder");
    ik.addObject("finger",10.0);
    //ik.addObject("Rfinger",10.0);
    //ik.addObject(-1000.0,400, 0, 1000, 425, 00, "obstacle",40, true);
    //#ik.addObject(360,540,117,360, 550,107,"cymbal",200)
    //#ik.addObject(90,530,-180,300,545,-181,"bell", 25)
    //ik.addObject(170,640,-70,170,720,-250,"tom",150,true);


    //print ik.currentPosition();



    //#setting ik parameters for the computing

    //#move to a position
    //ik.moveTo("leftArm",260,410,-120);
    //ik.moveTo(280,190,-345);
    //#ik.moveTo("cymbal",ik.ObjectPointLocation.ORIGIN_SIDE, 0,0,5)
    //#mtorso.moveTo(45)
    log.info(ik.currentPosition("leftArm").toString());
    log.info(ik.currentPosition("rightArm").toString());
//    shoulder.moveTo(90);
//    sleep(1000);
//    log.info(ik.currentPosition("leftArm").toString());

    //print "kinect Position" + str(ik.currentPosition("kinect"));

    
    
    //ik.holdTarget("leftArm", true);
    ik.visualize();
    ik.jmeApp.addPart("ltorso", "Models/ltorso.j3o", 1, null, new Vector3f(0,0,0), Vector3f.UNIT_X.mult(1), (float)Math.toRadians(0));
    ik.jmeApp.addPart("mtorso", "Models/mtorso.j3o", 1f, null, new Vector3f(0,0,0), Vector3f.UNIT_Y.mult(-1), (float)Math.toRadians(-90));
    ik.jmeApp.addPart("ttorso", "Models/ttorso1.j3o", 1f, "mtorso", new Vector3f(0,105f,10), Vector3f.UNIT_Z, (float)Math.toRadians(-90));
    ik.jmeApp.addPart("rightS", null, 1f, "ttorso", new Vector3f(0,300f,0), Vector3f.UNIT_Z, (float)Math.toRadians(0));
    ik.jmeApp.addPart("Romoplate", "Models/Romoplate1.j3o", 1f, "rightS", new Vector3f(-143f,0,-17), Vector3f.UNIT_Z.mult(-1), (float)Math.toRadians(-4));
    ik.jmeApp.addPart("Rshoulder", "Models/Rshoulder1.j3o", 1f, "Romoplate", new Vector3f(-23,-45f,0), Vector3f.UNIT_X.mult(-1), (float)Math.toRadians(-32));
    ik.jmeApp.addPart("Rrotate", "Models/rotate1.j3o", 1f, "Rshoulder", new Vector3f(-57,-55,8), Vector3f.UNIT_Y.mult(-1), (float)Math.toRadians(-90));
    ik.jmeApp.addPart("Rbicep", "Models/Rbicep1.j3o", 1f, "Rrotate", new Vector3f(5,-225,-32), Vector3f.UNIT_X.mult(-1), (float)Math.toRadians(20));
    ik.jmeApp.addPart("leftS", null, 1f, "ttorso", new Vector3f(0,300f,0), Vector3f.UNIT_Z, (float)Math.toRadians(0));
    ik.jmeApp.addPart("omoplate", "Models/Lomoplate1.j3o", 1f, "leftS", new Vector3f(143f,0,-11), Vector3f.UNIT_Z.mult(1), (float)Math.toRadians(-6));
    ik.jmeApp.addPart("shoulder", "Models/Lshoulder.j3o", 1f, "omoplate", new Vector3f(17,-45f,5), Vector3f.UNIT_X.mult(-1), (float)Math.toRadians(-30));
    ik.jmeApp.addPart("rotate", "Models/rotate1.j3o", 1f, "shoulder", new Vector3f(65,-58,-3), Vector3f.UNIT_Y.mult(1), (float)Math.toRadians(-90));
    ik.jmeApp.addPart("bicep", "Models/Lbicep.j3o", 1f, "rotate", new Vector3f(-14,-223,-28), Vector3f.UNIT_X.mult(-1), (float)Math.toRadians(17));
    ik.jmeApp.addPart("Rwrist", "Models/RWristFinger.j3o", 1f, "Rbicep", new Vector3f(15,-290,-10), Vector3f.UNIT_Y.mult(-1), (float)Math.toRadians(180));
    ik.jmeApp.addPart("wrist", "Models/LWristFinger.j3o", 1f, "bicep", new Vector3f(0,-290,-20), Vector3f.UNIT_Y.mult(1), (float)Math.toRadians(180));
    ik.jmeApp.addPart("neck", "Models/neck.j3o", 1f, "ttorso", new Vector3f(0,452.5f,-45), Vector3f.UNIT_X.mult(-1), (float)Math.toRadians(0));
    ik.jmeApp.addPart("neckroll", null, 1f, "neck", new Vector3f(0,0,0), Vector3f.UNIT_Z.mult(1), (float)Math.toRadians(2));
    ik.jmeApp.addPart("head", "Models/head.j3o", 1f, "neckroll", new Vector3f(0,10,20), Vector3f.UNIT_Y.mult(-1), (float)Math.toRadians(0));
    ik.jmeApp.addPart("jaw", "Models/jaw.j3o", 1f, "head", new Vector3f(-5,63,-50), Vector3f.UNIT_X.mult(-1), (float)Math.toRadians(0));

    //TODO add the object that can collide with the model
    //ik.jmeApp.addObject();

    //need to move a little so the position update
    mtorso.moveTo(91);
    ttorso.moveTo(91);
    Romoplate.moveTo(11);
    Rshoulder.moveTo(31);
    Rrotate.moveTo(91);
    Rbicep.moveTo(6);
    omoplate.moveTo(11);
    shoulder.moveTo(31);
    rotate.moveTo(91);
    bicep.moveTo(6);
    wrist.moveTo(91);
    Rwrist.moveTo(91);

    mtorso.moveTo(90);
    ttorso.moveTo(90);
    Romoplate.moveTo(10);
    Rshoulder.moveTo(30);
    Rrotate.moveTo(90);
    Rbicep.moveTo(5);
    omoplate.moveTo(10);
    shoulder.moveTo(30);
    rotate.moveTo(90);
    bicep.moveTo(5);
    wrist.moveTo(90);
    Rwrist.moveTo(90);

    ik.startOpenNI();
    ik.processKinectData();
  }

  private void startEngine(String arm) {
    getEngine(arm).start();
    
  }

  private Thread getEngine(String arm) {
    if (engines.containsKey(arm)){
      return engines.get(arm);
    }
    else {
      log.info("no engines found {}", arm);
      return null;
    }
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

    ServiceType meta = new ServiceType(IntegratedMovement2.class.getCanonicalName());
    meta.addDescription("a 3D kinematics service supporting D-H parameters");
    meta.addCategory("robot", "control");
    meta.addPeer("openni", "OpenNi", "Kinect service");
    meta.addDependency("inmoov.fr", "1.0.0");
    meta.setAvailable(true);
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
  
  
  public String addObject(double oX, double oY, double oZ, double eX, double eY, double eZ, String name, double radius, boolean render) {
    return addObject(new Point(oX, oY, oZ, 0, 0, 0), new Point(eX, eY, eZ, 0, 0, 0), name, radius, render);
  }

  public String addObject(Point origin, Point end, String name, double radius) {
    return addObject(origin, end, name, radius, false);
  }
  
  public String addObject(Point origin, Point end, String name, double radius, boolean render) {
    CollisionItem item = new CollisionItem(origin, end, name, radius, render);
    collisionItems.addItem(item);
    if (jmeApp != null){
      jmeApp.addObject(item);
    }
    return item.getName();
  }

  public String addObject(String name, double radius, boolean render) {
    return addObject(new Point(0, 0, 0, 0, 0, 0), new Point(0, 0, 0, 0, 0, 0), name, radius, render);
  }
  
  public String addObject(String name, double radius) {
    return addObject(name, radius, false);
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
    }
    if (openni != null) {
      map3d.updateKinectPosition(currentPosition(kinectName));
    }
    if (jmeApp != null) {
      //jmeApp.updateObjects(collisionItems.getItems());
      jmeApp.updatePosition(data);
    }
  }
  
  public void moveTo(String armName, String objectName, ObjectPointLocation location) {
    engines.get(armName).moveTo(collisionItems.getItem(objectName), location);
  }   
      
  public void stopMoving() {
    for (IMEngine engine: engines.values()) {
      engine.target=null;
    }    
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
      if (jmeApp != null) {
        jmeApp.addObject(getCollisionObject());
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


  public ConcurrentHashMap<String, CollisionItem> getCollisionObject() {
    return collisionItems.getItems();
  }


  public ObjectPointLocation[] getEnumLocationValue() {
    return ObjectPointLocation.values();
  }
  
  public Collection<DHRobotArm> getArms() {
    return null;
    //return this.arms.values();
  }
  
  public void visualize() throws InterruptedException {
    jmeApp = new TestJmeIMModel();
    //jmeApp.setObjects(getCollisionObject());
    //jmeApp.setShowSettings(false);
    AppSettings settings = new AppSettings(true);
    settings.setResolution(800,600);
    //settings.setUseInput(false);
    jmeApp.setSettings(settings);
    jmeApp.setShowSettings(false);
    jmeApp.setPauseOnLostFocus(false);
    jmeApp.setService(this);
    jmeApp.start();
    //need to wait for jmeApp to be ready or the models won't load
    synchronized (this) {
      wait(5000);
    }
    //add the existing objects
    jmeApp.addObject(collisionItems.getItems());
  }

  public synchronized void sendAngles(String name, double positionValueDeg) {
    invoke("publishAngles", name, positionValueDeg);
  }
  
  public Object[] publishAngles(String name, double positionValueDeg) {
    Object[] retval = new Object[]{(Object)name, (Object)positionValueDeg};
    return retval;
  }
  
  public void holdTarget(String arm, boolean holdEnabled) {
    engines.get(arm).holdTarget(holdEnabled);
  }
}

