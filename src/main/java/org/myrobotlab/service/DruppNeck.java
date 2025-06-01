package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.kinematics.DruppIKSolver;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.config.DruppNeckConfig;
import org.myrobotlab.service.interfaces.ServoControl;

/**
 * 
 * This is the drup neck service. It takes 3 servos, Up, Middle, and Down This
 * service uses the DruppIKSolver to compute the servo angles to move the head
 * to a specified roll, pitch and yaw orientation.
 * 
 * This is based on: https://github.com/parloma/penguin_wrist
 * 
 * @author kwatters
 *
 */
public class DruppNeck extends Service<DruppNeckConfig> {

  private static final long serialVersionUID = 1L;
  // 3 servos for the drupp neck
  protected transient ServoControl up;
  protected transient ServoControl middle;
  protected transient ServoControl down;

  public DruppNeck(String n, String id) {
    super(n, id);
  }

  public void startService() {
    super.startService();
    up = (ServoControl) startPeer("up");
    middle = (ServoControl) startPeer("middle");
    down = (ServoControl) startPeer("down");
  }

  private DruppIKSolver solver = new DruppIKSolver();

  /**
   * Specify the roll, pitch and yaw in degrees to move the drupp neck to. this
   * will use the drupp ik solver to compute the angle for the up, middle, and
   * down servos.
   * 
   * @param roll
   *          degrees
   * @param pitch
   *          degrees
   * @param yaw
   *          degrees
   * @throws Exception
   *           boom
   * 
   */
  public void moveTo(double roll, double pitch, double yaw) throws Exception {
    // convert to radians

    double rollRad = MathUtils.degToRad(roll);
    double pitchRad = MathUtils.degToRad(pitch);
    double yawRad = MathUtils.degToRad(yaw);
    // TODO: if the solver fails, should we catch this exception ?
    double[] result = solver.solve(rollRad, pitchRad, yawRad);
    // convert to degrees
    double upDeg = MathUtils.radToDeg(result[0]) + config.upOffset;
    double middleDeg = MathUtils.radToDeg(result[1]) + config.middleOffset;
    double downDeg = MathUtils.radToDeg(result[2]) + config.downOffset;
    // Ok, servos can only (typically) move from 0 to 180.. if any of the angles
    // are
    // negative... we can't move there.. let's log a warning
    // TODO: use the actual min/max .. and if we're out of range.. then log
    // this.
    // but for the drupp neck, if you've installed it correctly,
    // all servos can go from 0 to 180...
    if (upDeg < 0 || middleDeg < 0 || downDeg < 0 || upDeg > 180 || middleDeg > 180 || downDeg > 180) {
      log.warn("Target Position out of range! {} Pitch {} Yaw {} -> Up {} Middle {} Down {}", roll, pitch, yaw, MathUtils.round(upDeg, 3), MathUtils.round(middleDeg, 3),
          MathUtils.round(downDeg, 3));
      // Skipping this movement as it's likely unstable!
      return;
    }
    log.info("Input Roll {} Pitch {} Yaw {} -> Up {} Middle {} Down {}", roll, pitch, yaw, MathUtils.round(upDeg, 3), MathUtils.round(middleDeg, 3), MathUtils.round(downDeg, 3));
    // we should probably track the last moved to position.
    up.moveTo(upDeg);
    middle.moveTo(middleDeg);
    down.moveTo(downDeg);
    // TODO: broadcast state?
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

  // not really sure what sort of "attach" method we should have here.. this is
  // really the same as setServos
  // this assumes the servos are already attached to a servo controller
  public void attach(ServoControl up, ServoControl middle, ServoControl down) {
    this.up = up;
    this.middle = middle;
    this.down = down;
  }

  public void setServos(ServoControl up, ServoControl middle, ServoControl down) {
    attach(up, middle, down);
  }

  public double getUpOffset() {
    return config.upOffset;
  }

  public void setUpOffset(double upOffset) {
    this.config.upOffset = upOffset;
  }

  public double getMiddleOffset() {
    return config.middleOffset;
  }

  public void setMiddleOffset(double middleOffset) {
    this.config.middleOffset = middleOffset;
  }

  public double getDownOffset() {
    return config.downOffset;
  }

  public void setDownOffset(double downOffset) {
    this.config.downOffset = downOffset;
  }

  public static void main(String[] args) throws Exception {
    LoggingFactory.init("INFO");
    // To use the drup service you need to configure and attach the servos
    // then set them on the service.
    // Runtime.start("python", "Python");
    // Servo up = (Servo) Runtime.start("up", "Servo");
    // Servo middle = (Servo) Runtime.start("middle", "Servo");
    // Servo down = (Servo) Runtime.start("down", "Servo");
    // up.setPin(6);
    // middle.setPin(5);
    // down.setPin(4);
    // // String port = "COM4";
    // String port = "VIRTUAL_COM_PORT";
    // VirtualArduino va1 = (VirtualArduino) Runtime.start("va1",
    // "VirtualArduino");
    // va1.connect(port);
    // Arduino ard = (Arduino) Runtime.start("ard", "Arduino");
    // ard.connect(port);
    // ard.attach(up);
    // ard.attach(middle);
    // ard.attach(down);
    // Create the drupp service
    DruppNeck neck = (DruppNeck) Runtime.start("neck", "DruppNeck");
    Runtime.start("webgui", "WebGui");
    // neck.setServos(up, middle, down);
    // neck.moveTo(0, 0, 0);
    // neck.moveTo(0, 0, -45);
    // neck.moveTo(0, 0, 45);
  }

}
