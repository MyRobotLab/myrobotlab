package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WorkEMeta  extends MetaData {
  private static final long serialVersionUID = 1L;
public final static Logger log = LoggerFactory.getLogger(WorkEMeta.class);
  
  /**
   * This class is contains all the meta data details of a service.
   * It's peers, dependencies, and all other meta data related to the service.
   * 
   */
  public WorkEMeta() {

    
    Platform platform = Platform.getLocalInstance();
   addPeer("git", "Git", "synching repos");

    // motor control - output
   addPeer("joystick", "Joystick", "joystick control");
   addPeer("controller", "Sabertooth", "motor controller");
   addPeer("motorLeft", "MotorPort", "left motor");
   addPeer("motorRight", "MotorPort", "right motor");
    
    // global simulator
   addPeer("simulator", "simulator", "JMonkeyEngine", "the simulator");

    // global python
   addPeer("python", "python", "Python", "the python interface");
    
    // global webgui
   addPeer("webgui", "webgui", "WebGui", "web interface");

    // vision - input
    // TODO - going to have several "spouts" - and bolts (storm analogy)
   addPeer("eye", "OpenCV", "computer vision");// webcam spout
    //addPeer("leftFoscam ", "OpenCV", "computer vision");// webcam spout

    // speech - output
   addPeer("mouth", "Polly", "mouth");

    // ear - input
   addPeer("ear", "WebkitSpeechRecognition", "ear");

    // brain - input/output
   addPeer("brain", "ProgramAB", "ear");

    // emoji - output
   addPeer("emoji", "Emoji", "emotional state machine");

   addDependency("org.myrobotlab", "worke", null, "zip");
    
   addDescription("the worke bot !");
   addCategory("robot");
    
  }
  
  
}

