package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WorkEMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(WorkEMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.WorkE");
    Platform platform = Platform.getLocalInstance();
    meta.addPeer("git", "Git", "synching repos");

    // motor control - output
    meta.addPeer("joystick", "Joystick", "joystick control");
    meta.addPeer("controller", "Sabertooth", "motor controller");
    meta.addPeer("motorLeft", "MotorPort", "left motor");
    meta.addPeer("motorRight", "MotorPort", "right motor");
    
    // global simulator
    meta.addPeer("simulator", "simulator", "JMonkeyEngine", "the simulator");

    // global python
    meta.addPeer("python", "python", "Python", "the python interface");
    
    // global webgui
    meta.addPeer("webgui", "webgui", "WebGui", "web interface");

    // vision - input
    // TODO - going to have several "spouts" - and bolts (storm analogy)
    meta.addPeer("eye", "OpenCV", "computer vision");// webcam spout
    // meta.addPeer("leftFoscam ", "OpenCV", "computer vision");// webcam spout

    // speech - output
    meta.addPeer("mouth", "Polly", "mouth");

    // ear - input
    meta.addPeer("ear", "WebkitSpeechRecognition", "ear");

    // brain - input/output
    meta.addPeer("brain", "ProgramAB", "ear");

    // emoji - output
    meta.addPeer("emoji", "Emoji", "emotional state machine");

    meta.addDescription("the worke bot !");
    meta.addCategory("robot");
    return meta;
  }
  
  
}

