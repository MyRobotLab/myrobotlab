package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.kinematics.DruppIKSolver;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServoControl;

import marytts.util.math.MathUtils;

/**
 * 
 * This is the drup neck service.  It takes 3 servos, Up, Middle, and Down
 * This service uses the DruppIKSolver to compute the servo angles to move the head
 * to a specified roll, pitch and yaw orientation.
 * 
 * This is based on: https://github.com/parloma/penguin_wrist
 * 
 * @author kwatters
 *
 */
public class DruppNeck extends Service {

  // 3 servos for the drupp neck
  public transient ServoControl up;
  public transient ServoControl middle;
  public transient ServoControl down;

  // this is an offset angle that is added to the solution from the IK solver
  public double upOffset = 0;
  public double middleOffset = 0;
  public double downOffset = 0;
  
  public DruppNeck(String name) {
    super(name);
  }


  private DruppIKSolver solver = new DruppIKSolver();

  /** 
   * Specify the roll, pitch and yaw in degrees to move the drupp neck to.
   * this will use the drupp ik solver to compute the angle for the up, middle, and down servos.
   * 
   * @param roll
   * @param pitch
   * @param yaw
   * @throws Exception 
   */
  public void moveTo(double roll, double pitch, double yaw) throws Exception {

    double rollRad = MathUtils.degrees2radian(roll);
    double pitchRad = MathUtils.degrees2radian(pitch);
    double yawRad = MathUtils.degrees2radian(yaw);
    // convert to radians
    double[] result = solver.solve(rollRad, pitchRad, yawRad);
    // convert to degrees
    double upDeg = MathUtils.radian2degrees(result[0]) + upOffset;
    double middleDeg = MathUtils.radian2degrees(result[1]) + middleOffset;
    double downDeg = MathUtils.radian2degrees(result[2]) + downOffset;
    
    log.info("Input Roll {} Pitch {} Yaw {} -> Up {} Middle {} Down {}",roll,pitch,yaw,upDeg,middleDeg,downDeg);

    up.moveTo(upDeg);
    middle.moveTo(middleDeg);
    down.moveTo(downDeg);
    
  }

  /**
   * Enable the servos
   */
  public void enable() {
    up.enable();
    middle.enable();
    down.enable();
  }

  /**
   * Disable the servos
   */
  public void disable() {
    up.disable();
    middle.disable();
    down.disable();
  }
  
  public ServoControl getUp() {
    return up;
  }

  public void setUp(ServoControl up) {
    this.up = up;
  }

  public ServoControl getMiddle() {
    return middle;
  }

  public void setMiddle(ServoControl middle) {
    this.middle = middle;
  }

  public ServoControl getDown() {
    return down;
  }

  public void setDown(ServoControl down) {
    this.down = down;
  }

  public void setServos(ServoControl up, ServoControl middle, ServoControl down) {
    this.up = up;
    this.middle = middle;
    this.down = down;
  }
  
  public double getUpOffset() {
    return upOffset;
  }

  public void setUpOffset(double upOffset) {
    this.upOffset = upOffset;
  }

  public double getMiddleOffset() {
    return middleOffset;
  }

  public void setMiddleOffset(double middleOffset) {
    this.middleOffset = middleOffset;
  }

  public double getDownOffset() {
    return downOffset;
  }

  public void setDownOffset(double downOffset) {
    this.downOffset = downOffset;
  }

  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(InMoovHead.class.getCanonicalName());
    meta.addDescription("InMoov Drupp Neck Service");
    meta.addCategory("robot");

    meta.addPeer("up", "Servo", "Up servo");
    meta.addPeer("middle", "Servo", "Middle servo");
    meta.addPeer("down", "Servo", "Down servo");

    meta.setAvailable(true);
    
    return meta;
  }
  public static void main(String[] args) throws Exception {
    LoggingFactory.init("INFO");
    // To use the drup service you need to configure and attach the servos
    // then set them on the service.
    Servo up = (Servo)Runtime.start("up", "Servo");
    Servo middle = (Servo)Runtime.start("middle", "Servo");
    Servo down = (Servo)Runtime.start("down", "Servo");
    
    up.setPin(10);
    middle.setPin(11);
    down.setPin(12);
    
    VirtualArduino va1 = (VirtualArduino)Runtime.start("va1", "VirtualArduino");
    va1.connect("VIRTUAL_COM_PORT");
    
    Arduino ard = (Arduino)Runtime.start("ard", "Arduino");
    ard.connect("VIRTUAL_COM_PORT");
    
    
    ard.attach(up);
    ard.attach(middle);
    ard.attach(down);
    
    // Create the drupp service
    DruppNeck neck = (DruppNeck)Runtime.start("neck", "DruppNeck");
    
    neck.setServos(up, middle, down);
    
    neck.moveTo(0, 0, 0);
    
    neck.moveTo(0, 0, -45);
    
    neck.moveTo(0, 0, 45);

    
    
  }

}
