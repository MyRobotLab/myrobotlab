import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Motor;
import org.myrobotlab.service.PID2;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.SensorDataSink;
import org.myrobotlab.test.TestUtils;
import org.slf4j.Logger;

@Ignore
public class ArduinoMotorPotTest implements SensorDataSink {

  public final static Logger log = LoggerFactory.getLogger(ArduinoMotorPotTest.class);
  private String port ="COM30";
  private int leftPwm = 6;
  private int rightPwm = 7;
  private int potPin = 14;

  private double kp = 0.050; 
  private double ki = 0.020;
  private double kd = 0.020;

  private PID2 pid;
  private String key = "test";
  private Motor motor;

  // private int count = 0;
  // private int rate = 5;

  private double tolerance = 1.5;

  private Arduino arduino;
  
  @Test
  public void testArduinoMotPot() throws Exception {


    
    boolean enableLoadTiming = false;
    // Runtime.create("gui", "GUIService");

    // initialize the logger 
    TestUtils.initEnvirionment();
    // Create the pid controller 
    pid = (PID2)Runtime.createAndStart("pid", "PID2");
    // # set the pid parameters KP KI KD  (for now just porportial control)
    pid.setPID(key, kp, ki, kd);
    int direction = 1;
    pid.setControllerDirection(key, direction);
    pid.setMode(key, 1);
    // clip the output values from the pid control to a range between -1 and 1. 
    pid.setOutputRange(key, -1.0, 1.0);
    // This is the desired sample value from the potentiometer 512 = ~ 90 degrees
    int desiredValue = 223;
    pid.setSetpoint(key, desiredValue);
    pid.setSampleTime(key, 40);

    // Start the arduino and the feedback potentiometer polling
    arduino = (Arduino)Runtime.createAndStart("arduino", "Arduino");
    // make arduino connect blocking (or at least as long as "getVersion()" takes.
    arduino.connect(port);
    // wait for the arduino to actually connect!
    // arduino.getVersion();

    // Start the motor and attach it to the arduino.
    motor = (Motor)Runtime.createAndStart("motor", "Motor");
    motor.setType2Pwm(leftPwm, rightPwm);
    motor.attach(arduino);

    // Sensor callback
    arduino.analogReadPollingStart(potPin);
    arduino.sensorAttach(this);


    if (enableLoadTiming) {
      arduino.setLoadTimingEnabled(true);
    }
    // stop the motor initially
    motor.move(0);

    //Thread.sleep(5000);
    //System.out.println("Starting polling");

    System.out.println("Press the any key to exit.");
    System.in.read();

  }

  @Override
  public void update(Object data) {
    // about we downsample this call?
    // count++;
    //if (count % rate == 0) {
    log.info("Data: {}", data);
    pid.setInput(key,(Integer)data);
    pid.compute(key);
    double output = pid.getOutput(key);
    log.info("Data {} , Output : {}", data, output);
    if (Math.abs(pid.getSetpoint(key) - (Integer)data) > tolerance) {
      log.info("Setting pin mode as a test.");
      arduino.pinMode(6,0);
      //motor.move(output);
      //motor.move(-1.0);
    } else {
      // we made it!
      log.info("Arrived.");
      //motor.move(0);
    }

  }

  @Override
  public int getDataSinkType() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "feedback";
  }

  @Override
  public int getSensorType() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int[] getSensorConfig() {
    // TODO Auto-generated method stub
    return new int[]{potPin};
  }

}

