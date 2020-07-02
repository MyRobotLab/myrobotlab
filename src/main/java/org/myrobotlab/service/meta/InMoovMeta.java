package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoovMeta  extends MetaData {
  private static final long serialVersionUID = 1L;
public final static Logger log = LoggerFactory.getLogger(InMoovMeta.class);
  
  /**
   * This class is contains all the meta data details of a service.
   * It's peers, dependencies, and all other meta data related to the service.
   * 
   */
  public InMoovMeta() {

    
    Platform platform = Platform.getLocalInstance();
    
   addDescription("The InMoov service");
   addCategory("robot");
    //addDependency("inmoov.fr", "1.0.0");
    //addDependency("org.myrobotlab.inmoov", "1.0.0");
   addDependency("inmoov.fr", "inmoov", "1.1.22", "zip");
   addDependency("inmoov.fr", "jm3-model", "1.0.0", "zip");

    // SHARING !!! - modified key / actual name begin -------
   addPeer("head.arduino", "left", "Arduino", "shared left arduino");
   addPeer("torso.arduino", "left", "Arduino", "shared left arduino");

   addPeer("leftArm.arduino", "left", "Arduino", "shared left arduino");
   addPeer("leftHand.arduino", "left", "Arduino", "shared left arduino");
    // eyelidsArduino peer for backward compatibility
   addPeer("eyelidsArduino", "right", "Arduino", "shared right arduino");
   addPeer("rightArm.arduino", "right", "Arduino", "shared right arduino");
   addPeer("rightHand.arduino", "right", "Arduino", "shared right arduino");

   addPeer("eyesTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");
   addPeer("eyesTracking.controller", "left", "Arduino", "shared head Arduino");
   addPeer("eyesTracking.x", "head.eyeX", "Servo", "shared servo");
   addPeer("eyesTracking.y", "head.eyeY", "Servo", "shared servo");
   addPeer("mouthControl.mouth", "mouth", "MarySpeech", "shared Speech");
   addPeer("headTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");
   addPeer("headTracking.controller", "left", "Arduino", "shared head Arduino");
   addPeer("headTracking.x", "head.rothead", "Servo", "shared servo");
   addPeer("headTracking.y", "head.neck", "Servo", "shared servo");

    // SHARING !!! - modified key / actual name end ------

    // Global - undecorated by self name
   addPeer("python", "python", "Python", "shared Python service");

    // put peer definitions in
   addPeer("torso", "InMoovTorso", "torso");
   addPeer("eyelids", "InMoovEyelids", "eyelids");
   addPeer("leftArm", "InMoovArm", "left arm");
   addPeer("leftHand", "InMoovHand", "left hand");
   addPeer("rightArm", "InMoovArm", "right arm");
   addPeer("rightHand", "InMoovHand", "right hand");
    // webkit speech.
   addPeer("ear", "WebkitSpeechRecognition", "InMoov webkit speech recognition service");
    //addPeer("ear", "Sphinx", "InMoov Sphinx speech recognition
    // service");
   addPeer("eyesTracking", "Tracking", "Tracking for the eyes");
   addPeer("head", "InMoovHead", "the head");
   addPeer("headTracking", "Tracking", "Head tracking system");
   addPeer("mouth", "MarySpeech", "InMoov speech service");
   addPeer("mouthControl", "MouthControl", "MouthControl");
   addPeer("openni", "OpenNi", "Kinect service");
   addPeer("pid", "Pid", "Pid service");

    // For VirtualInMoov
   addPeer("jme", "JMonkeyEngine", "Virtual inmoov");
   addPeer("ik3d", "InverseKinematics3D", "Virtual inmoov");

    // For IntegratedMovement
   addPeer("integratedMovement", "IntegratedMovement", "Inverse kinematic type movement");
    
  }
  
}

