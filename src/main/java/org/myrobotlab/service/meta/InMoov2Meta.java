package org.myrobotlab.service.meta;

import java.util.LinkedHashMap;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.InMoov2Config;
import org.myrobotlab.service.config.PidConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.TrackingConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoov2Meta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoov2Meta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param type
   *          n
   * 
   */
  public InMoov2Meta() {

    addDescription("InMoov2 Service");
    addCategory("robot");

    addPeer("mouthControl", "MouthControl");
    addPeer("opencv", "OpenCV");
    addPeer("ultrasonicRight", "UltrasonicSensor");
    addPeer("ultrasonicLeft", "UltrasonicSensor");
    addPeer("pir", "Pir");

    addPeer("servoMixer", "ServoMixer");

    // the two legacy controllers .. :(
    addPeer("left", "Arduino");
    addPeer("right", "Arduino");
    addPeer("controller3", "Arduino");
    addPeer("controller4", "Arduino");

    // FIXME - needed ? 
    addPeer("htmlFilter", "HtmlFilter");

    addPeer("chatBot", "ProgramAB");
    addPeer("simulator", "JMonkeyEngine");

    addPeer("head", "InMoov2Head");
    addPeer("torso", "InMoov2Torso");
    // addPeer("eyelids", "InMoovEyelids", "eyelids");
    addPeer("leftArm", "InMoov2Arm");
    addPeer("leftHand", "InMoov2Hand");
    addPeer("rightArm", "InMoov2Arm");
    addPeer("rightHand", "InMoov2Hand");
    
    addPeer("imageDisplay", "ImageDisplay");
    addPeer("mouth", "MarySpeech");
    addPeer("ear", "WebkitSpeechRecognition");

    addPeer("headTracking", "Tracking");

    addPeer("neopixel", "NeoPixel");

    addPeer("audioPlayer", "AudioFile");
    addPeer("random", "Random");

    addDependency("fr.inmoov", "inmoov2", null, "zip");

  }

  public LinkedHashMap<String, ServiceConfig> getDefault(String name) {

    // FIXME !!! - 
    // Done in config
    // addPeer("headTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");
    // addPeer("headTracking.x", "head.rothead", "Servo", "shared servo");
    // addPeer("headTracking.y", "head.neck", "Servo", "shared servo");
    
    
    LinkedHashMap<String, ServiceConfig> config = new LinkedHashMap<>();
    
    InMoov2Config inmoov = new InMoov2Config();
    config.put(name, inmoov);

    inmoov.pid = name + ".pid";
    PidConfig pid = (PidConfig)MetaData.addPeer(config, inmoov.pid, "Pid");
    
    inmoov.head = name + ".head";
    config.putAll(MetaData.getDefault(inmoov.head, "InMoov2Head"));

    
    inmoov.headTracking = name + ".headTracking";

    // install headTracking defaults
    config.putAll(MetaData.getDefault(inmoov.headTracking, "Tracking"));
    TrackingConfig headTracking = (TrackingConfig) config.get(inmoov.headTracking);
    
    // setup name references to different services
    
    headTracking.tilt = name + ".head.neck";
    headTracking.pan = name + ".head.rothead";
    headTracking.cv = name + ".cv";
    headTracking.pid = name + ".pid";
    
    // remove undesired defaults from our default
    config.remove(name + ".headTracking.tilt");
    config.remove(name + ".headTracking.pan");
    config.remove(name + ".headTracking.controller");
    config.remove(name + ".headTracking.controller.serial");
    config.remove(name + ".headTracking.cv");

    return config;
  }

}
