package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.JoystickData;
import org.myrobotlab.service.interfaces.JoystickListener;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

public class InMoov2 extends Service implements TextListener, TextPublisher, JoystickListener {

  public final static Logger log = LoggerFactory.getLogger(InMoov2.class);

  private static final long serialVersionUID = 1L;

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(InMoov2.class);
    meta.addDescription("InMoov2 Service");
    meta.addCategory("robot");
    
    
    meta.addPeer("head", "InMoov2Head", "head");
    meta.addPeer("torso", "InMoov2Torso", "torso");
    // meta.addPeer("eyelids", "InMoovEyelids", "eyelids");
    meta.addPeer("leftArm", "InMoov2Arm", "left arm");
    meta.addPeer("leftHand", "InMoov2Hand", "left hand");
    meta.addPeer("rightArm", "InMoov2Arm", "right arm");
    meta.addPeer("rightHand", "InMoov2Hand", "right hand");
    
    return meta;
  }
  

  boolean autoStartBrowser = false;

  transient public SpeechRecognizer ear;
  
  transient public InMoovEyelids eyelids;
  
  transient public Tracking eyesTracking;
 
  transient public InMoov2Head head;
  
  transient public Tracking headTracking;
  
  Long lastPirActivityTime;
 
  transient public InMoov2Arm leftArm;
  
  transient public InMoovHand leftHand;
  
  int maxInactivityTimeSeconds = 120;
  
  transient public SpeechSynthesis mouth;
 
  boolean muted;
  
  transient public Pid pid;
  
  transient public Python python;
  
  transient public InMoov2Arm rightArm;
  
  transient public InMoovHand rightHand;
 
  transient public InMoovTorso torso;
  
  transient public WebGui webgui;
  
  // TODO - refactor into a Simulator interface when more simulators are borgd
  transient public JMonkeyEngine simulator;

 
  public InMoov2(String n, String id) {
    super(n, id);
  }
  
  
  @Override /* local strong type - is to be avoided - use name string */
  public void addTextListener(TextListener service) {
    // CORRECT WAY ! - no direct reference - just use the name in a subscription
    addListener("publishText", service.getName());
  }
  
  public void beginCheckingOnInactivity() {
    beginCheckingOnInactivity(maxInactivityTimeSeconds);
  }

  public void beginCheckingOnInactivity(int maxInactivityTimeSeconds) {
    this.maxInactivityTimeSeconds = maxInactivityTimeSeconds;
    // speakBlocking("power down after %s seconds inactivity is on",
    // this.maxInactivityTimeSeconds);
    log.info("power down after %s seconds inactivity is on", this.maxInactivityTimeSeconds);
    addTask("checkInactivity", 5 * 1000, 0, "checkInactivity");
  }

  public long checkInactivity() {
    // speakBlocking("checking");
    long lastActivityTime = getLastActivityTime();
    long now = System.currentTimeMillis();
    long inactivitySeconds = (now - lastActivityTime) / 1000;
    if (inactivitySeconds > maxInactivityTimeSeconds) {
      // speakBlocking("%d seconds have passed without activity",
      // inactivitySeconds);
      powerDown();
    } else {
      // speakBlocking("%d seconds have passed without activity",
      // inactivitySeconds);
      info("checking checkInactivity - %d seconds have passed without activity", inactivitySeconds);
    }
    return lastActivityTime;
  }
  
  public void disable() {
    if (head != null) {
      head.disable();
    }
    if (rightHand != null) {
      rightHand.disable();
    }
    if (leftHand != null) {
      leftHand.disable();
    }
    if (rightArm != null) {
      rightArm.disable();
    }
    if (leftArm != null) {
      leftArm.disable();
    }
    if (torso != null) {
      torso.disable();
    }
    if (eyelids != null) {
      eyelids.disable();
    }
  }

  public void enable() {
    if (head != null) {
      head.enable();
    }
    if (rightHand != null) {
      rightHand.enable();
    }
    if (leftHand != null) {
      leftHand.enable();
    }
    if (rightArm != null) {
      rightArm.enable();
    }
    if (leftArm != null) {
      leftArm.enable();
    }
    if (torso != null) {
      torso.enable();
    }
    if (eyelids != null) {
      eyelids.enable();
    }
  }

  public void fullSpeed() {
    if (head != null) {
      head.setVelocity(-1.0, -1.0, -1.0, -1.0, -1.0, -1.0);
    }
    if (rightHand != null) {
      rightHand.setVelocity(-1.0, -1.0, -1.0, -1.0, -1.0, -1.0);
    }
    if (leftHand != null) {
      leftHand.setVelocity(-1.0, -1.0, -1.0, -1.0, -1.0, -1.0);
    }
    if (rightArm != null) {
      rightArm.setSpeed(-1.0, -1.0, -1.0, -1.0);
    }
    if (leftArm != null) {
      leftArm.setSpeed(-1.0, -1.0, -1.0, -1.0);
    }
    if (torso != null) {
      torso.setVelocity(-1.0, -1.0, -1.0);
    }
    if (eyelids != null) {
      eyelids.setVelocity(-1.0, -1.0);
    }
  }
  

  /**
   * finds most recent activity
   * 
   * @return the timestamp of the last activity time.
   */
  public long getLastActivityTime() {

    long lastActivityTime = 0;

    if (leftHand != null) {
      lastActivityTime = Math.max(lastActivityTime, leftHand.getLastActivityTime());
    }

    if (leftArm != null) {
      lastActivityTime = Math.max(lastActivityTime, leftArm.getLastActivityTime());
    }

    if (rightHand != null) {
      lastActivityTime = Math.max(lastActivityTime, rightHand.getLastActivityTime());
    }

    if (rightArm != null) {
      lastActivityTime = Math.max(lastActivityTime, rightArm.getLastActivityTime());
    }

    if (head != null) {
      lastActivityTime = Math.max(lastActivityTime, head.getLastActivityTime());
    }

    if (torso != null) {
      lastActivityTime = Math.max(lastActivityTime, torso.getLastActivityTime());
    }
    if (eyelids != null) {
      lastActivityTime = Math.max(lastActivityTime, eyelids.getLastActivityTime());
    }

    if (lastPirActivityTime != null) {
      lastActivityTime = Math.max(lastActivityTime, lastPirActivityTime);
    }

    if (lastActivityTime == 0) {
      error("invalid activity time - anything connected?");
      lastActivityTime = System.currentTimeMillis();
    }

    return lastActivityTime;
  }
  
  public void halfSpeed() {
    if (head != null) {
      head.setVelocity(25.0, 25.0, 25.0, 25.0, -1.0, 25.0);
    }

    if (rightHand != null) {
      rightHand.setVelocity(30.0, 30.0, 30.0, 30.0, 30.0, 30.0);
    }
    if (leftHand != null) {
      leftHand.setVelocity(30.0, 30.0, 30.0, 30.0, 30.0, 30.0);
    }
    if (rightArm != null) {
      rightArm.setSpeed(25.0, 25.0, 25.0, 25.0);
    }
    if (leftArm != null) {
      leftArm.setSpeed(25.0, 25.0, 25.0, 25.0);
    }
    if (torso != null) {
      torso.setVelocity(20.0, 20.0, 20.0);
    }
    if (eyelids != null) {
      eyelids.setVelocity(30.0, 30.0);
    }
  }

  public boolean isMute() {
    return muted;
  }


  @Override
  public void onJoystickInput(JoystickData input) throws Exception {
    // TODO Auto-generated method stub
    
  }

 
  @Override
  public void onText(String text) {
    invoke("publishText", text);
  }

  // TODO FIX/CHECK this, migrate from python land
  public void powerDown() {

    rest();
    purgeTasks(); 
    disable();

    if (ear != null) {
      ear.lockOutAllGrammarExcept("power up");
    }

    python.execMethod("power_down");
  }

  // TODO FIX/CHECK this, migrate from python land
  public void powerUp() {
    enable();
    rest();
    
    if (ear != null) {
      ear.clearLock();
    }

    beginCheckingOnInactivity();

    python.execMethod("power_up");
  }

  /**
   * all published text from InMoov2 - including ProgramAB
   */
  @Override
  public String publishText(String text) {
    return text;
  }
  
  public void rest() {
    log.info("InMoov Native Rest Gesture Called");
    if (head != null) {
      head.rest();
    }
    if (rightHand != null) {
      rightHand.rest();
    }
    if (leftHand != null) {
      leftHand.rest();
    }
    if (rightArm != null) {
      rightArm.rest();
    }
    if (leftArm != null) {
      leftArm.rest();
    }
    if (torso != null) {
      torso.rest();
    }
    if (eyelids != null) {
      eyelids.rest();
    }
  }
  
  public void startAll(String leftPort, String rightPort) throws Exception {
    /*
    startMouth();
    startHead(leftPort);
    startOpenCV();
    startEar();
    startMouthControl(head.jaw, mouth);
    startLeftHand(leftPort);
    startRightHand(rightPort);
    // startEyelids(rightPort);
    startLeftArm(leftPort);
    startRightArm(rightPort);
    startTorso(leftPort);
    startHeadTracking();
    startEyesTracking();
    // TODO LP
    speakBlocking("startup sequence completed");
    */
  }
  
  public void startHead()
      {
    // log.warn(InMoov.buildDNA(myKey, serviceClass))
    // speakBlocking(languagePack.get("STARTINGHEAD") + " " + port);
    // ???   SHOULD THERE BE REFERENCES AT ALL ??? ... probably not
    if (head == null) {
      head = (InMoov2Head) startPeer("head");
    }

  }

  public ProgramAB startBrain() {

    try {

      ProgramAB chatBot = (ProgramAB) Runtime.start("brain", "ProgramAB");

      this.attach(chatBot);
      // FIXME - deal with language
      // speakBlocking(languagePack.get("CHATBOTACTIVATED"));
      chatBot.repetitionCount(10);
      chatBot.setPath("InMoov/chatBot");
      // FIXME - deal with language chatBot.startSession("default",
      // getLanguage());
      // reset some parameters to default...
      chatBot.setPredicate("topic", "default");
      chatBot.setPredicate("questionfirstinit", "");
      chatBot.setPredicate("tmpname", "");
      chatBot.setPredicate("null", "");
      // load last user session
      if (!chatBot.getPredicate("name").isEmpty()) {
        if (chatBot.getPredicate("lastUsername").isEmpty() || chatBot.getPredicate("lastUsername").equals("unknown")) {
          chatBot.setPredicate("lastUsername", chatBot.getPredicate("name"));
        }
      }
      chatBot.setPredicate("parameterHowDoYouDo", "");
      try {
        chatBot.savePredicates();
      } catch (IOException e) {
        log.error("saving predicates threw", e);
      }
      // start session based on last recognized person
      if (!chatBot.getPredicate("default", "lastUsername").isEmpty() && !chatBot.getPredicate("default", "lastUsername").equals("unknown")) {
        chatBot.startSession(chatBot.getPredicate("lastUsername"));
      }

      HtmlFilter htmlFilter = (HtmlFilter) Runtime.start("htmlFilter", "HtmlFilter");
      chatBot.addTextListener(htmlFilter);
      htmlFilter.addListener("publishText", getName(), "speak");

      return chatBot;
    } catch (Exception e) {
      error(e);
    }
    return null;
  }
  
  public void startSimulator() throws Exception {

    if (simulator == null) {
      simulator = (JMonkeyEngine)Runtime.start("simulator", "JMonkeyEngine");
    }

    // disable the frustrating servo events ...
    // Servo.eventsEnabledDefault(false);

    // ========== gael's calibrations begin ======================
    simulator.setRotation(getName() + ".head.jaw", "x");
    simulator.setRotation(getName() + ".head.neck", "x");
    simulator.setRotation(getName() + ".head.rothead", "y");
    simulator.setRotation(getName() + ".head.rollNeck", "z");
    simulator.setRotation(getName() + ".head.eyeY", "x");
    simulator.setRotation(getName() + ".head.eyeX", "y");
    simulator.setRotation(getName() + ".torso.topStom", "z");
    simulator.setRotation(getName() + ".torso.midStom", "y");
    simulator.setRotation(getName() + ".torso.lowStom", "x");
    simulator.setRotation(getName() + ".rightArm.bicep", "x");
    simulator.setRotation(getName() + ".leftArm.bicep", "x");
    simulator.setRotation(getName() + ".rightArm.shoulder", "x");
    simulator.setRotation(getName() + ".leftArm.shoulder", "x");
    simulator.setRotation(getName() + ".rightArm.rotate", "y");
    simulator.setRotation(getName() + ".leftArm.rotate", "y");
    simulator.setRotation(getName() + ".rightArm.omoplate", "z");
    simulator.setRotation(getName() + ".leftArm.omoplate", "z");
    simulator.setRotation(getName() + ".rightHand.wrist", "y");
    simulator.setRotation(getName() + ".leftHand.wrist", "y");

    simulator.setMapper(getName() + ".head.jaw", 0, 180, -5, 80);
    simulator.setMapper(getName() + ".head.neck", 0, 180, 20, -20);
    simulator.setMapper(getName() + ".head.rollNeck", 0, 180, 30, -30);
    simulator.setMapper(getName() + ".head.eyeY", 0, 180, 40, 140);
    simulator.setMapper(getName() + ".head.eyeX", 0, 180, -10, 70); // HERE there need to be
                                                     // two eyeX (left and
                                                     // right?)
    simulator.setMapper(getName() + ".rightArm.bicep", 0, 180, 0, -150);
    simulator.setMapper(getName() + ".leftArm.bicep", 0, 180, 0, -150);

    simulator.setMapper(getName() + ".rightArm.shoulder", 0, 180, 30, -150);
    simulator.setMapper(getName() + ".leftArm.shoulder", 0, 180, 30, -150);
    simulator.setMapper(getName() + ".rightArm.rotate", 0, 180, 80, -80);
    simulator.setMapper(getName() + ".leftArm.rotate", 0, 180, -80, 80);
    simulator.setMapper(getName() + ".rightArm.omoplate", 0, 180, 10, -180);
    simulator.setMapper(getName() + ".leftArm.omoplate", 0, 180, -10, 180);

    simulator.setMapper(getName() + ".rightHand.wrist", 0, 180, -20, 60);
    simulator.setMapper(getName() + ".leftHand.wrist", 0, 180, 20, -60);

    simulator.setMapper(getName() + ".torso.topStom", 0, 180, -30, 30);
    simulator.setMapper(getName() + ".torso.midStom", 0, 180, 50, 130);
    simulator.setMapper(getName() + ".torso.lowStom", 0, 180, -30, 30);

    // ========== gael's calibrations end ======================

    // ========== 3 joint finger mapping and attaching begin ===

    // ========== Requires VinMoov5.j3o ========================

    simulator.attach(getName() + ".leftHand.thumb", getName() + ".leftHand.thumb1", getName() + ".leftHand.thumb2", getName() + ".leftHand.thumb3");
    simulator.setRotation(getName() + ".leftHand.thumb1", "y");
    simulator.setRotation(getName() + ".leftHand.thumb2", "x");
    simulator.setRotation(getName() + ".leftHand.thumb3", "x");

    simulator.attach(getName() + ".leftHand.index", getName() + ".leftHand.index", getName() + ".leftHand.index2", getName() + ".leftHand.index3");
    simulator.setRotation(getName() + ".leftHand.index", "x");
    simulator.setRotation(getName() + ".leftHand.index2", "x");
    simulator.setRotation(getName() + ".leftHand.index3", "x");

    simulator.attach(getName() + ".leftHand.majeure", getName() + ".leftHand.majeure", getName() + ".leftHand.majeure2", getName() + ".leftHand.majeure3");
    simulator.setRotation(getName() + ".leftHand.majeure", "x");
    simulator.setRotation(getName() + ".leftHand.majeure2", "x");
    simulator.setRotation(getName() + ".leftHand.majeure3", "x");

    simulator.attach(getName() + ".leftHand.ringFinger", getName() + ".leftHand.ringFinger", getName() + ".leftHand.ringFinger2", getName() + ".leftHand.ringFinger3");
    simulator.setRotation(getName() + ".leftHand.ringFinger", "x");
    simulator.setRotation(getName() + ".leftHand.ringFinger2", "x");
    simulator.setRotation(getName() + ".leftHand.ringFinger3", "x");

    simulator.attach(getName() + ".leftHand.pinky", getName() + ".leftHand.pinky", getName() + ".leftHand.pinky2", getName() + ".leftHand.pinky3");
    simulator.setRotation(getName() + ".leftHand.pinky", "x");
    simulator.setRotation(getName() + ".leftHand.pinky2", "x");
    simulator.setRotation(getName() + ".leftHand.pinky3", "x");

    // left hand mapping complexities of the fingers
    simulator.setMapper(getName() + ".leftHand.index", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.index2", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.index3", 0, 180, -110, -179);

    simulator.setMapper(getName() + ".leftHand.majeure", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.majeure2", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.majeure3", 0, 180, -110, -179);

    simulator.setMapper(getName() + ".leftHand.ringFinger", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.ringFinger2", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.ringFinger3", 0, 180, -110, -179);

    simulator.setMapper(getName() + ".leftHand.pinky", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.pinky2", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.pinky3", 0, 180, -110, -179);

    simulator.setMapper(getName() + ".leftHand.thumb1", 0, 180, -30, -100);
    simulator.setMapper(getName() + ".leftHand.thumb2", 0, 180, 80, 20);
    simulator.setMapper(getName() + ".leftHand.thumb3", 0, 180, 80, 20);

    // right hand

    simulator.attach(getName() + ".rightHand.thumb", getName() + ".rightHand.thumb1", getName() + ".rightHand.thumb2", getName() + ".rightHand.thumb3");
    simulator.setRotation(getName() + ".rightHand.thumb1", "y");
    simulator.setRotation(getName() + ".rightHand.thumb2", "x");
    simulator.setRotation(getName() + ".rightHand.thumb3", "x");

    simulator.attach(getName() + ".rightHand.index", getName() + ".rightHand.index", getName() + ".rightHand.index2", getName() + ".rightHand.index3");
    simulator.setRotation(getName() + ".rightHand.index", "x");
    simulator.setRotation(getName() + ".rightHand.index2", "x");
    simulator.setRotation(getName() + ".rightHand.index3", "x");

    simulator.attach(getName() + ".rightHand.majeure", getName() + ".rightHand.majeure", getName() + ".rightHand.majeure2", getName() + ".rightHand.majeure3");
    simulator.setRotation(getName() + ".rightHand.majeure", "x");
    simulator.setRotation(getName() + ".rightHand.majeure2", "x");
    simulator.setRotation(getName() + ".rightHand.majeure3", "x");

    simulator.attach(getName() + ".rightHand.ringFinger", getName() + ".rightHand.ringFinger", getName() + ".rightHand.ringFinger2", getName() + ".rightHand.ringFinger3");
    simulator.setRotation(getName() + ".rightHand.ringFinger", "x");
    simulator.setRotation(getName() + ".rightHand.ringFinger2", "x");
    simulator.setRotation(getName() + ".rightHand.ringFinger3", "x");

    simulator.attach(getName() + ".rightHand.pinky", getName() + ".rightHand.pinky", getName() + ".rightHand.pinky2", getName() + ".rightHand.pinky3");
    simulator.setRotation(getName() + ".rightHand.pinky", "x");
    simulator.setRotation(getName() + ".rightHand.pinky2", "x");
    simulator.setRotation(getName() + ".rightHand.pinky3", "x");

    simulator.setMapper(getName() + ".rightHand.index", 0, 180, 65, -10);
    simulator.setMapper(getName() + ".rightHand.index2", 0, 180, 70, -10);
    simulator.setMapper(getName() + ".rightHand.index3", 0, 180, 70, -10);

    simulator.setMapper(getName() + ".rightHand.majeure", 0, 180, 65, -10);
    simulator.setMapper(getName() + ".rightHand.majeure2", 0, 180, 70, -10);
    simulator.setMapper(getName() + ".rightHand.majeure3", 0, 180, 70, -10);

    simulator.setMapper(getName() + ".rightHand.ringFinger", 0, 180, 65, -10);
    simulator.setMapper(getName() + ".rightHand.ringFinger2", 0, 180, 70, -10);
    simulator.setMapper(getName() + ".rightHand.ringFinger3", 0, 180, 70, -10);

    simulator.setMapper(getName() + ".rightHand.pinky", 0, 180, 65, -10);
    simulator.setMapper(getName() + ".rightHand.pinky2", 0, 180, 70, -10);
    simulator.setMapper(getName() + ".rightHand.pinky3", 0, 180, 60, -10);

    simulator.setMapper(getName() + ".rightHand.thumb1", 0, 180, 30, 110);
    simulator.setMapper(getName() + ".rightHand.thumb2", 0, 180, -100, -150);
    simulator.setMapper(getName() + ".rightHand.thumb3", 0, 180, -100, -160);

    // additional experimental mappings
    /*
     * simulator.attach(getName() + ".leftHand.pinky", getName() + ".leftHand.index2");
     * simulator.attach(getName() + ".leftHand.thumb", getName() + ".leftHand.index3");
     * simulator.setRotation(getName() + ".leftHand.index2", "x");
     * simulator.setRotation(getName() + ".leftHand.index3", "x");
     * simulator.setMapper(getName() + ".leftHand.index", 0, 180, -90, -270);
     * simulator.setMapper(getName() + ".leftHand.index2", 0, 180, -90, -270);
     * simulator.setMapper(getName() + ".leftHand.index3", 0, 180, -90, -270);
     */


  }

  public void startWebGui() {
    webgui = (WebGui) Runtime.create("webgui", "WebGui");
    webgui.setPort(8887);
    webgui.autoStartBrowser(autoStartBrowser);
    webgui.startService();
  }
  
  public void waitTargetPos() {
    if (head != null)
      head.waitTargetPos();
    if (eyelids != null)
      eyelids.waitTargetPos();
    if (leftArm != null)
      leftArm.waitTargetPos();
    if (rightArm != null)
      rightArm.waitTargetPos();
    if (leftHand != null)
      leftHand.waitTargetPos();
    if (rightHand != null)
      rightHand.waitTargetPos();
    if (torso != null)
      torso.waitTargetPos();
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      InMoov2 i02 = (InMoov2)Runtime.start("i02", "InMoov2");
      
      i02.startWebGui();

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
  
  
}
