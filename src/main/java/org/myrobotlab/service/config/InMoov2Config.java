package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.jme3.UserDataConfig;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.MapperSimple;
import org.myrobotlab.service.Pid.PidData;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.FiniteStateMachineConfig.Transition;
import org.myrobotlab.service.config.RandomConfig.RandomMessageConfig;

/**
 * InMoov2Config - configuration for InMoov2 service - this is a "default"
 * configuration If its configuration which will directly affect another service
 * the naming pattern should be {peerName}{propertyName} e.g. neoPixelErrorRed
 * 
 * FIXME make a color map that can be overridden
 * 
 * @author GroG
 *
 */
public class InMoov2Config extends ServiceConfig {

  /**
   * When the healthCheck is operating, it will check the battery level. If the
   * battery level is &lt; 5% it will publishFlash with red at regular interval
   */
  public boolean batteryInSystem = false;

  /**
   * enable custom sound map for state changes
   */
  public boolean customSound = false;

  public boolean flashOnErrors = true;

  public boolean flashOnPir = false;

  public boolean forceMicroOnIfSleeping = true;

  public boolean healthCheckActivated = false;

  /**
   * flashes if error has occurred - requires heartbeat
   */
  public boolean healthCheckFlash = false;

  public int healthCheckTimerMs = 60000;

  /**
   * Single heartbeat to drive InMoov2 .. it can check status, healthbeat, and
   * fire events to the FSM. Checks battery level and sends a heartbeat flash on
   * publishHeartbeat and onHeartbeat at a regular interval
   */
  public boolean heartbeat = true;

  /**
   * flashes the neopixel every time a health check is preformed. green == good
   * red == battery &lt; 5%
   */
  public boolean heartbeatFlash = false;

  /**
   * interval heath check processes in milliseconds
   */
  public long heartbeatInterval = 3000;

  public boolean loadAppsScripts = true;

  /**
   * loads all python gesture files in the gesture directory
   */
  public boolean loadGestures = true;

  /**
   * executes all scripts in the init directory on startup
   */
  public boolean loadInitScripts = true;

  /**
   * default to null - allow the OS to set it, unless explicilty set
   */
  public String locale = null;

  public boolean neoPixelDownloadBlue = true;

  public boolean neoPixelErrorRed = true;

  public boolean neoPixelFlashWhenSpeaking = false;

  public boolean openCVFaceRecognizerActivated = true;

  public boolean pirEnableTracking = false;

  public boolean pirOnFlash = true;

  /**
   * play pir sounds when pir switching states sound located in
   * data/InMoov2/sounds/pir-activated.mp3 sound located in
   * data/InMoov2/sounds/pir-deactivated.mp3
   */
  public boolean pirPlaySounds = false;

  public boolean pirWakeUp = true;

  /**
   * If true InMoov will send system events and make a boot report
   */
  public boolean reportOnBoot = true;

  /**
   * script related config - idea is good, but shouldn't be implemented with global scripts
   */
  public boolean robotCanMoveHeadWhileSpeaking = false;

  /**
   * startup and shutdown will pause inmoov - set the speed to this value then
   * attempt to move to rest
   */
  public double shutdownStartupSpeed = 50;

  /**
   * Sleep 5 minutes after last presence detected
   */
  public int sleepTimeoutMs = 300000;

  /**
   * Start sound
   */
  public boolean startupSound = true;

  /**
   * Interval in seconds for a idle state event to fire off. If the fsm is in a
   * state which will allow transitioning, the InMoov2 state will transition to
   * idle. Heartbeat will fire the event.
   */
  public Integer stateIdleInterval = 120;

  /**
   * Interval in seconds for a random state event to fire off. If the fsm is in
   * a state which will allow transitioning, the InMoov2 state will transition
   * to random. Heartbeat will fire the event.
   */
  public Integer stateRandomInterval = 120;

  /**
   * Publish system event when state changes
   */
  public boolean systemEventStateChange = true;

