package org.myrobotlab.service.config;

import org.myrobotlab.framework.Peer;
import org.myrobotlab.framework.Plan;
import org.myrobotlab.framework.Service;
import org.myrobotlab.jme3.UserData;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.service.InMoov2;
import org.myrobotlab.service.Pid.PidData;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.RandomConfig.RandomMessageConfig;

public class InMoov2Config extends ServiceConfig {

  public boolean pirWakeUp = false;

  public boolean pirEnableTracking = false;

  public boolean loadGestures = true;

  public boolean virtual = false;

  public String locale = "en-US";

  /**
   * startup and shutdown will pause inmoov - set the speed to this value then
   * attempt to move to rest
   */
  public double shutdownStartupSpeed = 50;

  public boolean heartbeat = true;

  /**
   * idle time measures the time the fsm is in an idle state
   */
  public boolean idleTimer = true;
  

  public InMoov2Config() {
  }

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    // peers FIXME global opencv
    addDefaultPeerConfig(plan, name, "audioPlayer", "AudioFile", true);
    addDefaultPeerConfig(plan, name, "chatBot", "ProgramAB", false);
    addDefaultPeerConfig(plan, name, "controller3", "Arduino", false);
    addDefaultPeerConfig(plan, name, "controller4", "Arduino", false);
    addDefaultPeerConfig(plan, name, "ear", "WebkitSpeechRecognition", false);
    addDefaultPeerConfig(plan, name, "eyeTracking", "Tracking", false);
    addDefaultPeerConfig(plan, name, "fsm", "FiniteStateMachine", false);
    addDefaultPeerConfig(plan, name, "head", "InMoov2Head", false);
    addDefaultPeerConfig(plan, name, "headTracking", "Tracking", false);
    addDefaultPeerConfig(plan, name, "htmlFilter", "HtmlFilter", false);
    addDefaultPeerConfig(plan, name, "imageDisplay", "ImageDisplay", false);
    addDefaultPeerConfig(plan, name, "leap", "LeapMotion", false);
    addDefaultPeerConfig(plan, name, "left", "Arduino", false);
    addDefaultPeerConfig(plan, name, "leftArm", "InMoov2Aram", false);
    addDefaultPeerConfig(plan, name, "leftHand", "InMoov2Hand", false);
    addDefaultPeerConfig(plan, name, "mouth", "MarySpeech", false);
    addDefaultPeerConfig(plan, name, "mouthControl", "MouthControl", false);
    addDefaultPeerConfig(plan, name, "neoPixel", "NeoPixel", false);
    addDefaultPeerConfig(plan, name, "opencv", "OpenCV", false);
    addDefaultPeerConfig(plan, name, "openWeatherMap", "OpenWeatherMap", false);
    addDefaultPeerConfig(plan, name, "pid", "Pid", false);
    addDefaultPeerConfig(plan, name, "pir", "Pir", false);
    addDefaultPeerConfig(plan, name, "random", "Random", false);
    addDefaultPeerConfig(plan, name, "right", "Arduino", false);
    addDefaultPeerConfig(plan, name, "rightArm", "InMoov2Arm", false);
    addDefaultPeerConfig(plan, name, "rightHand", "InMoov2Hand", false);
    addDefaultPeerConfig(plan, name, "servoMixer", "ServoMixer", false);
    addDefaultPeerConfig(plan, name, "simulator", "JMonkeyEngine", false);
    addDefaultPeerConfig(plan, name, "torso", "InMoov2Torso", false);
    addDefaultPeerConfig(plan, name, "ultrasonicRight", "UltrasonicSensor", false);
    addDefaultPeerConfig(plan, name, "ultrasonicLeft", "UltrasonicSensor", false);

    MouthControlConfig mouthControl = (MouthControlConfig) plan.get(getPeerName("mouthControl"));

    // setup name references to different services
    mouthControl.jaw = name + ".jaw";
    String i01Name = name;
    int index = name.indexOf(".");
    if (index > 0) {
      i01Name = name.substring(0, name.indexOf("."));
    }

    mouthControl.mouth = i01Name + ".mouth";

    // FIXME ! - look at this !!! I've made austartPeers = false !
    // by just sending a runtime that starts only i01
    RuntimeConfig rtConfig = (RuntimeConfig) plan.get("runtime");

    ProgramABConfig chatBot = (ProgramABConfig) plan.get(getPeerName("chatBot"));
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

    HtmlFilterConfig htmlFilter = (HtmlFilterConfig) plan.get(getPeerName("htmlFilter"));
    htmlFilter.textListeners = new String[] { name + ".mouth" };

    // FIXME - turns out subscriptions like this are not needed if they are in
    // onStarted
    // == Peer - mouth =============================
    // setup name references to different services
    MarySpeechConfig mouth = (MarySpeechConfig) plan.get(getPeerName("mouth"));
    mouth.speechRecognizers = new String[] { name + ".ear" };

