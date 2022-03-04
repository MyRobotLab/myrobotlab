package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoov2Meta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoov2Meta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public InMoov2Meta(String name) {

    super(name);
    // platform if there are different dependencies based on different platforms
    // Platform platform = Platform.getLocalInstance();
    
    addDescription("InMoov2 Service");
    addCategory("robot");

    addPeer("mouthControl", "mouth", "MarySpeech", "shared Speech");

    // Sensors -----------------
    addPeer("opencv", "OpenCV", "opencv");
    addPeer("ultrasonicRight", "UltrasonicSensor", "measure distance on the right");
    addPeer("ultrasonicLeft", "UltrasonicSensor", "measure distance on the left");
    addPeer("pir", "Pir", "infrared sensor");

    
    addPeer("servoMixer", "ServoMixer", "for making gestures");

    // the two legacy controllers .. :(
    addPeer("left", "Arduino", "legacy controller");
    addPeer("right", "Arduino", "legacy controller");
    addPeer("controller3", "Arduino", "legacy controller");
    addPeer("controller4", "Arduino", "legacy controller");

    addPeer("htmlFilter", "HtmlFilter", "filter speaking html");

    addPeer("chatBot", "ProgramAB", "chatBot");
    addPeer("simulator", "JMonkeyEngine", "simulator");

    addPeer("head", "InMoov2Head", "head");
    addPeer("torso", "InMoov2Torso", "torso");
    // addPeer("eyelids", "InMoovEyelids", "eyelids");
    addPeer("leftArm", "InMoov2Arm", "left arm");
    addPeer("leftHand", "InMoov2Hand", "left hand");
    addPeer("rightArm", "InMoov2Arm", "right arm");
    addPeer("rightHand", "InMoov2Hand", "right hand");
    addPeer("mouthControl", "MouthControl", "MouthControl");
    addPeer("imageDisplay", "ImageDisplay", "image display service");
    addPeer("mouth", "MarySpeech", "InMoov speech service");
    addPeer("ear", "WebkitSpeechRecognition", "InMoov webkit speech recognition service");

    addPeer("headTracking", "Tracking", "Head tracking system");

    addPeer("headTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");
    // sharePeer("headTracking.controller", "left", "Arduino", "shared head
    // Arduino"); NO !!!!
    addPeer("headTracking.x", "head.rothead", "Servo", "shared servo");
    addPeer("headTracking.y", "head.neck", "Servo", "shared servo");

    addPeer("neopixel", "NeoPixel", "neopixel animation");
    
    addPeer("audioPlayer", "AudioFile", "audio file");
  
    
    // Global - undecorated by self name
    // currently InMoov manually calls releasePeers - when it does
    // the interpreter is in a process of shutdown while all inmoov peer
    // services have not
    // run their initialization scripts - npe and other errors can happen when
    // creating and
    // releasing all peers in quick succession
    // addPeer("python", "python", "Python", "shared Python service");

    // latest - not ready until repo is ready
    addDependency("fr.inmoov", "inmoov2", null, "zip");

  }

}
