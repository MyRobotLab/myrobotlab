package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class InMoov2Meta {
  public final static Logger log = LoggerFactory.getLogger(InMoov2Meta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.InMoov2");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("InMoov2 Service");
    meta.addCategory("robot");

    meta.sharePeer("mouthControl.mouth", "mouth", "MarySpeech", "shared Speech");

        meta.addPeer("opencv", "OpenCV", "opencv");
        meta.addPeer("servomixer", "ServoMixer", "for making gestures");
    meta.addPeer("ultraSonicRight", "UltrasonicSensor", "measure distance");
    meta.addPeer("ultraSonicLeft", "UltrasonicSensor", "measure distance");
    meta.addPeer("pir", "Pir", "infrared sensor");

    // the two legacy controllers .. :(
    meta.addPeer("left", "Arduino", "legacy controller");
    meta.addPeer("right", "Arduino", "legacy controller");
    meta.addPeer("controller3", "Arduino", "legacy controller");
    meta.addPeer("controller4", "Arduino", "legacy controller");

    meta.addPeer("htmlFilter", "HtmlFilter", "filter speaking html");

    meta.addPeer("chatBot", "ProgramAB", "chatBot");
    meta.addPeer("simulator", "JMonkeyEngine", "simulator");

    meta.addPeer("head", "InMoov2Head", "head");
    meta.addPeer("torso", "InMoov2Torso", "torso");
    // meta.addPeer("eyelids", "InMoovEyelids", "eyelids");
    meta.addPeer("leftArm", "InMoov2Arm", "left arm");
    meta.addPeer("leftHand", "InMoov2Hand", "left hand");
    meta.addPeer("rightArm", "InMoov2Arm", "right arm");
    meta.addPeer("rightHand", "InMoov2Hand", "right hand");
    meta.addPeer("mouthControl", "MouthControl", "MouthControl");
    // meta.addPeer("imageDisplay", "ImageDisplay", "image display service");
    meta.addPeer("mouth", "MarySpeech", "InMoov speech service");
    meta.addPeer("ear", "WebkitSpeechRecognition", "InMoov webkit speech recognition service");

    meta.addPeer("headTracking", "Tracking", "Head tracking system");

    meta.sharePeer("headTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");
    // meta.sharePeer("headTracking.controller", "left", "Arduino", "shared head
    // Arduino"); NO !!!!
    meta.sharePeer("headTracking.x", "head.rothead", "Servo", "shared servo");
    meta.sharePeer("headTracking.y", "head.neck", "Servo", "shared servo");

    // Global - undecorated by self name
    meta.addPeer("python", "python", "Python", "shared Python service");

    // latest - not ready until repo is ready
    meta.addDependency("fr.inmoov", "inmoov2", null, "zip");

    return meta;
  }
  
}

