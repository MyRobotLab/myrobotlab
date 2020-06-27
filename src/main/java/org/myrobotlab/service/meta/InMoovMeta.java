package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoovMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(InMoovMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.InMoov");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("The InMoov service");
    meta.addCategory("robot");
    // meta.addDependency("inmoov.fr", "1.0.0");
    // meta.addDependency("org.myrobotlab.inmoov", "1.0.0");
    meta.addDependency("inmoov.fr", "inmoov", "1.1.22", "zip");
    meta.addDependency("inmoov.fr", "jm3-model", "1.0.0", "zip");

    // SHARING !!! - modified key / actual name begin -------
    meta.addPeer("head.arduino", "left", "Arduino", "shared left arduino");
    meta.addPeer("torso.arduino", "left", "Arduino", "shared left arduino");

    meta.addPeer("leftArm.arduino", "left", "Arduino", "shared left arduino");
    meta.addPeer("leftHand.arduino", "left", "Arduino", "shared left arduino");
    // eyelidsArduino peer for backward compatibility
    meta.addPeer("eyelidsArduino", "right", "Arduino", "shared right arduino");
    meta.addPeer("rightArm.arduino", "right", "Arduino", "shared right arduino");
    meta.addPeer("rightHand.arduino", "right", "Arduino", "shared right arduino");

    meta.addPeer("eyesTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");
    meta.addPeer("eyesTracking.controller", "left", "Arduino", "shared head Arduino");
    meta.addPeer("eyesTracking.x", "head.eyeX", "Servo", "shared servo");
    meta.addPeer("eyesTracking.y", "head.eyeY", "Servo", "shared servo");
    meta.addPeer("mouthControl.mouth", "mouth", "MarySpeech", "shared Speech");
    meta.addPeer("headTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");
    meta.addPeer("headTracking.controller", "left", "Arduino", "shared head Arduino");
    meta.addPeer("headTracking.x", "head.rothead", "Servo", "shared servo");
    meta.addPeer("headTracking.y", "head.neck", "Servo", "shared servo");

    // SHARING !!! - modified key / actual name end ------

    // Global - undecorated by self name
    meta.addPeer("python", "python", "Python", "shared Python service");

    // put peer definitions in
    meta.addPeer("torso", "InMoovTorso", "torso");
    meta.addPeer("eyelids", "InMoovEyelids", "eyelids");
    meta.addPeer("leftArm", "InMoovArm", "left arm");
    meta.addPeer("leftHand", "InMoovHand", "left hand");
    meta.addPeer("rightArm", "InMoovArm", "right arm");
    meta.addPeer("rightHand", "InMoovHand", "right hand");
    // webkit speech.
    meta.addPeer("ear", "WebkitSpeechRecognition", "InMoov webkit speech recognition service");
    // meta.addPeer("ear", "Sphinx", "InMoov Sphinx speech recognition
    // service");
    meta.addPeer("eyesTracking", "Tracking", "Tracking for the eyes");
    meta.addPeer("head", "InMoovHead", "the head");
    meta.addPeer("headTracking", "Tracking", "Head tracking system");
    meta.addPeer("mouth", "MarySpeech", "InMoov speech service");
    meta.addPeer("mouthControl", "MouthControl", "MouthControl");
    meta.addPeer("openni", "OpenNi", "Kinect service");
    meta.addPeer("pid", "Pid", "Pid service");

    // For VirtualInMoov
    meta.addPeer("jme", "JMonkeyEngine", "Virtual inmoov");
    meta.addPeer("ik3d", "InverseKinematics3D", "Virtual inmoov");

    // For IntegratedMovement
    meta.addPeer("integratedMovement", "IntegratedMovement", "Inverse kinematic type movement");
    return meta;
  }
  
}

