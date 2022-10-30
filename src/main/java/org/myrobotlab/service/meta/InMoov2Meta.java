package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.framework.Service;
import org.myrobotlab.jme3.UserData;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.service.InMoov2;
import org.myrobotlab.service.Pid.PidData;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.FiniteStateMachineConfig;
import org.myrobotlab.service.config.HtmlFilterConfig;
import org.myrobotlab.service.config.InMoov2Config;
import org.myrobotlab.service.config.JMonkeyEngineConfig;
import org.myrobotlab.service.config.MarySpeechConfig;
import org.myrobotlab.service.config.MouthControlConfig;
import org.myrobotlab.service.config.NeoPixelConfig;
import org.myrobotlab.service.config.PidConfig;
import org.myrobotlab.service.config.ProgramABConfig;
import org.myrobotlab.service.config.RandomConfig;
import org.myrobotlab.service.config.RandomConfig.RandomMessageConfig;
import org.myrobotlab.service.config.TrackingConfig;
import org.myrobotlab.service.config.WebkitSpeechRecognitionConfig;
import org.myrobotlab.service.config.WebkitSpeechSynthesisConfig;
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

    ProgramABConfig chatBot = (ProgramABConfig) plan.getPeerConfig("chatBot");
    Runtime runtime = Runtime.getInstance();
    String[] bots = new String[] { "cn-ZH", "en-US", "fi-FI", "hi-IN", "nl-NL", "ru-RU", "de-DE", "es-ES", "fr-FR", "it-IT", "pt-PT", "tr-TR" };
    String tag = runtime.getLocaleTag();
    if (tag != null) {
      String[] tagparts = tag.split("-");
      String lang = tagparts[0];
      for (String b : bots) {
        if (b.startsWith(lang)) {
          chatBot.currentBotName = b;
          break;
        }
      }
    }
    chatBot.textListeners = new String[] { name + ".htmlFilter" };
    chatBot.botDir = "data/ProgramAB";

    HtmlFilterConfig htmlFilter = (HtmlFilterConfig) plan.getPeerConfig("htmlFilter");
    htmlFilter.textListeners = new String[] { name + ".mouth" };

    // FIXME - turns out subscriptions like this are not needed if they are in
    // onStarted
    // == Peer - mouth =============================
    // setup name references to different services
    MarySpeechConfig mouth = (MarySpeechConfig) plan.getPeerConfig("mouth");
    mouth.speechRecognizers = new String[] { name + ".ear" };

    // == Peer - ear =============================
    // setup name references to different services
    WebkitSpeechRecognitionConfig ear = (WebkitSpeechRecognitionConfig) plan.getPeerConfig("ear");
    ear.textListeners = new String[] { name + ".chatBot" };

    JMonkeyEngineConfig simulator = (JMonkeyEngineConfig) plan.getPeerConfig("simulator");
    // FIXME - SHOULD USE RESOURCE DIR !
    String assestsDir = Service.getResourceDir(InMoov2.class) + "/JMonkeyEngine";
    simulator.modelPaths.add(assestsDir);

    simulator.multiMapped.put(name + ".leftHand.index", new String[] { name + ".leftHand.index", name + ".leftHand.index2", name + ".leftHand.index3" });
    simulator.multiMapped.put(name + ".leftHand.majeure", new String[] { name + ".leftHand.majeure", name + ".leftHand.majeure2", name + ".leftHand.majeure3" });
    simulator.multiMapped.put(name + ".leftHand.pinky", new String[] { name + ".leftHand.pinky", name + ".leftHand.pinky2", name + ".leftHand.pinky3" });
    simulator.multiMapped.put(name + ".leftHand.ringFinger", new String[] { name + ".leftHand.ringFinger", name + ".leftHand.ringFinger2", name + ".leftHand.ringFinger3" });
    simulator.multiMapped.put(name + ".leftHand.thumb", new String[] { name + ".leftHand.thumb1", name + ".leftHand.thumb2", name + ".leftHand.thumb3" });

    simulator.multiMapped.put(name + ".rightHand.index", new String[] { name + ".rightHand.index", name + ".rightHand.index2", name + ".rightHand.index3" });
    simulator.multiMapped.put(name + ".rightHand.majeure", new String[] { name + ".rightHand.majeure", name + ".rightHand.majeure2", name + ".rightHand.majeure3" });
    simulator.multiMapped.put(name + ".rightHand.pinky", new String[] { name + ".rightHand.pinky", name + ".rightHand.pinky2", name + ".rightHand.pinky3" });
    simulator.multiMapped.put(name + ".rightHand.ringFinger", new String[] { name + ".rightHand.ringFinger", name + ".rightHand.ringFinger2", name + ".rightHand.ringFinger3" });
    simulator.multiMapped.put(name + ".rightHand.thumb", new String[] { name + ".rightHand.thumb1", name + ".rightHand.thumb2", name + ".rightHand.thumb3" });

    // simulator.nodes.put("camera", new UserData());
    simulator.nodes.put(name + ".head.jaw", new UserData(new MapperLinear(0.0, 180.0, -5.0, 80.0, true, false), "x"));
    simulator.nodes.put(name + ".head.neck", new UserData(new MapperLinear(0.0, 180.0, 20.0, -20.0, true, false), "x"));
    simulator.nodes.put(name + ".head.rothead", new UserData(null, "y"));
    simulator.nodes.put(name + ".head.rollNeck", new UserData(new MapperLinear(0.0, 180.0, 30.0, -30.0, true, false), "z"));
    simulator.nodes.put(name + ".head.eyeY", new UserData(new MapperLinear(0.0, 180.0, 40.0, 140.0, true, false), "x"));
    simulator.nodes.put(name + ".head.eyeX", new UserData(new MapperLinear(0.0, 180.0, -10.0, 70.0, true, false), "y"));
    simulator.nodes.put(name + ".torso.topStom", new UserData(new MapperLinear(0.0, 180.0, -30.0, 30.0, true, false), "z"));
    simulator.nodes.put(name + ".torso.midStom", new UserData(new MapperLinear(0.0, 180.0, 50.0, 130.0, true, false), "y"));
    simulator.nodes.put(name + ".torso.lowStom", new UserData(new MapperLinear(0.0, 180.0, -30.0, 30.0, true, false), "x"));
    simulator.nodes.put(name + ".rightArm.bicep", new UserData(new MapperLinear(0.0, 180.0, 0.0, -150.0, true, false), "x"));
    simulator.nodes.put(name + ".leftArm.bicep", new UserData(new MapperLinear(0.0, 180.0, 0.0, -150.0, true, false), "x"));
    simulator.nodes.put(name + ".rightArm.shoulder", new UserData(new MapperLinear(0.0, 180.0, 30.0, -150.0, true, false), "x"));
    simulator.nodes.put(name + ".leftArm.shoulder", new UserData(new MapperLinear(0.0, 180.0, 30.0, -150.0, true, false), "x"));
    simulator.nodes.put(name + ".rightArm.rotate", new UserData(new MapperLinear(0.0, 180.0, 80.0, -80.0, true, false), "y"));
    simulator.nodes.put(name + ".leftArm.rotate", new UserData(new MapperLinear(0.0, 180.0, -80.0, 80.0, true, false), "y"));
    simulator.nodes.put(name + ".rightArm.omoplate", new UserData(new MapperLinear(0.0, 180.0, 10.0, -180.0, true, false), "z"));
    simulator.nodes.put(name + ".leftArm.omoplate", new UserData(new MapperLinear(0.0, 180.0, -10.0, 180.0, true, false), "z"));
    simulator.nodes.put(name + ".rightHand.wrist", new UserData(new MapperLinear(0.0, 180.0, -20.0, 60.0, true, false), "y"));
    simulator.nodes.put(name + ".leftHand.wrist", new UserData(new MapperLinear(0.0, 180.0, 20.0, -60.0, true, false), "y"));
    simulator.nodes.put(name + ".leftHand.thumb1", new UserData(new MapperLinear(0.0, 180.0, -30.0, -100.0, true, false), "y"));
    simulator.nodes.put(name + ".leftHand.thumb2", new UserData(new MapperLinear(0.0, 180.0, 80.0, 20.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.thumb3", new UserData(new MapperLinear(0.0, 180.0, 80.0, 20.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.index", new UserData(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.index2", new UserData(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.index3", new UserData(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.majeure", new UserData(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.majeure2", new UserData(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.majeure3", new UserData(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.ringFinger", new UserData(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.ringFinger2", new UserData(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.ringFinger3", new UserData(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.pinky", new UserData(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.pinky2", new UserData(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.pinky3", new UserData(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.thumb1", new UserData(new MapperLinear(0.0, 180.0, 30.0, 110.0, true, false), "y"));
    simulator.nodes.put(name + ".rightHand.thumb2", new UserData(new MapperLinear(0.0, 180.0, -100.0, -150.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.thumb3", new UserData(new MapperLinear(0.0, 180.0, -100.0, -160.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.index", new UserData(new MapperLinear(0.0, 180.0, 65.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.index2", new UserData(new MapperLinear(0.0, 180.0, 70.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.index3", new UserData(new MapperLinear(0.0, 180.0, 70.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.majeure", new UserData(new MapperLinear(0.0, 180.0, 65.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.majeure2", new UserData(new MapperLinear(0.0, 180.0, 70.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.majeure3", new UserData(new MapperLinear(0.0, 180.0, 70.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.ringFinger", new UserData(new MapperLinear(0.0, 180.0, 65.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.ringFinger2", new UserData(new MapperLinear(0.0, 180.0, 70.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.ringFinger3", new UserData(new MapperLinear(0.0, 180.0, 70.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.pinky", new UserData(new MapperLinear(0.0, 180.0, 65.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.pinky2", new UserData(new MapperLinear(0.0, 180.0, 70.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.pinky3", new UserData(new MapperLinear(0.0, 180.0, 60.0, -10.0, true, false), "x"));
    simulator.cameraLookAt = name + ".torso.lowStom";

    FiniteStateMachineConfig fsm = (FiniteStateMachineConfig) plan.getPeerConfig("fsm");
//    fsm.states.add("start"); // fist time
//    fsm.states.add("init"); // fist time
//    fsm.states.add("identify_user"); // fist time
//    fsm.states.add("detected_face"); // fist time
//    fsm.states.add("sleeping"); // pir running ? wake word ?
//    fsm.states.add("executing_gesture"); // gesture running
//    fsm.states.add("safe_random_movements"); // random movements
//    fsm.states.add("unsafe_random_movements"); // random movements
//    fsm.states.add("tracking"); // tracking
//    fsm.states.add("power_down"); // process of shutting down stuff

    fsm.transitions.add(new FiniteStateMachineConfig.Transition("start", "first_time", "init"));
    fsm.transitions.add(new FiniteStateMachineConfig.Transition("init", "first_time", "identify_user"));
    fsm.transitions.add(new FiniteStateMachineConfig.Transition("detected_face", "first_time", "identify_user"));

    // == Peer - random =============================
    RandomConfig random = (RandomConfig) plan.getPeerConfig("random");

    // setup name references to different services
    RandomMessageConfig rm = new RandomMessageConfig(3000, 8000, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);
    random.randomMessages.put(name + ".setLeftArmSpeed", rm);

    rm = new RandomMessageConfig(3000, 8000, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);
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
    PidConfig pid = (PidConfig) plan.getPeerConfig("pid");

    PidData tiltPid = new PidData();
    tiltPid.ki = 0.001;
    tiltPid.kp = 30.0;
    pid.data.put(headTracking.tilt, tiltPid);

    PidData panPid = new PidData();
    panPid.ki = 0.001;
    panPid.kp = 15.0;
    pid.data.put(headTracking.pan, panPid);

    PidData eyeTiltPid = new PidData();
    eyeTiltPid.ki = 0.001;
    eyeTiltPid.kp = 10.0;
    pid.data.put(eyeTracking.tilt, eyeTiltPid);

    PidData eyePanPid = new PidData();
    eyePanPid.ki = 0.001;
    eyePanPid.kp = 10.0;
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
