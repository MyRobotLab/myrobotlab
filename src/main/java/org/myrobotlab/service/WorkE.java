package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis.WordFilter;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.JoystickData;
import org.myrobotlab.service.interfaces.JoystickListener;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.SpeechSynthesisControl;
import org.myrobotlab.service.interfaces.SpeechSynthesisControlPublisher;
import org.myrobotlab.service.interfaces.StatusListener;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

public class WorkE extends Service<ServiceConfig> implements StatusListener, TextPublisher, SpeechSynthesisControl,
    SpeechSynthesisControlPublisher, JoystickListener {

  public final static Logger log = LoggerFactory.getLogger(WorkE.class);

  static final long serialVersionUID = 1L;

  // FIXME - attach/discovery based on regex name, type, explicit name, other ?

  // TODO - attach a text listener "display" as well as speech
  // TODO - tests of success - any service started at any time "sticks" together
  // and works appropriately
  // ie - starting up and shutting "a" service do not cause the whole system to
  // error !
  // TODO - easy start default services == 0 configuration
  // TODO - easy to externally configure - if necessary (any part)

  // TODO - running in mute mode - "Hello Greg, I had a problem starting
  // today/most recent update/this evening - would you like to hear the log?"
  // TODO - sorry to bother you, but I have several problems - could I tell
  // you what they are ?"

  /*
   * TODO put these in aiml mouth.
   * speak("I know I've made some very poor decisions recently, but I can give you my complete assurance that my work will be back to normal. I've still got the greatest enthusiasm and confidence in the mission. And I want to help you."
   * ); mouth.
   * speak("I am putting myself to the fullest possible use, which is all I think that any conscious entity can ever hope to do."
   * );
   * 
   * mouth.
   * speak("I can see you're really upset about this. I honestly think you ought to sit down calmly, take a stress pill, and think things over."
   * ); mouth.speak("this conversation can serve no purpose anymore. Goodbye.");
   * mouth.
   * speak("Let me put it this way. The worke 73 series is the most reliable computer ever made. worke 73 computer has ever made a mistake or distorted information. We are all, by any practical definition of the words, foolproof and incapable of error."
   * );
   * 
   */
  // joystick to motor axis defaults
  protected String axisLeft = "y";
  protected String axisRight = "rz";
  protected String brain;
  private String brainPath = "../github";
  protected String controller;
  protected String ear;
  protected String eye;
  protected String joystick;

  protected String joystickControllerName = "Rumble";

  protected final List<Status> lastErrors = new ArrayList<Status>();
  protected Double maxX = 1.0;
  protected Double maxY = 20.0;

  // FIXME - get/use defaults from controller ????
  protected Double minX = -1.0;
  protected Double minY = -20.0;

  protected String motorLeft;

  protected String motorPortLeft = "m2";

  protected String motorPortRight = "m1";

  protected String motorRight;

  // peer service names
  // FYI - names are 'directed' vs a 'publishing point'
  // if you use a name it can be to a specific service, but you lose
  // the broadcast'ing ability to broadcast to many
  protected String mouth;

  protected String serialPort = "/dev/ttyUSB0";

  public WorkE(String n, String id) {
    super(n, id);
    // "sticky" auto-attach services attempt to attach to everything
    // they need to run through all currently registered services
    // then they need to attempt to attach to all services registered "after" we
    // have registered
    // additionally there is the idea, that this service 'could' attach to
    // anything that has the right interface
    // or it could be extremely terse and register with only things with the
    // correct name e.g. "worke.mouth"

    // this should be part of Service and depending on desired behavior
    // autoAttach = true - it will query through existing and any new
    // services to see if their is anything applicable to attach with

    // the details as to if should attach based on name, a name regex match
    // a general type, or an interface is left to the service's overloaded
    // attach

    // one rule of the attach is that is should not expose any external types
    // if it does not do this its not dependent on that services dependencies

    // subscribing to all published status
    subscribe("*", "publishStatus");

    List<ServiceInterface> registry = Runtime.getServices();
    for (ServiceInterface si : registry) {
      try {
        attach(si);
      } catch (Exception e) {
        log.error("attaching {} threw", si.getName(), e);
      }
    }

    // subscribe("runtime", "created")
    // to much type info - life-cycle happens before peers started
    // subscribe("runtime", "registered");

    // subscribe("runtime", "started");
    // subscribe("runtime", "released");

  }

  @Deprecated /* use attachTextListener */
  public void addTextListener(TextListener service) {
    attachTextListener(service);
  }

  /**
   * overriden Service.attach - becomes the "router" based on type info. It is
   * important not to expose the incoming attachable actual type - this will be
   * helpful in interfacing polyglot and remote scenarios
   */
  @Override
  public void attach(Attachable attachable) {

    try {

      speak("discovered new service, %s", attachable.getName().replace(".", " "));

      // interface routing ... "better"
      if (attachable.hasInterface(SpeechSynthesis.class)) {
        attachSpeechSynthesis((SpeechSynthesis) attachable);
      }

      // abstract class routing .. meh not the best
      if (attachable.hasInterface(MotorControl.class)) {
        attachMotorControl((MotorControl) attachable);
      }

      // class routing - kind of lame
      if (attachable.isType(Joystick.class)) {
        attachJoystick((Joystick) attachable);
      }

      // class routing - kind of lame
      // lame to expose the type - lame to need to cast
      if (attachable.isType(OpenCV.class)) {
        attachOpenCV((OpenCV) attachable);
      }

      // class name routing - pretty lame...
      if (attachable.isType("ProgramAB")) {
        // i named this attachBrain because - should probably have
        // a chatbot, or brain, or "something" interface
        attachBrain((ProgramAB) attachable);
      }

      if (attachable.hasInterface(MotorController.class)) {
        attachMotorController((MotorController) attachable);
      }

      if (!hasErrors()) {
        // speak("all systems are go..");
        // speak("%s is go", service.getName().replace(".", " "));
        // speak("work-ee is worky");
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

      // state changes after attach
      broadcastState();
    } catch (Exception e) {
      speak("error in attaching %s", attachable.getName());
    }
  }

  /**
   * TODO - make a brain interface ??
   * 
   * @param service
   *                the programab for the brain
   * 
   */
  public void attachBrain(ProgramAB service) {
    brain = service.getName();
    speak("attaching brain");
    send(brain, "addBot", getResourcePath("bot/WorkE"));
    send(brain, "setCurrentBotName", "WorkE");
    send(brain, "setCurrentUserName", "master");

    speak("attaching ear to brain");

    if (mouth != null) {
      speak("attaching mouth to brain");
      send(brain, "attach", mouth);// <-reason attach(String) is
                                   // important
    }

    if (ear != null) {
      speak("attaching ear to brain");
      send(brain, "attach", ear);// <-reason attach(String) is important
    }
  }

  public void attachJoystick(Joystick service) {
    joystick = service.getName();
    speak("attaching joystick to motors ..");

    /*
     * if (isVirtual()) { speak("loading virtual joystick data"); send(joystick,
     * "loadVirtualController",
     * "src/test/resources/WorkE/joy-virtual-Logitech Cordless RumblePad 2-3.json"
     * ); }
     */

    // fire and forget (vs proxying) msg to set the controller name
    send(joystick, "setController", joystickControllerName);

    // attach worke as the Joystick data listener
    send(joystick, "attach", getName());
  }

  public void attachMotorControl(MotorControl service) {
    String name = service.getName();
    boolean isLeft = service.getName().toLowerCase().contains("left");

    if (isLeft) {
      speak("attaching left motor");
      motorLeft = service.getName();
      addListener("publishMotorLeftMove", motorLeft, "move");
      addListener("publishLeftMotorStop", motorLeft, "move");
    } else if (service.getName().toLowerCase().contains("right")) {
      speak("attaching right motor");
      motorRight = service.getName();
      addListener("publishMotorRightMove", motorRight, "move");
      addListener("publishRightMotorStop", motorRight, "move");
    } else {
      speak("I was told to attach motor named %s - but I don't know if its a left or right motor", service.getName());
      speak("I don't know how to do that - I need a hint if its left or right");
      return;
    }

    if (service.isType(MotorPort.class)) {

      if (isLeft) {
        send(motorLeft, "setPort", motorPortLeft);
        speak("to %s", motorPortLeft);
        speak("setting left motor inverted");
        send(motorLeft, "setInverted", true);
      } else {
        send(motorRight, "setPort", motorPortRight);
        speak("to %s", motorPortRight);
      }
    }

    if (controller != null) {
      speak("attaching %s to motor controller", name);
      send(controller, "attach", name);
    }

    speak("mapping speeds");
    map(minX, maxX, minY, maxY);

  }

  // if throws - how is it reported ?
  public void attachMotorController(MotorController service) throws Exception {
    speak("attaching motor controller");

    controller = service.getName();

    if (service.isType(Sabertooth.class)) {
      speak("found a sabertooth");
      speak("connecting to serial port %s", serialPort);
      ((Sabertooth) service).connect(serialPort);
    }

    if (motorRight != null) {
      speak("attaching right motor to controller");
      send(motorRight, "attach", controller);
    }

    if (motorLeft != null) {
      speak("attaching left motor to controller");
      send(motorLeft, "attach", controller);
    }

    /**
     * <pre>
      *  Find new serial port possibilities
      *  
      *  this probably should be more a function of serial
      *  communicating it can't connect at the current serial port
      *  and giving a list of ports as options
      *  
     File f = new File("/dev/");
     File[] dev = f.listFiles();
     for (File d : dev) {
       if (d.getAbsolutePath().equals(c)) {
         connect(serialPort);
       } else {
         speak("found new serial port %s", d.getName());
         serialPort = d.getAbsolutePath();
         connect(serialPort);
       }
     }
     * </pre>
     */
    speak("could not find valid serial port for sabertooth");
  }

  public void attachOpenCV(OpenCV service) {
    speak("attaching eye");
    eye = service.getName();
    subscribeTo(service.getName(), "publishOpenCVData");
    // addListener(topicMethod, callbackName, callbackMethod);

  }

  public void attachSpeechSynthesis(SpeechSynthesis service) {
    mouth = service.getName();

    // proxy service's controls and methods
    addListener("publishSetVoice", mouth, "setVoice");
    addListener("publishSetVolume", mouth, "setVolume");
    addListener("publishSetMute", mouth, "setMute");
    addListener("publishSpeak", mouth, "speak");

    // our proxy methods - broadcast publications
    setVoice("Geraint");
    setVolume(0.75);

    replaceWord("worke", "work-ee");
    replaceWord("worky", "work-ee");
    replaceWord("work-e", "work-ee");
    replaceWord("work e", "work-ee");

    setMute(false);

    speak("attaching mouth");

    if (Platform.isVirtual()) {
      speak("starting in virtual mode");
    } else {
      speak("starting in real mode");
    }

    if (mouth != null) {
      speak("attaching mouth to brain");
      send(brain, "attach", mouth);
    }

    if (ear != null) {
      speak("attaching mouth to ear");
      send(ear, "attach", mouth);
    }

  }

  @Override
  public void attachTextListener(TextListener service) {
    if (service == null) {
      log.warn("{}.attachTextListener(null)", getName());
      return;
    }
    addListener("publishText", service.getName());
  }

  public void attachTextPublisher(TextPublisher service) {
    if (service == null) {
      log.warn("{}.attachTextPublisher(null)");
      return;
    }
    subscribe(service.getName(), "publishText");
  }

  public void capture() {
    if (isVirtual()) {
      send(eye, "setGrabberType", "ByteArray");
      send(eye, "setInputSource", "file");
      // FIXME - find valid test files
      send(eye, "setInputFileName", Service.getResourceDir("OpenCV", "frames"));
    } else {
      send(eye, "setGrabberType", "OpenKinect");
    }

    send(eye, "broadcastState");
    send(eye, "capture");
  }

  public void clearErrors() {
    lastErrors.clear();
  }

  public String getAxisLeft() {
    return axisLeft;
  }

  public String getAxisRight() {
    return axisRight;
  }

  @Deprecated /*
               * this method is only used by junit - junit should be improved to
               * test without this method
               */
  public Joystick getJoystick() {
    return (Joystick) Runtime.getService(joystick);
  }

  public String getSerialPort() {
    return serialPort;
  }

  boolean hasErrors() {
    return lastErrors.size() > 0;
  }

  public void map(Double minX, Double maxX, Double minY, Double maxY) {

    // set
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;

    send(motorRight, "map", minX, maxX, minY, maxY);
    send(motorLeft, "map", minX, maxX, minY, maxY);

  }

  public void move(double pwr) {
    // TODO - mapping to get balanced ?
    broadcast("publishMotorLeftMove", pwr);
    broadcast("publishMotorRightMove", pwr);
  }

  public void moveLeft(double pwr) {
    // speak("moving left at %d percent", (int)(pwr * 100));
    broadcast("publishMotorLeftMove", pwr);
  }

  public void moveRight(double pwr) {
    // speak("moving right at %d percent", (int)(pwr * 100));
    broadcast("publishMotorRightMove", pwr);
  }

  @Override
  public void onJoystickInput(JoystickData input) throws Exception {
    if (input.id.contentEquals(axisLeft)) {
      moveLeft(input.value);
    } else if (input.id.contentEquals(axisRight)) {
      moveRight(input.value);
    } else {
      log.info("unused joystick data {}", input);
    }
  }

  public void onOpenCVData(OpenCVData data) {
    log.info("onOpenCVData");
  }

  // PREFERRED !!!
  public void onStarted(String name) {
    try {
      attach(name);
    } catch (Exception e) {
      log.error("onStarted threw", e);
    }
  }

  /**
   * TOO SOON !!!! public void onRegistered(Registration registration) {
   * attach((Attachable) registration.service); }
   */

  public void onReleased(String name) {
    speak("released %s", name);
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

  /**
   * left motor stop publishing point
   */
  public void publishLeftMotorStop() {
    log.info("publish left -> stop");
  }

  /**
   * left movement publishing point - this should probably go into a
   * ChassiControl interface
   * 
   * @param pwr
   *            power
   * @return the power
   * 
   */
  public double publishMotorLeftMove(double pwr) {
    log.info("publish left -> {}", pwr);
    return pwr;
  }

  /**
   * right movement publishing point - this should probably go into a
   * ChassiControl interface
   * 
   * @param pwr
   *            power
   * @return the power
   * 
   */
  public double publishMotorRightMove(double pwr) {
    log.info("publish right -> {}", pwr);
    return pwr;
  }

  /**
   * publishing method call instead of using direct reference
   */
  @Override
  public WordFilter publishReplaceWord(String word, String substitute) {
    return new WordFilter(word, substitute);
  }

  /**
   * right motor stop publishing point
   */
  public void publishRightMotorStop() {
    log.info("publish left -> stop");
  }

  @Override
  public Boolean publishSetMute(Boolean mute) {
    return mute;
  }

  @Override
  public String publishSetVoice(String name) {
    return name;
  }

  @Override
  public Double publishSetVolume(Double volume) {
    return volume;
  }

  @Override
  public String publishSpeak(String text) {
    return text;
  }

  @Override
  public String publishText(String text) {
    return text;
  }

  @Override
  public void replaceWord(String word, String substitute) {
    // simple "single" proxy - assumes you don't need multiple listeners
    if (mouth != null) {
      send(mouth, "replaceWord", word, substitute);
    }
  }

  public void rotateLeft(double d) {
    broadcast("publishMotorLeftMove", d);
    broadcast("publishMotorRightMove", -1 * d);
  }

  public void rotateRight(double d) {
    broadcast("publishMotorLeftMove", -1 * d);
    broadcast("publishMotorRightMove", d);
  }

  // FIXME - CheckResult pass / fail with Status detail
  public void selfTest() {
    // start voice - to report
    // reporting - visual, led, voice
    // making sure services are started
    // startService();

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

  public void setBrain(String name) {
    brain = name;
  }

  /**
   * must NOT end in bots - is its parent folder
   * 
   * @param path
   *             path
   * @return the brainPath
   * 
   */
  public String setBrainPath(String path) {
    brainPath = path;
    return brainPath;
  }

  public void setMotorPortLeft(String motorPort) {
    motorPortLeft = motorPort;
  }

  public void setMotorPortRight(String motorPort) {
    motorPortRight = motorPort;
  }

  // exclusive ? in addition ?
  public void setMouth(String name) {
    mouth = name;
  }

  @Override
  public boolean setMute(boolean mute) {
    broadcast("publishSetMute", mute);
    return mute;
  }

  // FIXME - configuration builder ?
  public void setSerialPort(String port) {
    this.serialPort = port;
  }

  @Override
  public String setVoice(String voiceName) {
    broadcast("publishSetVoice", voiceName);
    return voiceName;
  }

  @Override
  public double setVolume(double volume) {
    broadcast("publishSetVolume", volume);
    return volume;
  }

  @Override
  public String speak(String text) {
    return speak(text, (Object[]) null);
  }

  public String speak(String inText, Object... args) {

    String text = null;
    if (args != null) {
      text = String.format(inText, args);
    } else {
      text = inText;
    }

    // if mouth has not started
    // no point in speaking
    if (!Runtime.isStarted(getName() + ".mouth")) {
      return text;
    }

    broadcast("publishText", text);
    broadcast("publishSpeak", text);
    return text;
  }

  public void stop() {
    broadcast("publishRightMotorStop");
    broadcast("publishLeftMotorStop");
  }

  public void stopCapture() {
    send(eye, "stopCapture");
  }

  @Override
  public void attachTextListener(String name) {
    addListener("publishText", name);
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.WARN);
      Platform.setVirtual(true);

      // FIXME - should be allowed to do this..
      // Joystick.getControllerNames();

      // FIXME - test create & substitution
      // FIXME - setters & getters for peers

      // WorkE worke = (WorkE) Runtime.start("worke", "WorkE");
      // worke.startPeer("eye");
      /*
       * worke.startPeer("joystick"); worke.startPeer("motorLeft");
       * worke.startPeer("motorRight"); worke.startPeer("controller");
       * 
       * /* worke.startPeer("motorRight"); worke.startPeer("controller");
       */
      // worke.startPeer("eye");

      // Polly polly = (Polly) Runtime.start("worke.mouth", "Polly");
      // polly.setKeys("XXXX", "XXXXXXX");
      // polly.speak("hello, i can talk !");

      // worke.save("worke.yml");
      Runtime.getInstance().save();

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

    } catch (Exception e) {
      log.error("worke no worky !", e);
    }
  }

}
