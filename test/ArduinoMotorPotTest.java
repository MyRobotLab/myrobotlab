import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.MotorDualPwm;
import org.myrobotlab.service.Pid;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.data.SensorData;
import org.myrobotlab.test.TestUtils;
import org.slf4j.Logger;

@Ignore
public class ArduinoMotorPotTest {

  //public boolean uploadSketch = false;
  public boolean uploadSketch = false;
  
  public final static Logger log = LoggerFactory.getLogger(ArduinoMotorPotTest.class);
  private String port ="COM30";
  private String boardType ="uno";
  private int leftPwm = 6;
  private int rightPwm = 7;
  
  // A0 
  private int potPin = 0;

  private double kp = 0.050; 
  private double ki = 0.020;
  private double kd = 0.020;

  private Pid pid;
  private String key = "test";
  private MotorDualPwm motor;

  private int count = 0;
  private int rate = 5;

  private double tolerance = 1.5;
  private Arduino arduino;
  private String arduinoPath = "c:\\dev\\arduino-1.6.8\\";
  private String commandPath = "";
  private String additionalEnv = "";
  // platform dependent... (doesn't seem to require the .exe on windows)
  private String arduinoExecutable = "arduino";
  private String sketchFilename = "src\\resource\\Arduino\\MRLComm.c";
  // in order for arduino to update a sketch it needs to end in .ino and 
  // it needs to be in its own directory.
  private String destFilename = "\\MRLComm\\MRLComm.ino";
  // A helper function to upload the MRLComm sketch to the Arduino.
  // using the command line utilities.
  public void uploadMRLComm(String port, String board) throws IOException, InterruptedException {
    if (!(board.equals("uno") || board.equals("mega"))) {
      // TODO: validate the proper set of values.
      System.out.println("Invalid board type");
      return;
    }
    File src = new File(sketchFilename);
    File dest = new File(arduinoPath + destFilename);
    System.out.println("Copy from " +src.getAbsolutePath() + " to " + dest.getAbsolutePath());
    // copy MRLComm.c to MRLComm/MRLComm.ino for compilation and upload.
    FileUtils.copyFile(src, dest);
    // Create the command to run (and it's args.)
    String arduinoExe = arduinoPath + arduinoExecutable;
    ArrayList<String> args = new ArrayList<String>();
    // args.add("--verbose");
    args.add("--upload");
    args.add("--port");
    args.add(port);
    args.add("--board");
    args.add("arduino:avr:" + board);
    args.add(dest.getAbsolutePath());
    // run the command.
    String result = runCommand(arduinoExe, args);
    // print stdout/err from running the command
    System.out.println("Result..." + result);

    System.out.println("Uploaded Sketch.");
    System.out.flush();
    // take a breath...  We think it probably worked?  but not sure..
    Thread.sleep(2000);
  }
  