  public int trackingTimeoutMs = 10000;

  public String unlockInsult = "forgive me";

  public boolean virtual = false;
  
  /**
   * Prevent InMoov2.py from executing until ready for release
   */
  public boolean execScript = false;

  public InMoov2Config() {
  }

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    // FIXME define global peers named "python" "webgui" etc...

    // peers FIXME global opencv
    addDefaultPeerConfig(plan, name, "audioPlayer", "AudioFile", true);
    addDefaultPeerConfig(plan, name, "chatBot", "ProgramAB", true);
    addDefaultPeerConfig(plan, name, "controller3", "Arduino", false);
    addDefaultPeerConfig(plan, name, "controller4", "Arduino", false);
    addDefaultPeerConfig(plan, name, "ear", "WebkitSpeechRecognition", false);
    addDefaultPeerConfig(plan, name, "eyeTracking", "Tracking", false);
    addDefaultPeerConfig(plan, name, "fsm", "FiniteStateMachine", false);
    addDefaultGlobalConfig(plan, "log", "log", "Log", true);
    addDefaultPeerConfig(plan, name, "llm", "LLM", false);
    addDefaultPeerConfig(plan, name, "head", "InMoov2Head", false);
    addDefaultPeerConfig(plan, name, "headTracking", "Tracking", false);
    addDefaultPeerConfig(plan, name, "htmlFilter", "HtmlFilter", true);
    addDefaultPeerConfig(plan, name, "imageDisplay", "ImageDisplay", false);
    addDefaultPeerConfig(plan, name, "leap", "LeapMotion", false);
    addDefaultPeerConfig(plan, name, "left", "Arduino", false);
    addDefaultPeerConfig(plan, name, "leftArm", "InMoov2Arm", false);
    addDefaultPeerConfig(plan, name, "leftHand", "InMoov2Hand", false);
    addDefaultPeerConfig(plan, name, "mouth", "MarySpeech", false);
    addDefaultPeerConfig(plan, name, "mouth.audioFile", "AudioFile", false);
    addDefaultPeerConfig(plan, name, "mouthControl", "MouthControl", false);
    addDefaultPeerConfig(plan, name, "neoPixel", "NeoPixel", false);
    addDefaultPeerConfig(plan, name, "opencv", "OpenCV", false);
    addDefaultPeerConfig(plan, name, "openni", "OpenNi", false);
    addDefaultPeerConfig(plan, name, "oakd", "OakD", false);
    addDefaultPeerConfig(plan, name, "openWeatherMap", "OpenWeatherMap", false);
    addDefaultPeerConfig(plan, name, "pid", "Pid", false);
    addDefaultPeerConfig(plan, name, "pir", "Pir", false);
    addDefaultGlobalConfig(plan, "python", "python", "Python");
    addDefaultPeerConfig(plan, name, "py4j", "Py4j", false);
    addDefaultPeerConfig(plan, name, "random", "Random", false);
    addDefaultPeerConfig(plan, name, "right", "Arduino", false);
    addDefaultPeerConfig(plan, name, "rightArm", "InMoov2Arm", false);
    addDefaultPeerConfig(plan, name, "rightHand", "InMoov2Hand", false);
    addDefaultPeerConfig(plan, name, "servoMixer", "ServoMixer", false);
    addDefaultPeerConfig(plan, name, "simulator", "JMonkeyEngine", false);
    addDefaultPeerConfig(plan, name, "torso", "InMoov2Torso", false);
    addDefaultPeerConfig(plan, name, "ultrasonicRight", "UltrasonicSensor", false);
    addDefaultPeerConfig(plan, name, "ultrasonicLeft", "UltrasonicSensor", false);
    addDefaultPeerConfig(plan, name, "vertx", "Vertx", false);
    addDefaultPeerConfig(plan, name, "webxr", "WebXR", false);

    WebXRConfig webxr = (WebXRConfig) plan.get(getPeerName("webxr"));

