package org.myrobotlab.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.abstracts.AbstractMotor;
import org.myrobotlab.service.abstracts.AbstractMotorController;
import org.myrobotlab.service.abstracts.AbstractSpeechRecognizer;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.interfaces.StatusListener;
import org.slf4j.Logger;

/**
 * <pre>
 * FIXME  
 *        - auto-start peers
 *        - "easy" sensor fusion -  multi-callback channels lead to a single channel
 *        - FIXME - aiml integration
 *        - FIXME - running in mute mode - "Hello Greg, I had a problem starting today/most recent update/this evening - would you like to hear the log?"
 *        - FIXME - sorry to bother you, but I have several problems - could I tell you what they are ?"
 *        - virtual joystick 
 *        - FULL path displayed in ProgramAB "bots" 
 *        - jostick control of motors !!!! get it f*ing moving again !! 
 *        - NAVIGATE !!
 *        - FIXME - moveTo(35) // 35 cm using "all" encoders send to sensor fusion
 *        
 * -Dhttp.proxyHost=webproxy -Dhttp.proxyPort=8080 -Dhttps.proxyHost=webproxy -Dhttps.proxyPort=8080
 *  FOSCAM WORKY !!! - for IPCamera frame grabber
 * 
 * http://admin:admin@192.168.0.37/videostream.cgi?user=admin(and)pwd=admin
 *
 * </pre>
 * 
 * @author GroG
 *
 */
public class WorkE extends Service implements StatusListener {

  public final static Logger log = LoggerFactory.getLogger(WorkE.class);

  static final long serialVersionUID = 1L;

  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(WorkE.class);

    meta.addPeer("git", "Git", "synching repos");

    // motor control - output
    meta.addPeer("joystick ", "Joystick", "joystick control");
    meta.addPeer("controller", "Sabertooth", "motor controller");
    meta.addPeer("motorLeft", "MotorPort", "left motor");
    meta.addPeer("motorRight", "MotorPort", "right motor");

    meta.addPeer("webgui", "WebGui", "web interface");

    // vision - input
    // TODO - going to have several "spouts" - and bolts (storm analogy)
    meta.addPeer("cv ", "OpenCV", "computer vision");// webcam spout
    // meta.addPeer("leftFoscam ", "OpenCV", "computer vision");// webcam spout

    // speech - output
    meta.addPeer("mouth ", "Polly", "mouth");

    // ear - input
    meta.addPeer("ear", "WebkitSpeechRecognition", "ear");

    // brain - input/output
    meta.addPeer("brain", "ProgramAB", "ear");

    // emoji - output
    meta.addPeer("emoji", "Emoji", "emotional state machine");

