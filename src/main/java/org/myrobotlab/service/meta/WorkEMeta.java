package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.WorkEConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WorkEMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WorkEMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public WorkEMeta() {

    addPeer("git", "Git", "synching repos");

    // motor control - output
    // addPeer("joystick", "Joystick", "a way to steer the bot");
    addPeer("controller", "Sabertooth", "power motor controller for wheels");
    addPeer("motorLeft", "MotorPort", "left wheel motor");
    addPeer("motorRight", "MotorPort", "right wheel motor");

    addPeer("joystick", "Joystick", "for manual control");
    
    // global simulator
    addPeer("simulator", "simulator", "JMonkeyEngine", "a simulator, when the hardware isn't worky");

    // global python
    addPeer("python", "python", "Python", "the python programming interface");

    
    // global webgui
    addPeer("webgui", "webgui", "WebGui", "web interface");

    // vision - input
    // TODO - going to have several "spouts" - and bolts (storm analogy)
    addPeer("eye", "OpenCV", "an eye to see with");// webcam spout
    // addPeer("leftFoscam ", "OpenCV", "computer vision");// webcam spout

    // speech - output
    addPeer("mouth", "Polly", "a mouth to speak with");

    // ear - input
    addPeer("ear", "WebkitSpeechRecognition", "a ear to hear with");

    // brain - input/output
    addPeer("brain", "ProgramAB", "a brain to think with");

    // emoji - output
    addPeer("emoji", "Emoji", "emotional state machine");

    addDependency("org.myrobotlab", "worke", null, "zip");

    addDescription("the worke bot !");
    addCategory("robot");

  }
  
  @Override
  public Plan getDefault(String name) {

    WorkEConfig worke = new WorkEConfig();
    
    Plan plan = new Plan(name);
    // load default peers from meta here
    plan.putPeers(name, peers);
    worke.autoStartPeers = false;
    plan.addConfig(worke);
    
    return plan;
  }
}