    // == Peer - ear =============================
    // setup name references to different services
    WebkitSpeechRecognitionConfig ear = (WebkitSpeechRecognitionConfig) plan.get(getPeerName("ear"));
    ear.textListeners = new String[] { name + ".chatBot" };

    JMonkeyEngineConfig simulator = (JMonkeyEngineConfig) plan.get(getPeerName("simulator"));
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

    FiniteStateMachineConfig fsm = (FiniteStateMachineConfig) plan.get(getPeerName("fsm"));
    // fsm.states.add("start"); // fist time
    // fsm.states.add("init"); // fist time
    // fsm.states.add("identify_user"); // fist time
    // fsm.states.add("detected_face"); // fist time
    // fsm.states.add("sleeping"); // pir running ? wake word ?
    // fsm.states.add("executing_gesture"); // gesture running
    // fsm.states.add("safe_random_movements"); // random movements
    // fsm.states.add("unsafe_random_movements"); // random movements
    // fsm.states.add("tracking"); // tracking
    // fsm.states.add("power_down"); // process of shutting down stuff

    // fsm.transitions.add(new FiniteStateMachineConfig.Transition("start",
    // "first_time", "init"));
    // fsm.transitions.add(new FiniteStateMachineConfig.Transition("init",
    // "first_time", "identify_user"));
    // fsm.transitions.add(new
    // FiniteStateMachineConfig.Transition("detected_face", "first_time",
    // "identify_user"));

    // == Peer - random =============================
    RandomConfig random = (RandomConfig) plan.get(getPeerName("random"));

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
    TrackingConfig headTracking = (TrackingConfig) plan.get(getPeerName("headTracking"));

    // setup name references to different services
    headTracking.getPeer("tilt").name = name + ".head.neck";
    headTracking.getPeer("pan").name = name + ".head.rothead";
    headTracking.getPeer("cv").name = name + ".opencv";
    headTracking.getPeer("pid").name = name + ".pid";
    headTracking.getPeer("controller").name = name + ".left";

    // == Peer - eyeTracking =============================
    TrackingConfig eyeTracking = (TrackingConfig) plan.get(getPeerName("eyeTracking"));

    // setup name references to different services    
    eyeTracking.getPeer("tilt").name = name + ".head.eyeY";
    eyeTracking.getPeer("pan").name = name + ".head.eyeX";
    eyeTracking.getPeer("cv").name = name + ".opencv";
    eyeTracking.getPeer("pid").name = name + ".pid";
    eyeTracking.getPeer("controller").name = name + ".left";

    // == Peer - pid =============================
    PidConfig pid = (PidConfig) plan.get(getPeerName("pid"));

    PidData tiltPid = new PidData();
    tiltPid.ki = 0.001;
    tiltPid.kp = 30.0;
    pid.data.put(headTracking.getPeer("tilt").name, tiltPid);

    PidData panPid = new PidData();
    panPid.ki = 0.001;
    panPid.kp = 15.0;
    pid.data.put(headTracking.getPeer("pan").name, panPid);

    PidData eyeTiltPid = new PidData();
    eyeTiltPid.ki = 0.001;
    eyeTiltPid.kp = 10.0;
    pid.data.put(eyeTracking.getPeer("tilt").name, eyeTiltPid);

    PidData eyePanPid = new PidData();
    eyePanPid.ki = 0.001;
    eyePanPid.kp = 10.0;
    pid.data.put(eyeTracking.getPeer("pan").name, eyePanPid);

    NeoPixelConfig neoPixel = (NeoPixelConfig) plan.get(getPeerName("neoPixel"));
    neoPixel.pin = 2;
    neoPixel.controller = String.format("%s.left", name);
    neoPixel.red = 12;
    neoPixel.green = 180;
    neoPixel.blue = 212;
    neoPixel.pixelCount = 16;
    neoPixel.currentAnimation = "Ironman";

    // remove undesired defaults from our default
    plan.remove(name + ".headTracking.tilt");
    plan.remove(name + ".headTracking.pan");
    plan.remove(name + ".headTracking.pid");
    plan.remove(name + ".headTracking.controller");
    plan.remove(name + ".headTracking.controller.serial");
    plan.remove(name + ".headTracking.cv");

    plan.remove(name + ".eyeTracking.tilt");
    plan.remove(name + ".eyeTracking.pan");
    plan.remove(name + ".eyeTracking.pid");
    plan.remove(name + ".eyeTracking.controller");
    plan.remove(name + ".eyeTracking.controller.serial");
    plan.remove(name + ".eyeTracking.cv");

    // REMOVING ALL PEER FROM STARTING ! effectively autoStartPeers = false
    rtConfig.clear();
    rtConfig.add(name); // <-- adding i01 / not needed

    return plan;
  }

}