    meta.addDescription("the worke bot !");
    meta.addCategory("robot");
    return meta;
  }

  public static void main(String[] args) {
    try {

      // LoggingFactory.init(Level.INFO);
      Platform.setVirtual(true);
    
      // FIXME - should be allowed to do this..
      // Joystick.getControllerNames();

      // FIXME - test create & substitution
      // FIXME - setters & getters for peers
      // WorkE worke = (WorkE) Runtime.create("worke", "WorkE");

      Runtime.start("gui", "SwingGui");
      WorkE worke = (WorkE) Runtime.start("worke", "WorkE");
      worke.exportAll("worke.py");

    } catch (Exception e) {
      log.error("worke no worky !", e);
    }
  }

  // joystick to motor axis defaults
  String axisLeft = "y";
  String axisRight = "rz";

  // peers
  transient AbstractMotor motorRight;
  transient AbstractSpeechSynthesis mouth;
  transient ProgramAB brain;
  transient AbstractMotorController controller;
  transient Git git;
  transient OpenCV cv;
  transient ImageDisplay display;
  transient AbstractSpeechRecognizer ear;
  transient Emoji emoji;
  transient FiniteStateMachine fsm;
  transient Joystick joystick;
  transient AbstractMotor motorLeft;
  transient WebGui webgui;

  // virtual uart for controller
  transient Serial uart = null;

  // joystick controller default
  String joystickControllerName = "Rumble";

  final List<Status> lastErrors = new ArrayList<Status>();
  Double maxX = 1.0;
  Double maxY = 20.0;

  // FIXME - get/use defaults from controller ????
  Double minX = -1.0;
  Double minY = -20.0;

  String motorPortLeft = "m2";
  String motorPortRight = "m1";

  boolean mute = false;

  String serialPort = "/dev/ttyUSB0";

  private String brainPath = "../github";

  public WorkE(String n, String id) {
    super(n, id);
  }

  // FIXME
  // this is the applying of "all" configurations
  // it would be very good if this was always isolated to one standardized named
  // method
  // so any order dependent applying or attaching could be done
  // FIXME - possible default attach - (which typically requires configuration)
  // - in this particular case it was "randomly" decided that 2 parameters
  // FIXME - no defaults ?
  public void attach() throws Exception {

    // speakBlocking = true; FIXME - promote to Abstract

    // mute();

    // FIXME - running in mute mode - "Hello Greg, I had a problem starting
    // today/most recent update/this evening - would you like to hear the log?"
    // FIXME - sorry to bother you, but I have several problems - could I tell
    // you what they are ?"

    setVolume(0.75);
    /// speakBlocking(true);
    // mouth.setBlocking(true);
    mouth.setVoice("Brian"); // Brian
    mouth.addSubstitution("worke", "work-ee");
    mouth.addSubstitution("worky", "work-ee");
    mouth.addSubstitution("work-e", "work-ee");
    mouth.addSubstitution("work e", "work-ee");

    if (isVirtual()) {
      speak("running in virtual mode. creating virtual port");
      // FIXME - services should know when and how to become virtual
      // controller virtualization
      uart = Serial.connectVirtualUart(serialPort);
      uart.logRecv(true);// # dump bytes sent from controller

      // FIXME - this is "test" virtualization vs generalized virtualization -
      // rumble-pad tele-operation virtualization
      joystick = (Joystick) createPeer("joystick");
      // static ???

      speak("loading virtual joystick data");
      joystick.loadVirtualController("src/test/resources/WorkE/joy-virtual-Logitech Cordless RumblePad 2-3.json");
      // Runtime.start("gui", "SwingGui");
      broadcastState();

    }

    /*
     * TODO put these in aiml mouth.
     * speak("I know I've made some very poor decisions recently, but I can give you my complete assurance that my work will be back to normal. I've still got the greatest enthusiasm and confidence in the mission. And I want to help you."
     * ); mouth.
     * speak("I am putting myself to the fullest possible use, which is all I think that any conscious entity can ever hope to do."
     * );
     * 
     * mouth.
     * speak("I can see you're really upset about this. I honestly think you ought to sit down calmly, take a stress pill, and think things over."
     * );
     * mouth.speak("this conversation can serve no purpose anymore. Goodbye.");
     * mouth.
     * speak("Let me put it this way. The worke 73 series is the most reliable computer ever made. worke 73 computer has ever made a mistake or distorted information. We are all, by any practical definition of the words, foolproof and incapable of error."
     * );
     * 
     */
    // FIXME - sleep(1000); should be pauseContinue() which blocks and "stops"
    // waiting for input if error
    speak("subscribing to errors.");
    subscribe("*", "publishStatus");
    sleep(1000);

    speak("attaching joystick");
    // FIXME - do all createPeers here ???? No - can be left in startService as
    // a
    // startPeer
    // motorLeft = (AbstractMotor) createPeer("motorLeft");
    // motorRight = (AbstractMotor) createPeer("motorRight");

    // FIXME - what did you say ? -> says last thing

    // joystick.setController(joystickControllerIndex);
    joystick.setController(joystickControllerName);
    sleep(1000);

    // if error(continue?)

    speak("attaching motors to motor ports..");

    ((MotorPort) motorLeft).setPort(motorPortLeft);
    ((MotorPort) motorRight).setPort(motorPortRight);

    controller.attach(motorLeft);
    controller.attach(motorRight);

    sleep(1000);

    speak("attaching joystick to motors ..");
    motorLeft.attach(joystick.getAxis(axisLeft));
    motorRight.attach(joystick.getAxis(axisRight));

    sleep(1000);

    speak("mapping speeds");
    map(minX, maxX, minY, maxY);

    sleep(1000);

    speak("setting left motor inverted");
    motorLeft.setInverted(true);
    sleep(1000);
    
    // String botsDir = getRootDataDir();
    String fullBrainPath = brainPath + fs +  "bots" + fs + "worke";
    
    speak("synching repos");
    git.sync(getResourceDir() + fs + "react", "https://github.com/MyRobotLab/myrobotlab-react.git");
    git.sync(fullBrainPath, "https://github.com/MyRobotLab/worke.git");
    
    if (new File(fullBrainPath).exists()) {

      speak("attaching brain");
      // brain.setPath("..");
      brain.setPath(brainPath);
      brain.setCurrentBotName("worke"); // does this create a session ?
      brain.reloadSession("greg", "worke");
      // brain.reloadSession("greg", "worke"); // is this necessary??
    } else {
      speak("could not find a brain.  i looked everywhere for it");
    }

    speak("attaching ear to brain");
    brain.attach(ear);

    speak("attaching mouth to brain");
    brain.attach(mouth);
    sleep(1000);

    // speak("opening eye");
    // capture();
    sleep(1000);

    speak("connecting serial port");
    connect();
    sleep(1000);

    // TODO - timing ... & context
    // TODO - joystick used in a certain amount of time .. says, "manual
    // joystick override detecte - you have control"

    /*
     * <pre> refactor with moveTo after we have some form of encoder
     * speak("moving forward"); move(0.7); sleep(2000);
     * 
     * speak("turning left"); turnLeft(0.7); sleep(500); stop();
     * speak("turning right"); turnRight(0.7); sleep(1000); turnLeft(0.7);
     * sleep(500); speak("moving back"); move(-0.7); sleep(2000);
     * 
     * speak("stopping"); stop(); sleep(1000); </pre>
     */

    if (!hasErrors()) {
      speak("all systems are go..");
      speak("worke is worky");
    } else {
      speak("not all systems are fully functional");
      speak("would you like a list of things not working?");
      // aiml Yes,Sure,Ok,Go For it,Fine || No, Nope, nuh uh, naw ?

    }

    clearErrors();

    // speak("i am ready");

    // cv.broadcastState();

    /*
     * cv.broadcastState(); brain.broadcastState(); joystick.broadcastState();
     * controller.broadcastState(); motorLeft.broadcastState();
     * motorRight.broadcastState();
     */
    // speakBlocking(false);

    // FIXME - status
    // camera state
    // chassi state
    // battery level
    // charging state

  }

  public void capture() {
    if (isVirtual()) {
      cv.setGrabberType("ByteArray");
      cv.setInputSource("file");
      cv.setInputFileName("C:\\mrl.ssh\\frames");
    } else {
      cv.setGrabberType("OpenKinect");
    }
    cv.broadcastState();
    cv.capture();
  }

  public void clearErrors() {
    lastErrors.clear();
  }

  // FIXME ... FIND USB PORT - if /dev/ttyUSB0 doesn't exist find another
  // report a status that port has changed - found new port
  public void connect() throws Exception {
    if (isVirtual()) {
      connect(serialPort);
    } else {
      File f = new File("/dev/");
      File[] dev = f.listFiles();
      for (File d : dev) {
        if (d.getAbsolutePath().equals(serialPort)) {
          connect(serialPort);
        } else {
          speak("found new serial port %s", d.getName());
          serialPort = d.getAbsolutePath();
          connect(serialPort);
        }
      }
      speak("could not find valid serial port for sabertooth");
    }
  }

  public void connect(String port) throws Exception {
    controller = (AbstractMotorController) startPeer("controller");
    controller.connect(port);
  }

  public String getAxisLeft() {
    return axisLeft;
  }

  public String getAxisRight() {
    return axisRight;
  }

  public ProgramAB getBrain() {
    if (brain == null) {
      brain = (ProgramAB) startPeer("brain");
    }
    return brain;
  }

  public AbstractMotorController getController() {
    return controller;
  }

  public OpenCV getCv() {
    return cv;
  }

  public ImageDisplay getDisplay() {
    return display;
  }

  public Joystick getJoystick() {
    return joystick;
  }

  public AbstractMotor getMotorLeft() {
    return motorLeft;
  }

  public AbstractMotor getMotorRight() {
    return motorRight;
  }

  public AbstractSpeechRecognizer getRecognizer() {
    return ear;
  }

  public String getSerialPort() {
    return serialPort;
  }

  public AbstractSpeechSynthesis getSpeech() {
    return mouth;
  }

  boolean hasErrors() {
    return lastErrors.size() > 0;
  }

  public List<String> listVoices() {
    List<String> voiceNames = mouth.getVoiceNames();
    for (String name : voiceNames) {
      speak(name);
    }
    return voiceNames;
  }

  public void map(double minX, double maxX, double minY, double maxY) {
    // GOOD - guaranteed to get "a" motor ... probably even the "right" motor !!
    motorLeft = (AbstractMotor) createPeer("motorLeft");
    motorRight = (AbstractMotor) createPeer("motorRight");

    // set
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;

    motorLeft.map(minX, maxX, minY, maxY);
    motorRight.map(minX, maxX, minY, maxY);
  }

  public void move(double d) {
    motorLeft.move(d);
    motorRight.move(d);
  }

  public void mute() {
    mute = true;
  }

  @Override
  public void onStatus(Status status) {
    if (status.isError()) {
      // speak(status.toString());
      speak("%s has had an error", status.source);
      lastErrors.add(status);
    } else if (status.isWarn()) {
      // speak(status.toString());
      speak("%s has had a warning", status.source);
      // lastErrors.add(status);
    } else {
      // speak(status.toString());
    }
    
    
  }

  // FIXME - CheckResult pass / fail with Status detail
  public void selfTest() {
    // start voice - to report
    // reporting - visual, led, voice
    // making sure services are started
    // startService();

    mouth = (AbstractSpeechSynthesis) startPeer("mouth");
    Platform platform = Platform.getLocalInstance();
    speak("self status report");
    if (isVirtual()) {
      speak("running in virtual mode"); // get CmdOptions - running in
                                        // "automatic update" mode
    } else {
      speak("running in real mode");
    }

    speak(String.format("branch %s version %s", platform.getBranch(), platform.getVersion().split("\\.")[2]));

    // FIXME - check number of errors - option to clear errors
    speak(String.format("i have been alive for %s", Runtime.getUptime()));

    speak(String.format("%d services currently running", Runtime.getServiceNames().length));
    // FIXME X number of errors - "categorize them" - "self knowledge"
    // FIXME - relays - giving power
    // FIXME - StatusListener

    speak(String.format("i have %d errors", lastErrors.size()));

    if (lastErrors.size() > 0) {
      speak(String.format("last error occurred %s", Runtime.getDiffTime(System.currentTimeMillis() - lastErrorTs)));
    }

    // stop motors

    // check if started
    // check if attached
    // check if connected
    // check if manual control exists - joystick
    // check mouth
    // check mouth recognition - can i hear myself - check ;)
    // check network
    // check power / battery level - power meter
    // check if can see
    // check if can move

    // check news
    // check calender
    // check ethereum :p

  }

  public void setAxisLeft(String axisLeft) {
    this.axisLeft = axisLeft;
  }

  public void setAxisRight(String axisRight) {
    this.axisRight = axisRight;
  }

  public void setJoystick(String joystickControllerName) {
    this.joystickControllerName = joystickControllerName;
  }

  public void setMotorPortLeft(String motorPort) {
    motorPortLeft = motorPort;
  }

  public void setMotorPortRight(String motorPort) {
    motorPortRight = motorPort;
  }

  // FIXME - configuration builder ?
  public void setSerialPort(String port) {
    this.serialPort = port;
  }

  // aiml - "set voice to bob"
  public boolean setVoice(String name) {
    return mouth.setVoice(name);
  }

  // aiml - "set volume 30 percent"
  public void setVolume(double volume) {
    mouth.setVolume(volume);
  }

  public void speak(String inText, Object... args) {
    String text = String.format(inText, args);
    if (!mute) {
      mouth.speak(text);
    }
  }

  public void speakBlocking(boolean b) {
    mouth.setBlocking(b);
  }

  public void startService() {
    try {
      super.startService(); // FIXME framework should do this !!
      // GOOD ? - start "typeless" (because type is defined in meta data)
      // services here

      // FIXME FIXME FIXME - make worky through framework - no manual starts
      // here !!!      
      git = (Git) startPeer("git");
      controller = (AbstractMotorController) startPeer("controller");
      joystick = (Joystick) startPeer("joystick");
      motorLeft = (AbstractMotor) startPeer("motorLeft");
      motorRight = (AbstractMotor) startPeer("motorRight");
      cv = (OpenCV) startPeer("cv");
      mouth = (AbstractSpeechSynthesis) startPeer("mouth");
      ear = (AbstractSpeechRecognizer) startPeer("ear");
      emoji = (Emoji) startPeer("emoji");
      display = emoji.getDisplay();// (ImageDisplay) startPeer("display");
      fsm = emoji.getFsm();
      brain = (ProgramAB) startPeer("brain");
      webgui = (WebGui) startPeer("webgui");

      attach();

    } catch (Exception e) {
      error(e);
    }
  }
  
  /**
   * must NOT end in bots - is its parent folder
   * @param path
   * @return
   */
  public String setBrainPath(String path) {
    brainPath = path;
    return brainPath;
  }

  public void stop() {
    motorLeft.stop();
    motorRight.stop();
  }

  public void stopCapture() {
    cv.stopCapture();
  }

  public void turnLeft(double d) {
    motorLeft.move(d);
    motorRight.move(-1 * d);
  }

  public void turnRight(double d) {
    motorLeft.move(-1 * d);
    motorRight.move(d);
  }

  public void unmute() {
    mute = false;
  }
}