    Map<String, MapperSimple> map = new HashMap<>();
    MapperSimple mapper = new MapperSimple(-0.5, 0.5, 0, 180);
    map.put("i01.head.neck", mapper);
    webxr.controllerMappings.put("head.orientation.pitch", map);

    map = new HashMap<>();
    mapper = new MapperSimple(-0.5, 0.5, 0, 180);
    map.put("i01.head.rothead", mapper);
    webxr.controllerMappings.put("head.orientation.yaw", map);

    map = new HashMap<>();
    mapper = new MapperSimple(-0.5, 0.5, 0, 180);
    map.put("i01.head.roll", mapper);
    webxr.controllerMappings.put("head.orientation.roll", map);

    ServoMixerConfig servoMixer = (ServoMixerConfig) plan.get(getPeerName("servoMixer"));
    servoMixer.mouth = getPeerName("mouth");

    MouthControlConfig mouthControl = (MouthControlConfig) plan.get(getPeerName("mouthControl"));

    // setup name references to different services
    mouthControl.jaw = name + ".head.jaw";
    String i01Name = name;
    int index = name.indexOf(".");
    if (index > 0) {
      i01Name = name.substring(0, name.indexOf("."));
    }

    mouthControl.mouth = i01Name + ".mouth";
    
    UltrasonicSensorConfig ultrasonicLeft = (UltrasonicSensorConfig) plan.get(getPeerName("ultrasonicLeft"));
    ultrasonicLeft.triggerPin = 64;
    ultrasonicLeft.echoPin = 63;

    UltrasonicSensorConfig ultrasonicRight = (UltrasonicSensorConfig) plan.get(getPeerName("ultrasonicRight"));
    ultrasonicRight.triggerPin = 64;
    ultrasonicRight.echoPin = 63;
    
    
    ProgramABConfig chatBot = (ProgramABConfig) plan.get(getPeerName("chatBot"));

    chatBot.bots.add("resource/ProgramAB/Alice");
    chatBot.bots.add("resource/ProgramAB/Dr.Who");
    chatBot.bots.add("resource/ProgramAB/Ency");
    chatBot.bots.add("resource/ProgramAB/Mr. Turing");
    chatBot.bots.add("resource/ProgramAB/de-DE");
    chatBot.bots.add("resource/ProgramAB/en-US");
    chatBot.bots.add("resource/ProgramAB/es-ES");
    chatBot.bots.add("resource/ProgramAB/fi-FI");
    chatBot.bots.add("resource/ProgramAB/fr-FR");
    chatBot.bots.add("resource/ProgramAB/hi-IN");
    chatBot.bots.add("resource/ProgramAB/it-IT");
    chatBot.bots.add("resource/ProgramAB/nl-NL");
    chatBot.bots.add("resource/ProgramAB/pl-PL");
    chatBot.bots.add("resource/ProgramAB/pt-PT");
    chatBot.bots.add("resource/ProgramAB/ru-RU");
    chatBot.bots.add("resource/ProgramAB/tr-TR");

    Runtime runtime = Runtime.getInstance();
    String[] bots = new String[] { "cn-ZH", "en-US", "fi-FI", "hi-IN", "nl-NL", "pl-PL","ru-RU", "de-DE", "es-ES", "fr-FR", "it-IT", "pt-PT", "tr-TR" };
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

    chatBot.listeners.add(new Listener("publishText", getPeerName("htmlFilter"), "onText"));
    chatBot.listeners.add(new Listener("publishSession", name));

    LLMConfig llm = (LLMConfig) plan.get(getPeerName("llm"));
    llm.listeners.add(new Listener("publishText", name + ".htmlFilter", "onText"));

    HtmlFilterConfig htmlFilter = (HtmlFilterConfig) plan.get(getPeerName("htmlFilter"));
    htmlFilter.listeners.add(new Listener("publishText", name + ".mouth", "onText"));

