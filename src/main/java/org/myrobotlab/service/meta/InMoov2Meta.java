package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Pid.PidData;
import org.myrobotlab.service.config.FiniteStateMachineConfig;
import org.myrobotlab.service.config.InMoov2Config;
import org.myrobotlab.service.config.JMonkeyEngineConfig;
import org.myrobotlab.service.config.MarySpeechConfig;
import org.myrobotlab.service.config.MouthControlConfig;
import org.myrobotlab.service.config.NeoPixelConfig;
import org.myrobotlab.service.config.PidConfig;
import org.myrobotlab.service.config.RandomConfig;
import org.myrobotlab.service.config.RandomConfig.RandomMessageConfig;
import org.myrobotlab.service.config.TrackingConfig;
import org.myrobotlab.service.config.WebkitSpeechRecognitionConfig;
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

    addPeer("audioPlayer", "AudioFile");
    addPeer("chatBot", "ProgramAB");
    addPeer("controller3", "Arduino");
    addPeer("controller4", "Arduino");
    addPeer("ear", "WebkitSpeechRecognition");
    addPeer("eyeTracking", "Tracking");
    addPeer("fsm", "FiniteStateMachine");
    addPeer("head", "InMoov2Head");
    addPeer("headTracking", "Tracking");
    addPeer("htmlFilter", "HtmlFilter");
    addPeer("imageDisplay", "ImageDisplay");
    addPeer("leap", "LeapMotion");
    addPeer("left", "Arduino");
    addPeer("leftArm", "InMoov2Arm");
    addPeer("leftHand", "InMoov2Hand");
    addPeer("mouth", "MarySpeech");
    addPeer("mouthControl", "MouthControl");
    addPeer("neoPixel", "NeoPixel");
    addPeer("opencv", "OpenCV");
    addPeer("openWeatherMap", "OpenWeatherMap");
    addPeer("pid", "Pid");
    addPeer("pir", "Pir");
    addPeer("random", "Random");
    addPeer("right", "Arduino");
    addPeer("rightArm", "InMoov2Arm");
    addPeer("rightHand", "InMoov2Hand");
    addPeer("servoMixer", "ServoMixer");
    addPeer("simulator", "JMonkeyEngine");
    addPeer("torso", "InMoov2Torso");
    addPeer("ultrasonicRight", "UltrasonicSensor");
    addPeer("ultrasonicLeft", "UltrasonicSensor");

    addDependency("fr.inmoov", "inmoov2", null, "zip");
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
    inmoov.openWeatherMap = name + ".openWeatherMap";
    inmoov.neoPixel = name + ".neoPixel";
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
    fsm.states.add("safe_random_movements"); // random movements
    fsm.states.add("unsafe_random_movements"); // random movements
    fsm.states.add("tracking"); // tracking
    fsm.states.add("power_down"); // process of shutting down stuff

    fsm.transitions.add(new FiniteStateMachineConfig.Transition("start", "first_time", "init"));
    fsm.transitions.add(new FiniteStateMachineConfig.Transition("init", "first_time", "identify_user"));
    fsm.transitions.add(new FiniteStateMachineConfig.Transition("detected_face", "first_time", "identify_user"));

    // == Peer - random =============================
    RandomConfig random = (RandomConfig) plan.getPeerConfig("random");

    // setup name references to different services
    RandomMessageConfig rm = new RandomMessageConfig(3000, 8000, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);
    random.randomMessages.put(name + ".setLeftArmSpeed", rm);

    rm = new RandomMessageConfig(3000, 8000,  8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);
    random.randomMessages.put(name + ".setRightArmSpeed", rm);

    rm = new RandomMessageConfig(3000, 8000, 0.0, 5.0, 85.0, 95.0, 25.0, 30.0, 10.0, 15.0);
    random.randomMessages.put(name + ".moveLeftArm", rm);

    rm = new RandomMessageConfig(3000, 8000, 0.0, 5.0, 85.0, 95.0, 25.0, 30.0, 10.0, 15.0);
    random.randomMessages.put(name + ".moveRightArm", rm);

    rm = new RandomMessageConfig(3000, 8000, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);
    random.randomMessages.put(name + ".setLeftHandSpeed", rm);

    rm = new RandomMessageConfig(3000, 8000, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);
    random.randomMessages.put(name + ".setRightHandSpeed", rm);

    rm = new RandomMessageConfig(3000, 8000, 10.0, 160.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 130.0, 175.0);
    random.randomMessages.put(name + ".moveLeftHand", rm);

    rm = new RandomMessageConfig(3000, 8000, 10.0, 160.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 130.0, 175.0);
    random.randomMessages.put(name + ".moveRightHand", rm);

    rm = new RandomMessageConfig(3000, 8000, 8.0, 20.0, 8.0, 20.0, 8.0, 20.0);
    random.randomMessages.put(name + ".setHeadSpeed", rm);

    rm = new RandomMessageConfig(3000, 8000, 70.0, 110.0, 65.0, 115.0, 70.0, 110.0);
    random.randomMessages.put(name + ".moveHead", rm);

    rm = new RandomMessageConfig(3000, 8000, 2.0, 5.0, 2.0, 5.0, 2.0, 5.0);
    random.randomMessages.put(name + ".setTorsoSpeed", rm);

    rm = new RandomMessageConfig(3000, 8000, 85.0, 95.0, 88.0, 93.0, 70.0, 110.0);
    random.randomMessages.put(name + ".moveTorso", rm);

    // == Peer - mouthControl =============================
    MouthControlConfig mouthControl = (MouthControlConfig) plan.getPeerConfig("mouthControl");

    // setup name references to different services
    mouthControl.jaw = name + ".head.jaw";
    mouthControl.mouth = name + ".mouth";

    // == Peer - headTracking =============================
    TrackingConfig headTracking = (TrackingConfig) plan.getPeerConfig("headTracking");

    // setup name references to different services
    headTracking.tilt = name + ".head.neck";
    headTracking.pan = name + ".head.rothead";
    headTracking.cv = name + ".opencv";
    headTracking.pid = name + ".pid";
    headTracking.controller = name + ".left";

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
    tiltPid.ki = 1.0;
    tiltPid.kp = 30.0;
    pid.data.put(headTracking.tilt, tiltPid);

    PidData panPid = new PidData();
    panPid.ki = 1.0;
    panPid.kp = 15.0;
    pid.data.put(headTracking.pan, panPid);

    PidData eyeTiltPid = new PidData();
    eyeTiltPid.ki = 1.0;
    eyeTiltPid.kp = 30.0;
    pid.data.put(eyeTracking.tilt, eyeTiltPid);

    PidData eyePanPid = new PidData();
    eyePanPid.ki = 1.0;
    eyePanPid.kp = 15.0;
    pid.data.put(eyeTracking.pan, eyePanPid);

    NeoPixelConfig neoPixel = (NeoPixelConfig) plan.getPeerConfig("neoPixel");
    neoPixel.pin = 2;
    neoPixel.controller = String.format("%s.left", name);
    neoPixel.red = 12;
    neoPixel.green = 180;
    neoPixel.blue = 212;
    neoPixel.pixelCount = 16;
    neoPixel.currentAnimation = "Ironman";

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
