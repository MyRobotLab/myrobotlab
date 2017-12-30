package org.myrobotlab.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.PortConnector;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 *
 * 
 * &lt;pre&gt;
 * 
 * # &lt;ch&gt; P &lt;pw&gt; ​S&lt;spd&gt;​​T&lt;time&gt; &lt;cr&gt;
 * 
 * &lt;ch&gt;: pin / channel to which the servo is connected (0 to 31) in
 * decimal &lt;pw&gt;: desired pulse width (normally 500 to 2500) in
 * microseconds &lt;spd&gt;: servo movement speed in microseconds per second*
 * &lt;time&gt;: time in microseconds to travel from the current position to the
 * desired position. This affects all servos (65535 max) * &lt;cr&gt;: carriage
 * return (ASCII 13)**
 * 
 * &lt;/pre&gt;
 *
 * @author GroG
 * 
 */
public class Ssc32UsbServoController extends Service implements PortConnector, ServoController {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Ssc32UsbServoController.class);

  transient Serial serial;

  transient HashMap<String, ServoControl> servos = new HashMap<String, ServoControl>();

  Integer defaultBaud = 9600;

  static public double toUs(double degrees) {
    // arduino docs
    // https://www.arduino.cc/en/Reference/ServoWriteMicroseconds
    // (degrees * 5.5) + 1000;
    // although arduino code does it differently
    //
    // http://www.robotshop.com/forum/converting-servo-pulses-to-degrees-t4856
    // (degrees * 10) + 590; // for (2390 - 590)

    // return (degrees * 5.5) + 1000;
    return (int) Math.round((degrees * (2400 - 544) / 180) + 544);
  };

  static public double toDegrees(double us) {
    return (us - 544) / 10;
  };

  public Ssc32UsbServoController(String n) {
    super(n);
  }

  /**
   * connect - default connection speed is 9600
   * 
   * Press and hold the button. At first the LEDs will glow to indicate the
   * current Baud rate. 9600 (green) 38400 (red) 115200 (both green and red)
   * Press the button to cycle through baud rates
   */
  public void connect(String port) throws IOException {
    connect(port, defaultBaud, 8, 1, 0);
  }

  /**
   * disconnect serial
   */
  public void disconnect() {
    if (serial != null && serial.isConnected()) {
      serial.disconnect();
    }
  }

  @Override
  public void startService() {
    super.startService();
    serial = (Serial) startPeer("serial");
    // serial.addByteListener(this); TODO - listen on input pins?
  }

  public SerialDevice getSerial() {
    return serial;
  }

  @Override
  public boolean isConnected() {
    if (serial == null || !serial.isConnected()) {
      return false;
    }
    return true;
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

    ServiceType meta = new ServiceType(Ssc32UsbServoController.class.getCanonicalName());
    meta.addDescription("Lynxmotion usb 32 servo controller");
    meta.addCategory("servo", "control");
    meta.addPeer("serial", "Serial", "Serial Port");

    return meta;
  }

  @Override
  public void connect(String port, int rate, int databits, int stopbits, int parity) throws IOException {
    serial.open(port, rate, databits, stopbits, parity);
  }

  @Override
  public void servoAttachPin(ServoControl servo, int pin) {
    // DUNNO HOW TO RE-ENABLE unless its just a write to current position
    servoMoveTo(servo);
  }

  public void write(String cmd, Object... params) {
    if (serial == null || !serial.isConnected()) {
      error("must be connected to serial port - connect(port)");
      return;
    }
    try {
      String c = String.format("%s\r", String.format(cmd, params));
      log.info(String.format("cmd [%s]", c));
      serial.write(c.getBytes());
    } catch (Exception e) {
      log.error("serial threw", e);
    }
  }

  @Override
  public void servoSweepStart(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoSweepStop(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoMoveTo(ServoControl servo) {
    // # <ch> P <pw> ​S​​<spd>​​T​<time> <cr>
    log.info(String.format("servoMove %f", servo.getTargetOutput()));
    StringBuilder sb = new StringBuilder();
    sb.append("#").append(servo.getPin());
    sb.append("P").append((int) toUs(servo.getTargetOutput()));

    // T is 'over-calculated' - it represents 'travel time' - the time from
    // "any" position to destination distance
    // covered in that specified time :P
    // Velocity on this controller is specified in "Time for Travel" ,
    // The total number of milliseconds from where it currently is to a new
    // position

    double velocity = servo.getVelocity();
    if (velocity > 0) {
      // sb.append("T").append(velocity * 10); // T is us per second
      sb.append("T").append(velocity * 100);
    }

    // String cmd = "#%dP%d";
    // write("#%dP%d", servo.getPin(),
    // (int)servo.toUs(servo.getTargetOutput()));
    write(sb.toString());
  }

  @Override
  public void servoWriteMicroseconds(ServoControl servo, int uS) {
    StringBuilder sb = new StringBuilder();
    sb.append("#").append(servo.getPin());
    sb.append("P").append(uS);
    double velocity = servo.getVelocity();
    if (velocity > 0) {
      // sb.append("T").append(velocity * 10); // T is us per second
      sb.append("T").append(velocity * 100);
    }
    write(sb.toString());
  }

  @Override
  public void servoDetachPin(ServoControl servo) {
    int pin = servo.getPin();
    write("STOP %d #%dP0 #%dL #%dH", pin, pin, pin, pin);
  }

  @Override
  public void servoSetVelocity(ServoControl servo) {
    // TODO Auto-generated method stub

  }

  @Override
  public void servoSetAcceleration(ServoControl servo) {
    // probably a noop - as it can be pulled from the
    // servo controller when the servo is moved.
  }
  
  
  /**
   * attach with parameters which will set all necessary attributes on ServoControl
   * before calling the single parameter typed attach
   * 
   * @param servo the servo
   * @param pin the pin number 
   * @throws Exception e
   */
  @Override
  public void attach(ServoControl servo, int pin) throws Exception {
    servo.setPin(pin);
    attachServoControl(servo);
  }

  /**
   * returns all currently attached services
   */
  @Override
  public Set<String> getAttached() {
    return servos.keySet();
  }

  /**
   * Routing attach - routes ServiceInterface.attach(service) to appropriate
   * methods for this class
   */
  @Override
  public void attach(Attachable service) throws Exception {
    if (ServoControl.class.isAssignableFrom(service.getClass())) {
      attachServoControl((ServoControl) service);
      return;
    }

    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
  }

  /**
   * Single parameter attach{type}({type}) - this is where the "real" attach
   * logic is for this service.
   */
  @Override
  public void attachServoControl(ServoControl servo) throws Exception {
    if (isAttached(servo)) {
      return;
    }
    servos.put(servo.getName(), servo);
    servo.attach(this);
  }

  /**
   * Routing detach - routes ServiceInterface.detach(service) to appropriate
   * methods for this class
   */
  public void detach(Attachable service){
    if (ServoControl.class.isAssignableFrom(service.getClass())) {
      detachServoControl((ServoControl) service);
      return;
    }
    error("%s doesn't know how to detach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
  }

  /**
   * This is a typed detach for the one interface it knows how to attach to
   * 
   * @param servo - servo control service
   */
  public void detachServoControl(ServoControl servo) {
    if (isAttached(servo.getName())) {
      servos.remove(servo.getName());
      servo.detach(this);
    }
  }

  public boolean isAttached(String name) {
    return servos.containsKey(name);
  }

  public static void main(String[] args) {
    LoggingFactory.init("INFO");

    try {

      String port = "COM12";

      // ---- Virtual Begin -----
      // VirtualDevice virtual = (VirtualDevice) Runtime.start("virtual",
      // "VirtualDevice");
      // virtual.createVirtualSerial(port);
      // virtual.getUART(); uart.setTimeout(300);
      // ---- Virtual End -----

      // Runtime.start("gui", "SwingGui");
      // Runtime.start("python", "Python");
      // Joystick joystick = (Joystick)Runtime.start("joystick",
      // "Joystick");
      // Runtime.start("joystick", "Joystick");

      Ssc32UsbServoController ssc = (Ssc32UsbServoController) Runtime.start("ssc", "Ssc32UsbServoController");
      ssc.connect(port, Serial.BAUD_115200);
      SerialDevice serial = ssc.getSerial();

      // Servo little = (Servo) Runtime.start("little", "Servo");
      Servo blue = (Servo) Runtime.start("blue", "Servo");
      Servo big = (Servo) Runtime.start("big", "Servo");

      blue.setPin(16);
      big.setPin(27);

      // ssc.attach(little);
      ssc.attach(blue);
      ssc.attach(big);

      /*
       * for (int i = 400; i < 3000; ++i){ big.writeMicroseconds(i); }
       */

      /// big.setVelocity(10);
      big.moveTo(0);
      big.moveTo(90);
      big.moveTo(180);
      big.moveTo(10);
      big.disable();
      big.moveTo(180);
      big.enable();
      big.moveTo(10);
      big.moveTo(160);

      big.detach(ssc);

      ssc.detach(big);

      boolean b = true;
      if (b) {
        return;
      }

      // 500 2500
      // serial.write("#17P0 #16P0 @ \r");
      serial.write("STOP 16 STOP 17 \r");
      serial.write("#16P800 #17P1500 #27P1100 \r");
      serial.write("#16P1000 #17P500 #27P2100 \r");
      serial.write("#16P500\r");
      serial.write("#16P2500\r");
      serial.write("#16P1500\r");// pos 0
      serial.write("#16P500S10\r");

      serial.write("#16P2000\r");

      serial.write("#16P0 #17P1500 #27P1100 \r");

      // Runtime.start("webgui", "WebGui");
      // Runtime.start("motor", "Motor");

      /*
       * SwingGui gui = new SwingGui("gui"); gui.startService();
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @Override
  public void connect(String port, int rate) throws Exception {
    connect(port, rate, 8, 1, 0);
  }

/* (non-Javadoc)
 * @see org.myrobotlab.service.interfaces.ServoController#enablePin(java.lang.Integer, java.lang.Integer)
 */
@Override
public void enablePin(Integer sensorPin, Integer i) {
	// TODO Auto-generated method stub
	
}

/* (non-Javadoc)
 * @see org.myrobotlab.service.interfaces.ServoController#disablePin(java.lang.Integer)
 */
@Override
public void disablePin(Integer i) {
	// TODO Auto-generated method stub
	
}

}