  @Test
  public void testArduinoMotPot() throws Exception {
    
    
    if (uploadSketch)
      uploadMRLComm(port, boardType);
    
    boolean enableLoadTiming = false;
    // Runtime.create("gui", "SwingGui");
    // initialize the logger 
    TestUtils.initEnvirionment();
    // Create the pid controller 
    pid = (Pid)Runtime.createAndStart("pid", "Pid");
    // # set the pid parameters KP KI KD  (for now just porportial control)
    pid.setPID(key, kp, ki, kd);
    int direction = 1;
    pid.setControllerDirection(key, direction);
    pid.setMode(key, 1);
    // clip the output values from the pid control to a range between -1 and 1. 
    pid.setOutputRange(key, -1.0, 1.0);
    // This is the desired sample value from the potentiometer 512 = ~ 90 degrees
    int desiredValue = 512;
    pid.setSetpoint(key, desiredValue);
    pid.setSampleTime(key, 40);
    // Start the arduino and the feedback potentiometer polling
    arduino = (Arduino)Runtime.createAndStart("arduino", "Arduino");
    // make arduino connect blocking (or at least as long as "getVersion()" takes.
    arduino.connect(port);
    // wait for the arduino to actually connect!
    // Start the motor and attach it to the arduino.
    motor = (MotorDualPwm)Runtime.createAndStart("motor", "Motor");
    motor.setPwmPins(leftPwm, rightPwm);
    motor.attachMotorController(arduino);
    // Sensor callback
    // arduino.analogReadPollingStart(potPin);
    // arduino.sensorAttach(this);
    
    // pin zero sample rate 1.  (TODO: fix the concept of a sample rate!)
    // we actually want it to be specified in Hz..  not cycles ...
    // AnalogPinSensor feedbackPot = new AnalogPinSensor(0,1);
    // feedbackPot.addSensorDataListener(this); // null config is this right ?
    // arduino.sensorAttach(feedbackPot);
    
    if (enableLoadTiming) {
      arduino.enableBoardInfo(true);
    }
    // stop the motor initially
    motor.move(0);
    System.out.println("Press the any key to exit.");
    System.in.read();

  }

  
  public void onSensorData(SensorData event) {
    // about we downsample this call?
	int[] data = (int[])event.getData();
    count++;
    int value = data[0];
    log.info("Data: {}", data);
    pid.setInput(key, value);
    pid.compute(key);
    double output = pid.getOutput(key);
    log.info("Data {} , Output : {}", data, output);
    if (Math.abs(pid.getSetpoint(key) - value) > tolerance) {
      // log.info("Setting pin mode as a test.");
     //  arduino.pinMode(6,0);
     // arduino.analogWrite(6, 0);
      //arduino.pinMode(address, mode);
      //arduino.digitalWrite(4, 0);
      if (count % rate == 0) {
        motor.invoke("move", output);
      }
      // motor.move(output);
      //motor.move(-1.0);
    } else {
      // we made it!
      log.info("Arrived.");
      if (motor.getPowerLevel() != 0) {
        motor.move(0);
      }
    }

  }

  
  /**
   * Helper function to run a system command and return the stdout / stderr as a string
   * 
   * @param program
   * @param args
   * @return
   * @throws IOException
   * @throws InterruptedException
   */
  protected String runCommand(String program , ArrayList<String> args) throws IOException, InterruptedException {

    ArrayList<String> command = new ArrayList<String>();
    command.add(program);
    if (args != null) {
      for (String arg : args) {
        command.add(arg);
      }
    }
    System.out.println("RUNNING COMMAND :" + join(command," "));

    ProcessBuilder builder = new ProcessBuilder(command);
    // we need to specify environment variables
    Map<String, String> environment = builder.environment();
    
    String ldLibPath = commandPath;
    if (additionalEnv.length() >0) {
      ldLibPath += ":" + additionalEnv;
    }
    
    environment.put("LD_LIBRARY_PATH", ldLibPath);
    Process handle = builder.start();

    InputStream stdErr = handle.getErrorStream();
    InputStream stdOut = handle.getInputStream();

    // TODO: we likely don't need this
    // OutputStream stdIn = handle.getOutputStream();

    StringBuilder outputBuilder = new StringBuilder();
    byte[] buff = new byte[4096];

    // TODO: should we read both of these streams? 
    // if we break out of the first loop is the process terminated?
    // read stdout
    for (int n; (n = stdOut.read(buff)) != -1;) {
      outputBuilder.append(new String(buff, 0, n));
    }
    // read stderr
    for (int n; (n = stdErr.read(buff)) != -1;) {
      outputBuilder.append(new String(buff, 0, n));
    }

    stdOut.close();
    stdErr.close();

    // TODO: stdin if we use it.
    // stdIn.close();

    // the process should be closed by now?

    handle.waitFor();

    handle.destroy();

    int exitValue = handle.exitValue();
    // print the output from the command
    System.out.println(outputBuilder.toString());
    System.out.println("Exit Value : " + exitValue);

    return outputBuilder.toString();
  }

  /**
   * Helper function to run a program , return the stderr / stdout as a string and 
   * to catch any exceptions that occur
   * 
   * @param cmd
   * @param args
   * @return
   */
  protected String RunAndCatch(String cmd, ArrayList<String> args) {
    String returnValue;
    try {
      returnValue = runCommand(cmd, args);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      returnValue = e.getMessage();
      e.printStackTrace();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      returnValue = e.getMessage();
      e.printStackTrace();
    }
    return returnValue;
  }
  
  // TODO: this should be on a string utils static class.
  private String join(ArrayList<String> list, String joinChar) {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    int size = list.size();
    for (String part : list) {
      i++;
      sb.append(part);
      if (i != size) {
        sb.append(joinChar);
      }
    }
    return sb.toString();
  }

public boolean isLocal() {
	// TODO Auto-generated method stub
	return true;
}



}

