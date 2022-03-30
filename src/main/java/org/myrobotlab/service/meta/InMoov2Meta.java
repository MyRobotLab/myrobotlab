package org.myrobotlab.service.meta;
import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Pid.PidData;
import org.myrobotlab.service.config.FiniteStateMachineConfig;
import org.myrobotlab.service.config.InMoov2Config;
import org.myrobotlab.service.config.NeoPixelConfig;
import org.myrobotlab.service.config.PidConfig;
import org.myrobotlab.service.config.RuntimeConfig;
import org.myrobotlab.service.config.TrackingConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoov2Meta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoov2Meta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public InMoov2Meta() {

    addDescription("InMoov2 Service");
    addCategory("robot");

    // skeletal parts
    addPeer("head", "InMoov2Head");
    addPeer("torso", "InMoov2Torso");
    // addPeer("eyelids", "InMoovEyelids", "eyelids");
    addPeer("leftArm", "InMoov2Arm");
    addPeer("leftHand", "InMoov2Hand");
    addPeer("rightArm", "InMoov2Arm");
    addPeer("rightHand", "InMoov2Hand");

    addPeer("leap", "LeapMotion");

    addPeer("opencv", "OpenCV");

    addPeer("left", "Arduino");
    addPeer("right", "Arduino");

    addPeer("mouthControl", "MouthControl");
    addPeer("ultrasonicRight", "UltrasonicSensor");
    addPeer("ultrasonicLeft", "UltrasonicSensor");
    addPeer("pir", "Pir");
    addPeer("pid", "Pid");

    addPeer("servoMixer", "ServoMixer");

    // the two legacy controllers .. :(
    addPeer("left", "Arduino");
    addPeer("right", "Arduino");
    addPeer("controller3", "Arduino");
    addPeer("controller4", "Arduino");

    addPeer("htmlFilter", "HtmlFilter");

    // Sensors -----------------
    addPeer("opencv", "OpenCV", "opencv");
    addPeer("ultrasonicRight", "UltrasonicSensor", "measure distance on the right");
    addPeer("ultrasonicLeft", "UltrasonicSensor", "measure distance on the left");
    addPeer("pir", "Pir", "infrared sensor");

    addPeer("mouth", "MarySpeech");
    addPeer("ear", "WebkitSpeechRecognition");

    addPeer("imageDisplay", "ImageDisplay");

    addPeer("headTracking", "Tracking");
    addPeer("eyeTracking", "Tracking");

    addPeer("neopixel", "NeoPixel");

    addPeer("audioPlayer", "AudioFile");
    addPeer("random", "Random");
    addPeer("simulator", "JMonkeyEngine");

    addDependency("fr.inmoov", "inmoov2", null, "zip");

    // the two legacy controllers .. :(
    addPeer("left", "Arduino");
    addPeer("right", "Arduino");
    addPeer("controller3", "Arduino");
    addPeer("controller4", "Arduino");
    
    addPeer("fsm", "FiniteStateMachine");
  }

  @Override
  public Plan getDefault(String name) {

    InMoov2Config inmoov = new InMoov2Config();
    
    Plan plan = new Plan(name);
    // load default peers from meta here
    plan.putPeers(name, peers);

    inmoov.audioPlayer = name + ".audioPlayer";
    inmoov.chatBot = name + ".chatBot";
    inmoov.ear = name + ".ear";
    inmoov.eyeTracking = name + ".eyeTracking";
    inmoov.fsm = name + ".fsm";
    inmoov.head = name + ".head";
    inmoov.headTracking = name + ".headTracking"; 
    inmoov.htmlFilter = name + ".htmlFilter";
    inmoov.imageDisplay = name + ".imageDisplay";
    inmoov.leftArm = name + ".leftArm";
    inmoov.leftHand = name + ".leftHand";
    inmoov.mouth = name + ".mouth";
    inmoov.mouthControl = name + ".mouthControl";    
    inmoov.opencv = name + ".opencv";
    inmoov.pid = name + ".pid";
    inmoov.pir = name + ".pir";
    inmoov.random = name + ".random";
    inmoov.rightArm = name + ".rightArm";
    inmoov.rightHand = name + ".rightHand";
    inmoov.servoMixer = name + ".servoMixer";
    inmoov.simulator = name + ".simulator";
    inmoov.torso = name + ".torso";
    inmoov.ultrasonicRight = name + ".ultrasonicRight";
    inmoov.ultrasonicLeft = name + ".ultrasonicLeft";
    
    FiniteStateMachineConfig fsm = (FiniteStateMachineConfig) plan.getPeerConfig("fsm");
    fsm.states.add("start"); // fist time
    fsm.states.add("init"); // fist time
    fsm.states.add("identify_user"); // fist time
    fsm.states.add("detected_face"); // fist time
    fsm.states.add("sleeping"); // pir running ? wake word ?
    fsm.states.add("executing_gesture"); // gesture running
    fsm.states.add("safe_random_movements");  // random movements
    fsm.states.add("unsafe_random_movements");  // random movements
    fsm.states.add("tracking"); // tracking
    fsm.states.add("power_down"); // process of shutting down stuff
    
    fsm.transitions.add(new FiniteStateMachineConfig.Transition("start","first_time","init"));
    fsm.transitions.add(new FiniteStateMachineConfig.Transition("init","first_time","identify_user"));
    fsm.transitions.add(new FiniteStateMachineConfig.Transition("detected_face","first_time","identify_user"));
    
    
    // == Peer - headTracking =============================
    TrackingConfig headTracking = (TrackingConfig) plan.getPeerConfig("headTracking");

    // setup name references to different services
    headTracking.tilt = name + ".head.neck";
    headTracking.pan = name + ".head.rothead";
    headTracking.cv = name + ".opencv";
    headTracking.pid = name + ".pid";

    // == Peer - eyeTracking =============================
    TrackingConfig eyeTracking = (TrackingConfig) plan.getPeerConfig("eyeTracking");

    // setup name references to different services
    eyeTracking.tilt = name + ".head.eyeY";
    eyeTracking.pan = name + ".head.eyeX";
    eyeTracking.cv = name + ".opencv";
    eyeTracking.pid = name + ".pid";
    eyeTracking.controller = name + ".left";

    // == Peer - pid =============================
    inmoov.pid = name + ".pid";
    PidConfig pid = (PidConfig) plan.getPeerConfig("pid");

    PidData tiltPid = new PidData();
    tiltPid.ki = 0.001;
    tiltPid.kp = 0.035;
    pid.data.put(headTracking.tilt, tiltPid);

    PidData panPid = new PidData();
    panPid.ki = 0.001;
    panPid.kp = 0.015;
    pid.data.put(headTracking.pan, panPid);

    PidData eyeTiltPid = new PidData();
    eyeTiltPid.ki = 0.001;
    eyeTiltPid.kp = 0.035;
    pid.data.put(eyeTracking.tilt, eyeTiltPid);

    PidData eyePanPid = new PidData();
    eyePanPid.ki = 0.001;
    eyePanPid.kp = 0.015;
    pid.data.put(eyeTracking.pan, eyePanPid);
    
    NeoPixelConfig neopixel = (NeoPixelConfig) plan.getPeerConfig("neopixel");
    neopixel.pin = 2;
    neopixel.controller = String.format("%s.left", name);
    neopixel.red = 12;
    neopixel.green = 180;
    neopixel.blue = 212;
    neopixel.pixelCount = 16;
    neopixel.currentAnimation = "Ironman";

    // remove undesired defaults from our default
    plan.removeConfig(name + ".headTracking.tilt");
    plan.removeConfig(name + ".headTracking.pan");
    plan.removeConfig(name + ".headTracking.pid");
    plan.removeConfig(name + ".headTracking.controller");
    plan.removeConfig(name + ".headTracking.controller.serial");
    plan.removeConfig(name + ".headTracking.cv");

    plan.removeConfig(name + ".eyeTracking.tilt");
    plan.removeConfig(name + ".eyeTracking.pan");
    plan.removeConfig(name + ".eyeTracking.pid");
    plan.removeConfig(name + ".eyeTracking.controller");
    plan.removeConfig(name + ".eyeTracking.controller.serial");
    plan.removeConfig(name + ".eyeTracking.cv");

    inmoov.autoStartPeers = false;
    plan.addConfig(inmoov);
    
    return plan;
  }


}