    // FIXME - turns out subscriptions like this are not needed if they are in
    // onStarted
    // == Peer - mouth =============================
    // setup name references to different services
    MarySpeechConfig mouth = (MarySpeechConfig) plan.get(getPeerName("mouth"));
    mouth.voice = "Mark";
    // == Peer - ear =============================
    // setup name references to different services
    WebkitSpeechRecognitionConfig ear = (WebkitSpeechRecognitionConfig) plan.get(getPeerName("ear"));
    ear.listeners.add(new Listener("publishText", name + ".chatBot", "onText"));
    ear.listening = true;

    JMonkeyEngineConfig simulator = (JMonkeyEngineConfig) plan.get(getPeerName("simulator"));

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
    simulator.nodes.put(name + ".head.jaw", new UserDataConfig(new MapperLinear(0.0, 180.0, -5.0, 80.0, true, false), "x"));
    simulator.nodes.put(name + ".head.neck", new UserDataConfig(new MapperLinear(0.0, 180.0, 20.0, -20.0, true, false), "x"));
    simulator.nodes.put(name + ".head.rothead", new UserDataConfig(null, "y"));
    simulator.nodes.put(name + ".head.rollNeck", new UserDataConfig(new MapperLinear(0.0, 180.0, 30.0, -30.0, true, false), "z"));
    simulator.nodes.put(name + ".head.eyeY", new UserDataConfig(new MapperLinear(0.0, 180.0, 40.0, 140.0, true, false), "x"));
    simulator.nodes.put(name + ".head.eyeX", new UserDataConfig(new MapperLinear(0.0, 180.0, -10.0, 70.0, true, false), "y"));
    simulator.nodes.put(name + ".torso.topStom", new UserDataConfig(new MapperLinear(0.0, 180.0, -30.0, 30.0, true, false), "z"));
    simulator.nodes.put(name + ".torso.midStom", new UserDataConfig(new MapperLinear(0.0, 180.0, 50.0, 130.0, true, false), "y"));
    simulator.nodes.put(name + ".torso.lowStom", new UserDataConfig(new MapperLinear(0.0, 180.0, -30.0, 30.0, true, false), "x"));
    simulator.nodes.put(name + ".rightArm.bicep", new UserDataConfig(new MapperLinear(0.0, 180.0, 0.0, -150.0, true, false), "x"));
    simulator.nodes.put(name + ".leftArm.bicep", new UserDataConfig(new MapperLinear(0.0, 180.0, 0.0, -150.0, true, false), "x"));
    simulator.nodes.put(name + ".rightArm.shoulder", new UserDataConfig(new MapperLinear(0.0, 180.0, 30.0, -150.0, true, false), "x"));
    simulator.nodes.put(name + ".leftArm.shoulder", new UserDataConfig(new MapperLinear(0.0, 180.0, 30.0, -150.0, true, false), "x"));
    simulator.nodes.put(name + ".rightArm.rotate", new UserDataConfig(new MapperLinear(0.0, 180.0, 80.0, -80.0, true, false), "y"));
    simulator.nodes.put(name + ".leftArm.rotate", new UserDataConfig(new MapperLinear(0.0, 180.0, -80.0, 80.0, true, false), "y"));
    simulator.nodes.put(name + ".rightArm.omoplate", new UserDataConfig(new MapperLinear(0.0, 180.0, 10.0, -180.0, true, false), "z"));
    simulator.nodes.put(name + ".leftArm.omoplate", new UserDataConfig(new MapperLinear(0.0, 180.0, -10.0, 180.0, true, false), "z"));
    simulator.nodes.put(name + ".rightHand.wrist", new UserDataConfig(new MapperLinear(0.0, 180.0, -20.0, 60.0, true, false), "y"));
    simulator.nodes.put(name + ".leftHand.wrist", new UserDataConfig(new MapperLinear(0.0, 180.0, 20.0, -60.0, true, false), "y"));
    simulator.nodes.put(name + ".leftHand.thumb1", new UserDataConfig(new MapperLinear(0.0, 180.0, -30.0, -100.0, true, false), "y"));
    simulator.nodes.put(name + ".leftHand.thumb2", new UserDataConfig(new MapperLinear(0.0, 180.0, 80.0, 20.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.thumb3", new UserDataConfig(new MapperLinear(0.0, 180.0, 80.0, 20.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.index", new UserDataConfig(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.index2", new UserDataConfig(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.index3", new UserDataConfig(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.majeure", new UserDataConfig(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.majeure2", new UserDataConfig(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.majeure3", new UserDataConfig(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.ringFinger", new UserDataConfig(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.ringFinger2", new UserDataConfig(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.ringFinger3", new UserDataConfig(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.pinky", new UserDataConfig(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.pinky2", new UserDataConfig(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".leftHand.pinky3", new UserDataConfig(new MapperLinear(0.0, 180.0, -110.0, -179.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.thumb1", new UserDataConfig(new MapperLinear(0.0, 180.0, 30.0, 110.0, true, false), "y"));
    simulator.nodes.put(name + ".rightHand.thumb2", new UserDataConfig(new MapperLinear(0.0, 180.0, -100.0, -150.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.thumb3", new UserDataConfig(new MapperLinear(0.0, 180.0, -100.0, -160.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.index", new UserDataConfig(new MapperLinear(0.0, 180.0, 65.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.index2", new UserDataConfig(new MapperLinear(0.0, 180.0, 70.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.index3", new UserDataConfig(new MapperLinear(0.0, 180.0, 70.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.majeure", new UserDataConfig(new MapperLinear(0.0, 180.0, 65.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.majeure2", new UserDataConfig(new MapperLinear(0.0, 180.0, 70.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.majeure3", new UserDataConfig(new MapperLinear(0.0, 180.0, 70.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.ringFinger", new UserDataConfig(new MapperLinear(0.0, 180.0, 65.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.ringFinger2", new UserDataConfig(new MapperLinear(0.0, 180.0, 70.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.ringFinger3", new UserDataConfig(new MapperLinear(0.0, 180.0, 70.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.pinky", new UserDataConfig(new MapperLinear(0.0, 180.0, 65.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.pinky2", new UserDataConfig(new MapperLinear(0.0, 180.0, 70.0, -10.0, true, false), "x"));
    simulator.nodes.put(name + ".rightHand.pinky3", new UserDataConfig(new MapperLinear(0.0, 180.0, 60.0, -10.0, true, false), "x"));
    simulator.cameraLookAt = name + ".torso.lowStom";

    FiniteStateMachineConfig fsm = (FiniteStateMachineConfig) plan.get(getPeerName("fsm"));
    // TODO - events easily gotten from InMoov data ?? auto callbacks in python
    // if
    // exists ?
    fsm.start = "boot";
    fsm.transitions.add(new Transition("boot", "wake", "wake"));
    // setup, nor sleep should be affected by idle
    fsm.transitions.add(new Transition("setup", "setup_done", "idle"));
    fsm.transitions.add(new Transition("random", "idle", "idle"));
    fsm.transitions.add(new Transition("idle", "sleep", "sleep"));
    fsm.transitions.add(new Transition("idle", "power_down", "power_down"));
    fsm.transitions.add(new Transition("idle", "random", "random"));
    fsm.transitions.add(new Transition("sleep", "wake", "wake"));
    fsm.transitions.add(new Transition("sleep", "power_down", "power_down"));
    fsm.transitions.add(new Transition("wake", "setup", "setup"));
    fsm.transitions.add(new Transition("wake", "idle", "idle"));
    fsm.transitions.add(new Transition("idle", "setup", "setup"));
    // power_down to shutdown
    // fsm.transitions.add(new Transition("systemCheck", "systemCheckFinished",
    // "awake"));
    // fsm.transitions.add(new Transition("awake", "sleep", "sleeping"));

    PirConfig pir = (PirConfig) plan.get(getPeerName("pir"));
    pir.pin = "D23";
    pir.controller = name + ".left";
    pir.listeners.add(new Listener("publishPirOn", name));
    pir.listeners.add(new Listener("publishPirOff", name));

    // == Peer - random =============================
    RandomConfig random = (RandomConfig) plan.get(getPeerName("random"));
    random.enabled = false;

    // setup name references to different services
    RandomMessageConfig rm = new RandomMessageConfig(name, "setLeftArmSpeed", 3000, 8000, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);
    random.randomMessages.put(name + ".setLeftArmSpeed", rm);

    rm = new RandomMessageConfig(name, "setRightArmSpeed", 3000, 8000, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);
    random.randomMessages.put(name + ".setRightArmSpeed", rm);

    rm = new RandomMessageConfig(name, "moveLeftArm", 000, 8000, 0.0, 5.0, 85.0, 95.0, 25.0, 30.0, 10.0, 15.0);
    random.randomMessages.put(name + ".moveLeftArm", rm);

    rm = new RandomMessageConfig(name, "moveRightArm", 3000, 8000, 0.0, 5.0, 85.0, 95.0, 25.0, 30.0, 10.0, 15.0);
    random.randomMessages.put(name + ".moveRightArm", rm);

    rm = new RandomMessageConfig(name, "setLeftHandSpeed", 3000, 8000, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);
    random.randomMessages.put(name + ".setLeftHandSpeed", rm);

    rm = new RandomMessageConfig(name, "setRightHandSpeed", 3000, 8000, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);
    random.randomMessages.put(name + ".setRightHandSpeed", rm);

    rm = new RandomMessageConfig(name, "moveLeftHand", 3000, 8000, 10.0, 160.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 130.0, 175.0);
    random.randomMessages.put(name + ".moveLeftHand", rm);

    rm = new RandomMessageConfig(name, "moveRightHand", 3000, 8000, 10.0, 160.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 130.0, 175.0);
    random.randomMessages.put(name + ".moveRightHand", rm);

    rm = new RandomMessageConfig(name, "setHeadSpeed", 3000, 8000, 8.0, 20.0, 8.0, 20.0, 8.0, 20.0);
    random.randomMessages.put(name + ".setHeadSpeed", rm);

    rm = new RandomMessageConfig(name, "moveHead", 3000, 8000, 70.0, 110.0, 65.0, 115.0, 70.0, 110.0);
    random.randomMessages.put(name + ".moveHead", rm);

    rm = new RandomMessageConfig(name, "setTorsoSpeed", 3000, 8000, 2.0, 5.0, 2.0, 5.0, 2.0, 5.0);
    random.randomMessages.put(name + ".setTorsoSpeed", rm);

    rm = new RandomMessageConfig(name, "moveTorso", 3000, 8000, 85.0, 95.0, 88.0, 93.0, 70.0, 110.0);
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
    tiltPid.ki = -0.001;
    tiltPid.kp = -40.0;
    tiltPid.inverted = true;
    pid.data.put(headTracking.getPeer("tilt").name, tiltPid);

    PidData panPid = new PidData();
    panPid.ki = 0.001;
    panPid.kp = 40.0;
    pid.data.put(headTracking.getPeer("pan").name, panPid);

    PidData eyeTiltPid = new PidData();
    eyeTiltPid.ki = -0.001;
    eyeTiltPid.kp = -30.0;
    eyeTiltPid.inverted = true;
    pid.data.put(eyeTracking.getPeer("tilt").name, eyeTiltPid);

    PidData eyePanPid = new PidData();
    eyePanPid.ki = 0.001;
    eyePanPid.kp = 30.0;
    pid.data.put(eyeTracking.getPeer("pan").name, eyePanPid);

    NeoPixelConfig neoPixel = (NeoPixelConfig) plan.get(getPeerName("neoPixel"));
    neoPixel.pin = 2;
    // neoPixel.controller = String.format("%s.left", name);
    neoPixel.controller = String.format("%s.controller3", name);
    neoPixel.red = 12;
    neoPixel.green = 180;
    neoPixel.blue = 212;
    neoPixel.pixelCount = 16;
    neoPixel.currentAnimation = "Ironman";

    // remove undesired defaults from our default
    // FIXME getPeerName(key) -
    // FIXME REMOVAL !!
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

    plan.remove(name + ".oakd.py4j");

    // InMoov2 --to--> InMoov2 loopbacks
    // allow user to override or extend with python
    listeners.add(new Listener("publishBoot", name));
    // listeners.add(new Listener("publishHeartbeat", name));
    // listeners.add(new Listener("publishConfigFinished", name));

    LogConfig log = (LogConfig) plan.get(getPeerName("log"));
    log.level = "warn";
    log.listeners.add(new Listener("publishErrors", name));
    // service --to--> InMoov2

    // InMoov2 --to--> service
    listeners.add(new Listener("publishEvent", getPeerName("chatBot"), "getResponse"));
    listeners.add(new Listener("publishFlash", getPeerName("neoPixel")));
    listeners.add(new Listener("publishPlayAudioFile", getPeerName("audioPlayer")));
    listeners.add(new Listener("publishPlayAnimation", getPeerName("neoPixel")));
    listeners.add(new Listener("publishStopAnimation", getPeerName("neoPixel")));
    // listeners.add(new Listener("publishProcessMessage",
    // getPeerName("python"), "onPythonMessage"));
    listeners.add(new Listener("publishProcessMessage", getPeerName("python"), "onPythonMessage"));
    
    listeners.add(new Listener("publishPython", getPeerName("python")));

    // InMoov2 --to--> InMoov2
    listeners.add(new Listener("publishMoveHead", getPeerName("head"), "onMove"));
    listeners.add(new Listener("publishMoveRightArm", getPeerName("rightArm"), "onMove"));
    listeners.add(new Listener("publishMoveLeftArm", getPeerName("leftArm"), "onMove"));
    listeners.add(new Listener("publishMoveRightHand", getPeerName("rightHand"), "onMove"));
    listeners.add(new Listener("publishMoveLeftHand", getPeerName("leftHand"), "onMove"));
    listeners.add(new Listener("publishMoveTorso", getPeerName("torso"), "onMove"));

    // service --to--> InMoov2
    htmlFilter.listeners.add(new Listener("publishText", name));

    OakDConfig oakd = (OakDConfig) plan.get(getPeerName("oakd"));
    oakd.listeners.add(new Listener("publishClassification", name));
    oakd.getPeer("py4j").name = getPeerName("py4j");

    webxr.listeners.add(new Listener("publishJointAngles", name));

    // service --to--> service
    AudioFileConfig mouth_audioFile = (AudioFileConfig) plan.get(getPeerName("mouth.audioFile"));
    mouth_audioFile.listeners.add(new Listener("publishPeak", name));
    mouth_audioFile.listeners.add(new Listener("publishPeak", name + ".head.jaw", "moveTo"));
    mouth_audioFile.peakDelayMs = 150L;
    mouth_audioFile.peakMultiplier = 200.0;
    mouth_audioFile.peakSampleInterval = 2.0;
    mouth_audioFile.publishPeakResetDelayMs = 100L;
        
    // mouth_audioFile.listeners.add(new Listener("publishAudioEnd", name));
    // mouth_audioFile.listeners.add(new Listener("publishAudioStart", name));

    // Needs upcoming pr
    fsm.listeners.add(new Listener("publishStateChange", name, "publishStateChange"));
    
    // peer --to--> peer
    mouth.listeners.add(new Listener("publishStartSpeaking", name));
    mouth.listeners.add(new Listener("publishStartSpeaking", getPeerName("ear")));
    mouth.listeners.add(new Listener("publishEndSpeaking", name));
    mouth.listeners.add(new Listener("publishEndSpeaking", getPeerName("ear")));

    return plan;
  }

}
