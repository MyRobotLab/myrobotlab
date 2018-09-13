package org.myrobotlab.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.jme3.InMoov3DApp;
import org.myrobotlab.kinematics.DHLinkType;
import org.myrobotlab.kinematics.GravityCenter;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.openni.OpenNiData;
import org.myrobotlab.openni.Skeleton;
import org.myrobotlab.service.Servo.IKData;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

import com.jme3.system.AppSettings;

/**
 * InMoov - The InMoov Service.
 * 
 * The InMoov service allows control of the InMoov robot. This robot was created
 * by Gael Langevin. It's an open source 3D printable robot. All of the parts
 * and instructions to build are on his blog (http://inmoov.blogspot.com/ and
 * http://www.inmoov.fr/). InMoov is a composite of servos, Arduinos,
 * microphone, camera, kinect and computer. The InMoov service is composed of
 * many other peer services, and allows easy initialization and control of these
 * sub systems.
 *
 */
public class InMoov extends Service {

  private static final String GESTURES_DIRECTORY = "gestures";
  public String CALIBRATION_FILE = "calibration.py";

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoov.class);

  // FIXME - startPIR - all other starts of complex composite service need to
  // try to get their Arduino from the Arduino MAP !!! FIRST
  // BEFORE CREATING ONE !!!

  // FIXME - EVERYTHING .. ya EVERYTHING a local top level reference !

  // OBJECTIVE - try only have complex composite interaction here - everything
  // else should be done directly to targeted services !!!
  // OBJECTIVE - always return a service !!!

  // port map NOT SURE ???
  // will no right & left and com ports
  // 3 definitions at the top left right and head
  // port index, local references

  // this is good, because arduino's ultimately are identified by port keys
  // FIXME - should be controllers ServoControllers not Arduinos
  HashMap<String, Arduino> arduinos = new HashMap<String, Arduino>();

  // services which do not require a body part
  // or can influence multiple body parts

  // Dynamic reflective services such as WebGui & Xmpp are to be left out of
  // Peer definitions

  String defaultLeftPort; // FIXME - THIS IS A BUG GET RID OF IT - ALL ACCESS
  // THROUGH MAP !!!
  String defaultRightPort; // FIXME - THIS IS A BUG GET RID OF IT - ALL ACCESS

  // THROUGH MAP !!!
  // FIXME ALL PEERS NEED TO BE PRIVATE - ACCESS THROUGH GETTERS WHICH DO A
  // Runtime.create !
  // hands and arms
  transient public InMoovHead head;
  transient public InMoovTorso torso;
  transient public InMoovArm leftArm;
  transient public InMoovHand leftHand;
  transient public InMoovArm rightArm;
  transient public InMoovHand rightHand;
  transient public InMoovEyelids eyelids;

  transient private HashMap<String, InMoovArm> arms = new HashMap<String, InMoovArm>();
  transient private HashMap<String, InMoovHand> hands = new HashMap<String, InMoovHand>();

  // metas
  transient public SpeechRecognizer ear;
  transient public SpeechSynthesis mouth;
  transient public static Tracking eyesTracking;
  transient public static Tracking headTracking;
  transient public static OpenCV opencv;
  transient public static OpenNi openni;
  transient public MouthControl mouthControl;
  transient public Python python;

  transient public final static String LEFT = "left";
  transient public final static String RIGHT = "right";

  transient public Pid pid;

  boolean copyGesture = false;
  public double openNiShouldersOffset = -50.0;
  public boolean openNiLeftShoulderInverted = true;
  public boolean openNiRightShoulderInverted = true;
  boolean firstSkeleton = true;
  boolean saveSkeletonFrame = false;

  // reflective or non-interactive meta
  // transient public WebGui webgui;
  // transient public Xmpp xmpp;
  // transient public Security security;

  boolean speakErrors = false;
  String lastInMoovError = "";

  // long lastActivityTime;

  int maxInactivityTimeSeconds = 120;

  public String lang_shutDown = "Extinguish my system, please wait 10 seconds";

  //
  private boolean mute = false;

  public static int attachPauseMs = 100;

  // TODO InMoovLife service
  public static boolean RobotIsTrackingSomething() {
    if (eyesTracking != null) {
      if (!eyesTracking.isIdle()) {
        return true;
      }
    }
    if (headTracking != null) {
      if (!headTracking.isIdle()) {
        return true;
      }
    }
    return false;
  }

  public static boolean RobotIsOpenCvCapturing() {
    if (opencv != null) {
      if (opencv.capturing) {
        return true;
      }
    }
    return false;
  }

  public static boolean RobotIsOpenNiCapturing() {
    if (openni != null) {
      if (openni.capturing) {
        return true;
      }
    }
    return false;
  }

  public static boolean RobotCanMoveHeadRandom = true;
  public static boolean RobotCanMoveEyesRandom = true;
  public static boolean RobotCanMoveBodyRandom = true;
  public static boolean RobotCanMoveRandom = true;
  public static boolean RobotIsSleeping = false;
  public static boolean RobotIsStarted = false;

  // END TODO InMoovLife service

  public Integer pirPin = null;

  // ---------- new getter interface begin ---------------------------

  Long startSleep = null;

  // ---------- new getter interface begin ---------------------------

  Long lastPIRActivityTime = null;

  boolean useHeadForTracking = true;

  boolean useEyesForTracking = false;

  transient InMoov3DApp vinMoovApp;

  private IntegratedMovement integratedMovement;
  private Arduino pirArduino;

  static String speechService = "MarySpeech";
  // static String speechService = "AcapelaSpeech";
  //static String speechService = "NaturalReaderSpeech";
  static String speechRecognizer = "WebkitSpeechRecognition";

  public InMoov(String n) {
    super(n);
    // addRoutes();
    // FIXME - mebbe starts with same error - don't say it again unless a
    // certain time has passed

  }

  /**
   * subscribe to the runtime for any new services
   */
  public void addRoutes() {

    Runtime r = Runtime.getInstance();
    subscribe(r.getName(), "registered");

    // handle my own error the same way too
    subscribe(getName(), "publishError");
  }

  public void atEase() {
    if (head != null) {
      head.rest();
    }
    if (rightHand != null) {
      rightHand.rest();
      rightHand.disable();
    }
    if (leftHand != null) {
      leftHand.rest();
      leftHand.disable();
    }
    if (rightArm != null) {
      rightArm.rest();
      rightArm.disable();
    }
    if (leftArm != null) {
      leftArm.rest();
      leftArm.disable();
    }
    if (torso != null) {
      torso.rest();
      torso.disable();
    }
    if (eyelids != null) {
      eyelids.rest();
      eyelids.disable();
    }
  }

  @Deprecated
  public void attach() {
    enable();
  }

  public void enable() {
    if (head != null) {
      head.enable();
    }
    if (rightHand != null) {
      rightHand.enable();
    }
    if (leftHand != null) {
      sleep(attachPauseMs);
      leftHand.enable();
    }
    if (rightArm != null) {
      sleep(attachPauseMs);
      rightArm.enable();
    }
    if (leftArm != null) {
      sleep(attachPauseMs);
      leftArm.enable();
    }
    if (torso != null) {
      torso.enable();
    }
    if (eyelids != null) {
      eyelids.enable();
    }
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

  @Override
  public void broadcastState() {
    if (leftHand != null) {
      leftHand.broadcastState();
    }

    if (rightHand != null) {
      rightHand.broadcastState();
    }

    if (leftArm != null) {
      leftArm.broadcastState();
    }

    if (rightArm != null) {
      rightArm.broadcastState();
    }

    if (head != null) {
      head.broadcastState();
    }

    if (torso != null) {
      torso.broadcastState();
    }

    if (eyelids != null) {
      eyelids.broadcastState();
    }

    if (headTracking != null) {
      headTracking.broadcastState();
    }

    if (eyesTracking != null) {
      eyesTracking.broadcastState();
    }
  }

  public void cameraOff() {
    if (opencv != null) {
      opencv.stopCapture();
    }
  }

  public void cameraOn() throws Exception {
    startOpenCV();
    opencv.capture();
  }

  public String captureGesture() {
    return captureGesture(null);
  }

  public String captureGesture(String gestureName) {
    StringBuffer script = new StringBuffer();
    Date date = new Date();

    String indentSpace = "";
    script.append("# - " + date + " - Captured gesture :\n");

    if (gestureName != null) {
      indentSpace = "  ";
      script.append(String.format("def %s():\n", gestureName));
    }

    if (head != null) {
      script.append(indentSpace);
      script.append(head.getScript(getName()));
    }

    if (leftArm != null) {
      script.append(indentSpace);
      script.append(leftArm.getScript(getName()));
    }
    if (rightArm != null) {
      script.append(indentSpace);
      script.append(rightArm.getScript(getName()));
    }

    if (leftHand != null) {
      script.append(indentSpace);
      script.append(leftHand.getScript(getName()));
    }
    if (rightHand != null) {
      script.append(indentSpace);
      script.append(rightHand.getScript(getName()));
    }

    if (torso != null) {
      script.append(indentSpace);
      script.append(torso.getScript(getName()));
    }

    if (eyelids != null) {
      script.append(indentSpace);
      script.append(eyelids.getScript(getName()));
    }

    send("python", "appendScript", script.toString());

    return script.toString();
  }

  public long checkInactivity() {
    // speakBlocking("checking");
    long lastActivityTime = getLastActivityTime();
    long now = System.currentTimeMillis();
    long inactivitySeconds = (now - lastActivityTime) / 1000;
    if (inactivitySeconds > maxInactivityTimeSeconds && isAttached()) {
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

  public void clearTrackingPoints() {
    if (headTracking == null) {
      error("attach head before tracking");
    } else {
      headTracking.clearTrackingPoints();
    }
  }

  public void closePinch(String which) {
    moveHand(which, 130, 140, 180, 180, 180);
  }

  public boolean copyGesture(boolean b) throws Exception {
    log.info("copyGesture {}", b);
    if (b) {
      if (openni == null) {
        openni = startOpenNI();
      }
      speakBlocking("copying gestures");
      openni.startUserTracking();
    } else {
      speakBlocking("stop copying gestures");
      if (openni != null) {
        openni.stopCapture();
        firstSkeleton = true;
      }
    }

    copyGesture = b;
    return b;
  }

  @Deprecated
  public void detach() {
    disable();
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
      rightArm.setVelocity(-1.0, -1.0, -1.0, -1.0);
    }
    if (leftArm != null) {
      leftArm.setVelocity(-1.0, -1.0, -1.0, -1.0);
    }
    if (torso != null) {
      torso.setVelocity(-1.0, -1.0, -1.0);
    }
    if (eyelids != null) {
      eyelids.setVelocity(-1.0, -1.0);
    }
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
      rightArm.setVelocity(25.0, 25.0, 25.0, 25.0);
    }
    if (leftArm != null) {
      leftArm.setVelocity(25.0, 25.0, 25.0, 25.0);
    }
    if (torso != null) {
      torso.setVelocity(20.0, 20.0, 20.0);
    }
    if (eyelids != null) {
      eyelids.setVelocity(30.0, 30.0);
    }
  }

  String getBoardType(String side, String type) {
    if (type != null) {
      return type;
    }

    if (RIGHT.equals(side)) {
      return Arduino.BOARD_TYPE_MEGA;
    }

    return Arduino.BOARD_TYPE_MEGA;
  }
  // ------ starts end ---------
  // ------ composites begin ---------

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

    if (lastPIRActivityTime != null) {
      lastActivityTime = Math.max(lastActivityTime, lastPIRActivityTime);
    }

    if (lastActivityTime == 0) {
      error("invalid activity time - anything connected?");
      lastActivityTime = System.currentTimeMillis();
    }

    return lastActivityTime;
  }

  public Python getPython() {
    try {
      if (python == null) {
        python = (Python) startPeer("python");
      }

    } catch (Exception e) {
      error(e);
    }
    return python;
  }

  public void handClose(String which) {
    moveHand(which, 180, 180, 180, 180, 180);
  }

  public void onError(String msg) {
    // lets try not to nag
    if (!lastInMoovError.equals(msg) && speakErrors) {
      speakBlocking(msg);
    }
    lastInMoovError = msg;
  }

  public void handOpen(String which) {
    moveHand(which, 0, 0, 0, 0, 0);
  }

  public void handRest(String which) {
    moveHand(which, 60, 40, 40, 40, 40);
  }

  // ------ composites end

  // ------ composites servos begin -----------

  public boolean isAttached() {
    boolean attached = false;
    if (leftHand != null) {
      attached |= leftHand.isAttached();
    }

    if (leftArm != null) {
      attached |= leftArm.isAttached();
    }

    if (rightHand != null) {
      attached |= rightHand.isAttached();
    }

    if (rightArm != null) {
      attached |= rightArm.isAttached();
    }

    if (head != null) {
      attached |= head.isAttached();
    }

    if (torso != null) {
      attached |= torso.isAttached();
    }

    if (eyelids != null) {
      attached |= eyelids.isAttached();
    }
    return attached;
  }

  public boolean isMute() {
    return mute;
  }

  public void moveArm(String which, double bicep, double rotate, double shoulder, double omoplate) {
    if (!arms.containsKey(which)) {
      error("setArmSpeed %s does not exist", which);
    } else {
      arms.get(which).moveTo(bicep, rotate, shoulder, omoplate);
    }
  }

  public void moveEyes(double eyeX, double eyeY) {
    if (head != null) {
      head.moveTo(null, null, eyeX, eyeY, null, null);
    } else {
      log.error("moveEyes - I have a null head");
    }
  }

  public void moveHand(String which, double thumb, double index, double majeure, double ringFinger, double pinky) {
    moveHand(which, thumb, index, majeure, ringFinger, pinky, null);
  }

  public void moveHand(String which, double thumb, double index, double majeure, double ringFinger, double pinky, Double wrist) {
    if (!hands.containsKey(which)) {
      error("moveHand %s does not exist", which);
    } else {
      hands.get(which).moveTo(thumb, index, majeure, ringFinger, pinky, wrist);
    }
  }

  // ------ composites servos end ----------- 

  public void moveHead(double neck, double rothead) {
    if (head != null) {
      head.moveTo(neck, rothead);
    } else {
      log.error("moveHead - I have a null head");
    }
  }

  public void moveHead(double neck, double rothead, double rollNeck) {
    if (head != null) {
      head.moveTo(neck, rothead, rollNeck);
    } else {
      log.error("moveHead - I have a null head");
    }
  }

  public void moveHead(double neck, double rothead, double eyeX, double eyeY, double jaw) {
    if (head != null) {
      head.moveTo(neck, rothead, eyeX, eyeY, jaw);
    } else {
      log.error("I have a null head");
    }
  }

  public void moveHead(double neck, double rothead, double eyeX, double eyeY, double jaw, double rollNeck) {
    if (head != null) {
      head.moveTo(neck, rothead, eyeX, eyeY, jaw, rollNeck);
    } else {
      log.error("I have a null head");
    }
  }

  // ---------- canned gestures end ---------

  public void moveTorso(double topStom, double midStom, double lowStom) {
    if (torso != null) {
      torso.moveTo(topStom, midStom, lowStom);
    } else {
      log.error("moveTorso - I have a null torso");
    }
  }

  public void moveTorsoBlocking(double topStom, double midStom, double lowStom) {
    if (torso != null) {
      torso.moveToBlocking(topStom, midStom, lowStom);
    } else {
      log.error("moveTorsoBlocking - I have a null torso");
    }
  }

  public void moveEyelids(double eyelidleft, double eyelidright) {
    if (eyelids != null) {
      eyelids.moveTo(eyelidleft, eyelidright);
    } else {
      log.error("moveEyelids - I have a null Eyelids");
    }
  }

  public void moveHeadBlocking(double neck, double rothead) {
    if (head != null) {
      head.moveToBlocking(neck, rothead);
    } else {
      log.error("moveHead - I have a null head");
    }
  }

  public void moveHeadBlocking(double neck, double rothead, double rollNeck) {
    if (head != null) {
      head.moveToBlocking(neck, rothead, rollNeck);
    } else {
      log.error("moveHead - I have a null head");
    }
  }

  public void moveHeadBlocking(double neck, double rothead, double eyeX, double eyeY, double jaw) {
    if (head != null) {
      head.moveToBlocking(neck, rothead, eyeX, eyeY, jaw);
    } else {
      log.error("I have a null head");
    }
  }

  public void moveHeadBlocking(double neck, double rothead, double eyeX, double eyeY, double jaw, double rollNeck) {
    if (head != null) {
      head.moveToBlocking(neck, rothead, eyeX, eyeY, jaw, rollNeck);
    } else {
      log.error("I have a null head");
    }
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

  public void onOpenNIData(OpenNiData data) {

    if (data != null) {
      Skeleton skeleton = data.skeleton;

      if (firstSkeleton) {
        speakBlocking("i see you");
        firstSkeleton = false;
      }

      if (copyGesture) {

        if (leftArm != null) {

          if (!Double.isNaN(skeleton.leftElbow.getAngleXY())) {
            if (skeleton.leftElbow.getAngleXY() >= 0) {
              leftArm.bicep.moveTo(skeleton.leftElbow.getAngleXY());
            }
          }
          if (!Double.isNaN(skeleton.leftShoulder.getAngleXY())) {
            if (skeleton.leftShoulder.getAngleXY() >= 0) {
              leftArm.omoplate.moveTo(skeleton.leftShoulder.getAngleXY());
            }
          }
          if (!Double.isNaN(skeleton.leftShoulder.getAngleYZ())) {
            if (skeleton.leftShoulder.getAngleYZ() + openNiShouldersOffset >= 0) {
              leftArm.shoulder.moveTo(skeleton.leftShoulder.getAngleYZ() - 50);
            }
          }
        }
        if (rightArm != null) {

          if (!Double.isNaN(skeleton.rightElbow.getAngleXY())) {
            if (skeleton.rightElbow.getAngleXY() >= 0) {
              rightArm.bicep.moveTo(skeleton.rightElbow.getAngleXY());
            }
          }
          if (!Double.isNaN(skeleton.rightShoulder.getAngleXY())) {
            if (skeleton.rightShoulder.getAngleXY() >= 0) {
              rightArm.omoplate.moveTo(skeleton.rightShoulder.getAngleXY());
            }
          }
          if (!Double.isNaN(skeleton.rightShoulder.getAngleYZ())) {
            if (skeleton.rightShoulder.getAngleYZ() + openNiShouldersOffset >= 0) {
              rightArm.shoulder.moveTo(skeleton.rightShoulder.getAngleYZ() - 50);
            }
          }
        }

      }
    }

    // TODO - route data appropriately
    // rgb & depth image to OpenCV
    // servos & depth image to gui (entire InMoov + references to servos)
  }

  // ---------- movement commands begin ---------

  public void openlefthand() {
    moveHand("left", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
  }

  public void openPinch(String which) {
    moveHand(which, 0.0, 0.0, 180.0, 180.0, 180.0);
  }

  public void openrighthand() {
    moveHand("right", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
  }

  public void powerDown() {

    rest();
    purgeTasks();
    disable();

    // TODO standard relay line ?
    // right
    // rightSerialPort.digitalWrite(53, Arduino.LOW);
    // leftSerialPort.digitalWrite(53, Arduino.LOW);
    if (ear != null) {
      ear.lockOutAllGrammarExcept("power up");
    }

    startSleep = System.currentTimeMillis();
    python.execMethod("power_down");
  }

  public void powerUp() {
    startSleep = null;
    enable();
    rest();
    if (ear != null) {
      ear.clearLock();
    }

    beginCheckingOnInactivity();

    python.execMethod("power_up");
  }

  public void publishPin(Pin pin) {
    log.info("{} - {}", pin.pin, pin.value);
    if (pin.value == 1) {
      lastPIRActivityTime = System.currentTimeMillis();
    }
    // if its PIR & PIR is active & was sleeping - then wake up !
    if (pirPin == pin.pin && startSleep != null && pin.value == 1) {
      // attach(); // good morning / evening / night... asleep for % hours
      powerUp();
      // Calendar now = Calendar.getInstance();

      /*
       * FIXME - make a getSalutation String salutation = "hello "; if
       * (now.get(Calendar.HOUR_OF_DAY) < 12) { salutation = "good morning "; }
       * else if (now.get(Calendar.HOUR_OF_DAY) < 16) { salutation =
       * "good afternoon "; } else { salutation = "good evening "; }
       * 
       * 
       * speakBlocking(String.format("%s. i was sleeping but now i am awake" ,
       * salutation));
       */
    }
  }

  @Override
  public void purgeTasks() {
    speakBlocking("purging all tasks");
    super.purgeTasks();
  }

  /*
   * Service registration event. On newly registered service the InMoov service
   * will set up various routing.
   * 
   * Routing of errors back to the InMoov service. This will allow the mouth to
   * announce errors
   * 
   */
  public void onRegistered(ServiceInterface sw) {
    subscribe(sw.getName(), "publishError");
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

  @Override
  public boolean save() {
    super.save();
    if (leftHand != null) {
      leftHand.save();
    }

    if (rightHand != null) {
      rightHand.save();
    }

    if (rightArm != null) {
      rightArm.save();
    }

    if (leftArm != null) {
      leftArm.save();
    }

    if (head != null) {
      head.save();
    }

    if (openni != null) {
      openni.save();
    }

    return true;
  }

  @Deprecated
  public void setArmSpeed(String which, Double bicep, Double rotate, Double shoulder, Double omoplate) {
    if (!arms.containsKey(which)) {
      error("setArmSpeed %s does not exist", which);
    } else {
      arms.get(which).setSpeed(bicep, rotate, shoulder, omoplate);
    }
  }

  public void setArmVelocity(String which, Double bicep, Double rotate, Double shoulder, Double omoplate) {
    if (!arms.containsKey(which)) {
      error("setArmVelocity %s does not exist", which);
    } else {
      arms.get(which).setVelocity(bicep, rotate, shoulder, omoplate);
    }
  }

  @Deprecated
  public void setHandSpeed(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky) {
    setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, null);
  }

  public void setHandVelocity(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky) {
    setHandVelocity(which, thumb, index, majeure, ringFinger, pinky, null);
  }

  @Deprecated
  public void setHandSpeed(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    if (!hands.containsKey(which)) {
      error("setHandSpeed %s does not exist", which);
    } else {
      hands.get(which).setSpeed(thumb, index, majeure, ringFinger, pinky, wrist);
    }
  }

  public void setHandVelocity(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    if (!hands.containsKey(which)) {
      error("setHandSpeed %s does not exist", which);
    } else {
      hands.get(which).setVelocity(thumb, index, majeure, ringFinger, pinky, wrist);
    }
  }

  @Deprecated
  public void setHeadSpeed(Double rothead, Double neck) {
    setHeadSpeed(rothead, neck, null, null, null);
  }

  public void setHeadVelocity(Double rothead, Double neck) {
    setHeadVelocity(rothead, neck, null, null, null, null);
  }

  public void setHeadVelocity(Double rothead, Double neck, Double rollNeck) {
    setHeadVelocity(rothead, neck, null, null, null, rollNeck);
  }

  public void setHeadVelocity(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {
    setHeadVelocity(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed, null);
  }

  @Deprecated
  public void setHeadSpeed(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {
    if (head != null) {
      head.setSpeed(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed);
    } else {
      log.warn("setHeadSpeed - I have no head");
    }
  }

  @Deprecated
  public void setHeadVelocity(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed, Double rollNeckSpeed) {
    if (head != null) {
      head.setVelocity(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed, rollNeckSpeed);
    } else {
      log.warn("setHeadVelocity - I have no head");
    }
  }

  public void setMute(boolean mute) {
    this.mute = mute;
  }

  @Deprecated
  public void setTorsoSpeed(Double topStom, Double midStom, Double lowStom) {
    if (torso != null) {
      torso.setSpeed(topStom, midStom, lowStom);
    } else {
      log.warn("setTorsoSpeed - I have no torso");
    }
  }

  public void setTorsoVelocity(Double topStom, Double midStom, Double lowStom) {
    if (torso != null) {
      torso.setVelocity(topStom, midStom, lowStom);
    } else {
      log.warn("setTorsoVelocity - I have no torso");
    }
  }

  public void setEyelidsVelocity(Double eyelidleft, Double eyelidright) {
    if (eyelids != null) {
      eyelids.setVelocity(eyelidleft, eyelidright);
    } else {
      log.warn("setEyelidsVelocity - I have no eyelids");
    }
  }

  public List<AudioData> speakBlocking(Status test) {
    if (test != null && !mute) {
      return speakBlocking(test.toString());
    }
    return null;
  }

  public List<AudioData> speakBlocking(String toSpeak) {
    if (mouth != null && !mute) {
      try {
        return mouth.speakBlocking(toSpeak);
      } catch (Exception e) {
        Logging.logError(e);
      }
    }
    return null;
  }

  public boolean speakErrors(boolean b) {
    speakErrors = b;
    return b;
  }

  /*************
   * STARTS BEGIN
   * 
   * @param leftPort
   *          com port
   * @param rightPort
   *          com port
   * @throws Exception
   *           e
   * 
   ************************/

  public void startAll(String leftPort, String rightPort) throws Exception {
    // TODO add vision
    startMouth();
    startHead(leftPort);
    startEar();

    startMouthControl(leftPort);

    startLeftHand(leftPort);
    startRightHand(rightPort);
    //startEyelids(rightPort);
    startLeftArm(leftPort);
    startRightArm(rightPort);
    startTorso(leftPort);

    startHeadTracking(leftPort, 12, 13);
    startEyesTracking(leftPort, 22, 24);

    speakBlocking("startup sequence completed");
  }

  public InMoovArm startArm(String side, String port, String type) throws Exception {
    speakBlocking(String.format("starting %s arm", side));

    InMoovArm arm = (InMoovArm) startPeer(String.format("%sArm", side));
    arms.put(side, arm);
    arm.setSide(side);// FIXME WHO USES SIDE - THIS SHOULD BE NAME !!!
    if (type == null) {
      type = Arduino.BOARD_TYPE_MEGA;
    }

    arm.arduino.setBoard(type);
    arm.arduino.usedByInmoov = true;
    arm.arduino.serial.usedByInmoov = true;
    arm.connect(port);
    arduinos.put(port, arm.arduino);

    return arm;
  }

  // TODO TODO TODO - context & status report -
  // "current context is right hand"
  // FIXME - voice control for all levels (ie just a hand or head !!!!)
  public SpeechRecognizer startEar() throws Exception {
    speakBlocking("starting ear");
    ear = (SpeechRecognizer) startPeer("ear");
    subscribe(ear.getName(), "listeningEvent");
    subscribe(ear.getName(), "pauseListening");
    subscribe(ear.getName(), "recognized");

    if (mouth != null) {
      ear.addMouth(mouth);
    }
    return ear;
  }

  public Tracking startEyesTracking(String port, int xPin, int yPin) throws Exception {
    speakBlocking("starting eyes tracking");

    if (head == null) {
      startHead(port);
    }
    eyesTracking = (Tracking) startPeer("eyesTracking");
    eyesTracking.connect(opencv, head.eyeX, head.eyeY);
    // TODO: why do we need this next line?
    arduinos.put(port, (Arduino) eyesTracking.getArduino());
    return eyesTracking;
  }

  public InMoovHand startHand(String side, String port, String type) throws Exception {
    speakBlocking(String.format("starting %s hand", side));

    InMoovHand hand = (InMoovHand) startPeer(String.format("%sHand", side));
    hand.setSide(side);
    hands.put(side, hand);
    if (type == null) {
      type = Arduino.BOARD_TYPE_MEGA;
    }

    hand.arduino.setBoard(type);
    hand.arduino.serial.usedByInmoov = true;
    hand.arduino.usedByInmoov = true;
    hand.connect(port);
    arduinos.put(port, hand.arduino);
    return hand;
  }

  public InMoovHead startHead(String port) throws Exception {
    return startHead(port, null, 12, 13, 22, 24, 26, 30);
  }

  public InMoovHead startHead(String port, String type) throws Exception {
    return startHead(port, type, 12, 13, 22, 24, 26, 30);
  }

  public InMoovHead startHead(String port, Integer headYPin, Integer headXPin, Integer eyeXPin, Integer eyeYPin, Integer jawPin, Integer rollNeckPin) throws Exception {
    return startHead(port, null, headYPin, headXPin, eyeXPin, eyeYPin, jawPin, rollNeckPin);
  }

  public InMoovHead startHead(String port, String type, Integer headYPin, Integer headXPin, Integer eyeXPin, Integer eyeYPin, Integer jawPin, Integer rollNeckPin)
      throws Exception {
    // log.warn(InMoov.buildDNA(myKey, serviceClass))
    speakBlocking(String.format("starting head on %s", port));

    opencv = (OpenCV) startPeer("opencv");
    head = (InMoovHead) startPeer("head");

    if (type == null) {
      type = Arduino.BOARD_TYPE_MEGA;
    }

    head.arduino.setBoard(type);
    head.arduino.usedByInmoov = true;
    head.arduino.serial.usedByInmoov = true;
    head.connect(port, headYPin, headXPin, eyeXPin, eyeYPin, jawPin, rollNeckPin);
    arduinos.put(port, head.arduino);
    return head;
  }

  @Deprecated
  public void enableAutoDisable(Boolean param) {
    setAutoDisable(param);
  }

  public void setAutoDisable(Boolean param) {
    if (head != null) {
      head.setAutoDisable(param);
    }
    if (rightArm != null) {
      rightArm.setAutoDisable(param);
    }
    if (leftArm != null) {
      leftArm.setAutoDisable(param);
    }
    if (leftHand != null) {
      leftHand.setAutoDisable(param);
    }
    if (rightHand != null) {
      leftHand.setAutoDisable(param);
    }
    if (torso != null) {
      torso.setAutoDisable(param);
    }
    if (eyelids != null) {
      eyelids.setAutoDisable(param);
    }
  }

  private void setOverrideAutoDisable(Boolean param) {
    if (head != null) {
      head.setOverrideAutoDisable(param);
    }
    if (rightArm != null) {
      rightArm.setOverrideAutoDisable(param);
    }
    if (leftArm != null) {
      leftArm.setOverrideAutoDisable(param);
    }
    if (leftHand != null) {
      leftHand.setOverrideAutoDisable(param);
    }
    if (rightHand != null) {
      rightHand.setOverrideAutoDisable(param);
    }
    if (torso != null) {
      torso.setOverrideAutoDisable(param);
    }
    if (eyelids != null) {
      eyelids.setOverrideAutoDisable(param);
    }
  }

  // NOTE - BEST Services are one which are reflective on startService
  // like xmpp which exposes a the reflective REST API are startService
  public Tracking startHeadTracking(String port, int neckPin, int rotheadPin) throws Exception {
    speakBlocking("starting head tracking");

    if (head == null) {
      startHead(port);
    }
    //rest before tracking at fullspeed
    setHeadVelocity(80.0, 80.0, 80.0);
    moveHeadBlocking(head.neck.getRest(), head.rothead.getRest(), head.rollNeck.getRest());
    setHeadVelocity(-1.0, -1.0, -1.0);
    headTracking = (Tracking) startPeer("headTracking");
    // We should pass the servos that control the head in here! 
    headTracking.connect(opencv, head.rothead, head.neck);
    // TODO: why is this needed?!
    arduinos.put(port, (Arduino) headTracking.controller);
    return headTracking;
  }

  public void stopHeadTracking() {
    if (headTracking != null) {
      headTracking.stopTracking();
      setHeadVelocity(80.0, 80.0, 80.0);
      moveHeadBlocking(head.neck.getRest(), head.rothead.getRest(), head.rollNeck.getRest());
    }
  }

  public InMoovArm startLeftArm(String port) throws Exception {
    return startLeftArm(port, null);
  }

  public InMoovArm startLeftArm(String port, String type) throws Exception {
    leftArm = startArm(LEFT, port, type);
    return leftArm;
  }

  public InMoovHand startLeftHand(String port) throws Exception {
    return startLeftHand(port, null);
  }

  public InMoovHand startLeftHand(String port, String type) throws Exception {
    leftHand = startHand(LEFT, port, type);
    return leftHand;
  }

  // gestures begin ---------------

  public SpeechSynthesis startMouth() throws Exception {
    mouth = (SpeechSynthesis) startPeer("mouth");
    speakBlocking("starting mouth");

    if (ear != null) {
      ear.addMouth(mouth);
    }
    return mouth;
  }

  public MouthControl startMouthControl(String port) throws Exception {
    speakBlocking("starting mouth control");
    if (mouthControl == null) {

      if (head == null) {
        startHead(port);
      }

      mouthControl = (MouthControl) startPeer("mouthControl");

      mouthControl.arduino.connect(port);
      mouthControl.jaw.attach(mouthControl.arduino, 26);

      arduinos.put(port, mouthControl.arduino);
      String p = mouthControl.getArduino().getSerial().getPortName();
      if (p != null) {
        arduinos.put(p, mouthControl.arduino);
      }
      mouthControl.setmouth(10, 50);
    }
    return mouthControl;
  }

  // starting routines need to be fully re-entrant
  // they can be used to get a reference and start a very limited sub-system
  // of inmoov
  // very useful in the fact a head subsystem can be tested without starting
  // all of the peer services of the head
  public OpenCV startOpenCV() throws Exception {
    if (opencv != null) {
      opencv = (OpenCV) startPeer("opencv");
    }
    return opencv;
  }

  public OpenNi startOpenNI() throws Exception {
    if (openni == null) {
      speakBlocking("starting kinect");
      openni = (OpenNi) startPeer("openni");
      pid = (Pid) startPeer("pid");

      pid.setPID("kinect", 10.0, 0.0, 1.0);
      pid.setMode("kinect", Pid.MODE_AUTOMATIC);
      pid.setOutputRange("kinect", -1, 1);

      pid.setControllerDirection("kinect", 0);

      // re-mapping of skeleton !
      openni.skeleton.leftElbow.mapXY(0, 180, 180, 0);
      openni.skeleton.rightElbow.mapXY(0, 180, 180, 0);
      if (openNiLeftShoulderInverted) {
        openni.skeleton.leftShoulder.mapYZ(0, 180, 180, 0);
      }
      if (openNiRightShoulderInverted) {
        openni.skeleton.rightShoulder.mapYZ(0, 180, 180, 0);
      }

      // openni.skeleton.leftShoulder

      // openni.addListener("publishOpenNIData", this.getName(),
      // "getSkeleton");
      // openni.addOpenNIData(this);
      subscribe(openni.getName(), "publishOpenNIData");
    }
    return openni;
  }

  public void startPIR(String port, int pin) throws IOException {
    speakBlocking(String.format("starting pee. eye. are. sensor on port %s pin %d", port, pin));
    if (arduinos.containsKey(port)) {
      Arduino arduino = arduinos.get(port);
      // arduino.connect(port);
      // arduino.setSampleRate(8000);
      arduino.enablePin(pin, 10);
      pirArduino = arduino;
      pirPin = pin;
      arduino.addListener("publishPin", this.getName(), "publishPin");

    } else {
      // FIXME - SHOULD ALLOW STARTUP AND LATER ACCESS VIA PORT ONCE OTHER
      // STARTS CHECK MAP FIRST
      log.error("{} arduino not found - start some other system first (head, arm, hand)", port);
    }

  }

  public InMoovArm startRightArm(String port) throws Exception {
    return startRightArm(port, null);
  }

  public InMoovArm startRightArm(String port, String type) throws Exception {
    if (rightArm != null) {
      info("right arm already started");
      return rightArm;
    }
    rightArm = startArm(RIGHT, port, type);
    return rightArm;
  }

  public InMoovHand startRightHand(String port) throws Exception {
    return startRightHand(port, null);
  }

  public InMoovHand startRightHand(String port, String type) throws Exception {
    rightHand = startHand(RIGHT, port, type);
    return rightHand;
  }

  @Override
  public void startService() {
    super.startService();
    python = getPython();
  }

  public InMoovTorso startTorso(String port) throws Exception {
    return startTorso(port, null);
  }

  public InMoovTorso startTorso(String port, String type) throws Exception {
    // log.warn(InMoov.buildDNA(myKey, serviceClass))
    speakBlocking(String.format("starting torso on %s", port));

    torso = (InMoovTorso) startPeer("torso");

    if (type == null) {
      type = Arduino.BOARD_TYPE_MEGA;
    }

    torso.arduino.setBoard(type);
    torso.arduino.usedByInmoov = true;
    torso.arduino.serial.usedByInmoov = true;
    torso.connect(port);
    arduinos.put(port, torso.arduino);

    return torso;
  }

  public InMoovEyelids startEyelids(String port) throws Exception {
    return startEyelids(port, null, 22, 24);
  }

  public InMoovEyelids startEyelids(String port, int eyelidleftPin, int eyelidrightPin) throws Exception {
    return startEyelids(port, null, eyelidleftPin, eyelidrightPin);
  }

  /*
   * Old startEyelids method for backward compatibility;
   * Old because arduino controller dependent...
   * So, here we create a default peer arduino controller
   */
  public InMoovEyelids startEyelids(String port, String type, int eyeLidLeftPin, int eyeLidRightPin) throws Exception {
    // log.warn(InMoov.buildDNA(myKey, serviceClass))
    speakBlocking(String.format("starting eyelids on %s", port));

    Arduino eyelidsArduino = (Arduino) createPeer("eyelidsArduino");
    eyelidsArduino.startService();

    if (type == null) {
      type = Arduino.BOARD_TYPE_MEGA;
    }

    eyelidsArduino.setBoard(type);
    eyelidsArduino.usedByInmoov = true;
    eyelidsArduino.serial.usedByInmoov = true;
    eyelidsArduino.connect(port);
    if (!eyelidsArduino.isConnected()) {
      error("arduino %s not connected", eyelidsArduino.getName());
      return null;
    }
    arduinos.put(port, eyelidsArduino);
    return startEyelids(eyelidsArduino, eyeLidLeftPin, eyeLidRightPin);
  }

  /*
   * New startEyelids attach method ( for testing );
   */
  public InMoovEyelids startEyelids(ServoController controller, Integer eyeLidLeftPin, Integer eyeLidRightPin) throws Exception {
    eyelids = (InMoovEyelids) startPeer("eyelids");
    eyelids.attach(controller, eyeLidLeftPin, eyeLidRightPin);
    return eyelids;
  }

  public void stopPIR() {
    if (pirArduino != null && pirPin != null) {
      pirArduino.disablePin(pirPin);
      pirPin = null;
      pirArduino = null;
    }
    /*
     * if (arduinos.containsKey(port)) { Arduino arduino = arduinos.get(port);
     * arduino.connect(port); arduino.setSampleRate(8000);
     * arduino.digitalReadPollStart(pin); pirPin = pin;
     * arduino.addListener("publishPin", this.getName(), "publishPin"); }
     */

  }

  public void stopTracking() {
    if (eyesTracking != null) {
      eyesTracking.stopTracking();
    }

    if (headTracking != null) {
      headTracking.stopTracking();
    }
  }

  // This is an in-flight check vs power up or power down
  public void systemCheck() {
    speakBlocking("starting system check");
    speakBlocking("testing");

    rest();
    sleep(500);

    if (rightHand != null) {
      speakBlocking("testing right hand");
      rightHand.test();
    }

    if (rightArm != null) {
      speakBlocking("testing right arm");
      rightArm.test();
    }

    if (leftHand != null) {
      speakBlocking("testing left hand");
      leftHand.test();
    }

    if (leftArm != null) {
      speakBlocking("testing left arm");
      leftArm.test();
    }

    if (head != null) {
      speakBlocking("testing head");
      head.test();
    }

    if (torso != null) {
      speakBlocking("testing torso");
      torso.test();
    }

    if (eyelids != null) {
      speakBlocking("testing eyelids");
      eyelids.test();
    }

    sleep(500);
    rest();
    broadcastState();
    speakBlocking("system check completed");
  }

  /*
   * public boolean load(){ super.load(); if (leftHand != null){
   * leftHand.load(); }
   * 
   * if (rightHand != null){ rightHand.load(); }
   * 
   * if (rightArm != null){ rightArm.load(); }
   * 
   * if (leftArm != null){ leftArm.load(); }
   * 
   * if (head != null){ head.load(); }
   * 
   * if (openni != null){ openni.load(); }
   * 
   * return true; }
   */

  public void track() {
    if (headTracking == null) {
      error("attach head before tracking");
    } else {
      headTracking.trackPoint(0.5, 0.5);
    }
  }

  public void trackHumans() {
    if (eyesTracking != null) {
      eyesTracking.faceDetect();
    }

    if (headTracking != null) {
      headTracking.faceDetect();
    }
  }

  public void trackPoint() {

    if (eyesTracking != null) {
      eyesTracking.startLKTracking();
      eyesTracking.trackPoint(0.5, 0.5);
    }

    if (headTracking != null) {
      headTracking.startLKTracking();
      headTracking.trackPoint(0.5, 0.5);
    }
  }

  public void loadGestures() {
    loadGestures(GESTURES_DIRECTORY);
  }

  /**
   * This method will look at all of the .py files in a directory. One by one it
   * will load the files into the python interpreter. A gesture python file
   * should contain 1 method definition that is the same as the filename.
   * 
   * @param directory
   *          - the directory that contains the gesture python files.
   */
  public void loadGestures(String directory) {
    // TODO: iterate over each of the python files in the directory
    // and load them into the python interpreter.
    File dir = makeGesturesDirectory(directory);
    for (File f : dir.listFiles()) {
      if (f.getName().toLowerCase().endsWith(".py")) {
        log.info("Loading Gesture Python file {}", f.getAbsolutePath());
        Python p = (Python) Runtime.getService("python");
        String script = null;
        try {
          script = FileIO.toString(f.getAbsolutePath());
        } catch (IOException e) {
          log.warn("IO Error loading gesture file {} -- {}", f.getAbsolutePath(), e);
          continue;
        }
        // evaluate the gestures scripts in a blocking way.
        boolean result = p.exec(script, true, true);
        if (!result) {
          log.warn("Error while loading gesture file: {}", f.getAbsolutePath());
        } else {
          log.info("Successfully loaded gesture {}", f.getAbsolutePath());
        }
      }
    }
  }

  public void loadCalibration() {
    loadCalibration(CALIBRATION_FILE);
  }

  public void loadCalibration(String calibrationFilename) {
    File calibF = new File(calibrationFilename);
    if (calibF.exists()) {
      log.info("Loading Calibration Python file {}", calibF.getAbsolutePath());
      Python p = (Python) Runtime.getService("python");
      try {
        p.execFile(calibF.getAbsolutePath());
      } catch (IOException e) {
        // TODO Auto-generated catch block
        log.warn("Error loading calibratoin file {}", calibF.getAbsolutePath());
        e.printStackTrace();
      }
    }
  }

  public void saveCalibration() {
    saveCalibration(CALIBRATION_FILE);
  }

  public void saveCalibration(String calibrationFilename) {

    File calibFile = new File(calibrationFilename);
    FileWriter calibrationWriter = null;
    try {
      calibrationWriter = new FileWriter(calibFile);
      calibrationWriter.write("##################################\n");
      calibrationWriter.write("# InMoov auto generated calibration \n");
      calibrationWriter.write("# " + new Date() + "\n");
      calibrationWriter.write("##################################\n");
      // String inMoovName = this.getName();
      // iterate all the services that are running.
      // we want all servos that are currently in the system?
      for (ServiceInterface service : Runtime.getServices()) {
        if (service instanceof Servo) {
          Servo s = (Servo) service;
          if (!s.getName().startsWith(this.getName())) {
            continue;
          }
          calibrationWriter.write("\n");
          // first detach the servo.
          calibrationWriter.write("# Servo Config : " + s.getName() + "\n");
          calibrationWriter.write(s.getName() + ".detach()\n");
          calibrationWriter.write(s.getName() + ".setMinMax(" + s.getMin() + "," + s.getMax() + ")\n");
          calibrationWriter.write(s.getName() + ".setVelocity(" + s.getVelocity() + ")\n");
          calibrationWriter.write(s.getName() + ".setRest(" + s.getRest() + ")\n");
          if (s.getPin() != null) {
            calibrationWriter.write(s.getName() + ".setPin(" + s.getPin() + ")\n");
          } else {
            calibrationWriter.write("# " + s.getName() + ".setPin(" + s.getPin() + ")\n");
          }

          s.map(s.getMinInput(), s.getMaxInput(), s.getMinOutput(), s.getMaxOutput());
          // save the servo map
          calibrationWriter.write(s.getName() + ".map(" + s.getMinInput() + "," + s.getMaxInput() + "," + s.getMinOutput() + "," + s.getMaxOutput() + ")\n");
          // if there's a controller reattach it at rest
          if (s.getController() != null) {
            String controller = s.getController().getName();
            calibrationWriter.write(s.getName() + ".attach(\"" + controller + "\"," + s.getPin() + "," + s.getRest() + ")\n");
          }
          if (s.getAutoDisable()) {
            calibrationWriter.write(s.getName() + ".setAutoDisable(True)\n");
          }
        }

      }
      calibrationWriter.write("\n");
      calibrationWriter.close();
    } catch (IOException e) {
      log.warn("Error writing calibration file {}", calibrationFilename);
      e.printStackTrace();
      return;
    }
  }

  public void savePose(String poseName) {
    // TODO: consider a prefix for the pose name? 
    String script = captureGesture(poseName);

  }

  private File makeGesturesDirectory(String directory) {
    File dir = new File(directory);
    dir.mkdirs();
    if (!dir.isDirectory()) {
      // TODO: maybe create the directory ?
      log.warn("Gestures directory {} doest not exist.", directory);
      return null;
    }
    return dir;
  }

  public void saveGesture(String gestureName, String directory) {
    // TODO: consider the gestures directory as a property on the inmoov
    String gestureMethod = mapGestureNameToPythonMethod(gestureName);
    String gestureFilename = directory + File.separator + gestureMethod + ".py";
    File gestureFile = new File(gestureFilename);
    if (gestureFile.exists()) {
      log.warn("Gesture file {} already exists.. not overwiting it.", gestureFilename);
      return;
    }
    FileWriter gestureWriter = null;
    try {
      gestureWriter = new FileWriter(gestureFile);
      // print the first line of the python file
      gestureWriter.write("def " + gestureMethod + "():\n");
      // now for each servo, we should write out the approperiate moveTo
      // statement
      // TODO: consider doing this only for the inmoov services.. but for now..
      // i think
      // we want all servos that are currently in the system?
      for (ServiceInterface service : Runtime.getServices()) {
        if (service instanceof Servo) {
          double pos = ((Servo) service).getPos();
          gestureWriter.write("  " + service.getName() + ".moveTo(" + pos + ")\n");
        }
      }
      gestureWriter.write("\n");
      gestureWriter.close();
    } catch (IOException e) {
      log.warn("Error writing gestures file {}", gestureFilename);
      e.printStackTrace();
      return;
    }
    // TODO: consider writing out cooresponding AIML?
  }

  private String mapGestureNameToPythonMethod(String gestureName) {
    // TODO: some fancier mapping?
    String methodName = gestureName.replaceAll(" ", "");
    return methodName;
  }

  public void saveGesture(String gestureName) {
    // TODO: allow a user to save a gesture to the gestures directory
    saveGesture(gestureName, GESTURES_DIRECTORY);
  }

  // waiting controable threaded gestures we warn user
  boolean gestureAlreadyStarted = false;

  public void startedGesture() {
    startedGesture("unknown");
  }

  public void startedGesture(String nameOfGesture) {
    if (gestureAlreadyStarted) {
      warn("Warning 1 gesture already running, this can break spacetime and lot of things");
    } else {
      gestureAlreadyStarted = true;
      RobotCanMoveRandom = false;
      setOverrideAutoDisable(true);
    }
  }

  public void finishedGesture() {
    finishedGesture("unknown");
  }

  public void finishedGesture(String nameOfGesture) {
    if (gestureAlreadyStarted) {
      waitTargetPos();
      RobotCanMoveRandom = true;
      setOverrideAutoDisable(false);
      gestureAlreadyStarted = false;
    }
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);

      String leftPort = "COM3";
      String rightPort = "COM4";

      VirtualArduino vleft = (VirtualArduino) Runtime.start("vleft", "VirtualArduino");
      VirtualArduino vright = (VirtualArduino) Runtime.start("vright", "VirtualArduino");
      vleft.connect("COM3");
      vright.connect("COM4");
      Runtime.start("gui", "SwingGui");
      Runtime.start("python", "Python");
      InMoov i01 = (InMoov) Runtime.start("i01", "InMoov");
      i01.startAll(leftPort, rightPort);
      i01.moveArm("left", 20.0, 10.0, 5.0, 40.0);
      log.info(i01.captureGesture());
      i01.rest();
      log.info(i01.captureGesture("rest"));

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(InMoov.class.getCanonicalName());
    meta.addDescription("The InMoov service");
    meta.addCategory("robot");
    // meta.addDependency("inmoov.fr", "1.0.0");
    // meta.addDependency("org.myrobotlab.inmoov", "1.0.0");
    meta.addDependency("inmoov.fr", "inmoov", "1.1.1", "zip");
    meta.addDependency("inmoov.fr", "jm3-model", "1.0.0", "zip");

    // SHARING !!! - modified key / actual name begin -------
    meta.sharePeer("head.arduino", "left", "Arduino", "shared left arduino");
    meta.sharePeer("torso.arduino", "left", "Arduino", "shared left arduino");

    meta.sharePeer("mouthControl.arduino", "left", "Arduino", "shared left arduino");

    meta.sharePeer("leftArm.arduino", "left", "Arduino", "shared left arduino");
    meta.sharePeer("leftHand.arduino", "left", "Arduino", "shared left arduino");
    // eyelidsArduino peer for backward compatibility
    meta.sharePeer("eyelidsArduino", "right", "Arduino", "shared right arduino");
    meta.sharePeer("rightArm.arduino", "right", "Arduino", "shared right arduino");
    meta.sharePeer("rightHand.arduino", "right", "Arduino", "shared right arduino");

    meta.sharePeer("eyesTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");
    meta.sharePeer("eyesTracking.controller", "left", "Arduino", "shared head Arduino");
    meta.sharePeer("eyesTracking.x", "head.eyeX", "Servo", "shared servo");
    meta.sharePeer("eyesTracking.y", "head.eyeY", "Servo", "shared servo");

    meta.sharePeer("headTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");
    meta.sharePeer("headTracking.controller", "left", "Arduino", "shared head Arduino");
    meta.sharePeer("headTracking.x", "head.rothead", "Servo", "shared servo");
    meta.sharePeer("headTracking.y", "head.neck", "Servo", "shared servo");

    meta.sharePeer("mouthControl.arduino", "left", "Arduino", "shared head Arduino");
    meta.sharePeer("mouthControl.mouth", "mouth", speechService, "shared Speech");
    meta.sharePeer("mouthControl.jaw", "head.jaw", "Servo", "shared servo");
    // SHARING !!! - modified key / actual name end ------

    // Global - undecorated by self name
    meta.addRootPeer("python", "Python", "shared Python service");

    // put peer definitions in
    meta.addPeer("torso", "InMoovTorso", "torso");
    meta.addPeer("eyelids", "InMoovEyelids", "eyelids");
    meta.addPeer("leftArm", "InMoovArm", "left arm");
    meta.addPeer("leftHand", "InMoovHand", "left hand");
    meta.addPeer("rightArm", "InMoovArm", "right arm");
    meta.addPeer("rightHand", "InMoovHand", "right hand");
    // webkit speech.
    meta.addPeer("ear", speechRecognizer, "InMoov webkit speech recognition service");
    // meta.addPeer("ear", "Sphinx", "InMoov Sphinx speech recognition
    // service");
    meta.addPeer("eyesTracking", "Tracking", "Tracking for the eyes");
    meta.addPeer("head", "InMoovHead", "the head");
    meta.addPeer("headTracking", "Tracking", "Head tracking system");
    meta.addPeer("mouth", speechService, "InMoov speech service");
    meta.addPeer("mouthControl", "MouthControl", "MouthControl");
    meta.addPeer("opencv", "OpenCV", "InMoov OpenCV service");
    meta.addPeer("openni", "OpenNi", "Kinect service");
    meta.addPeer("pid", "Pid", "Pid service");

    // For VirtualInMoov
    meta.addPeer("jmonkeyEngine", "JMonkeyEngine", "Virtual inmoov");

    // For IntegratedMovement
    meta.addPeer("integratedMovement", "IntegratedMovement", "Inverse kinematic type movement");

    return meta;
  }

  // vinmoov cosmetics and optional vinmoov monitor idea ( poc i know nothing
  // about jme...)
  // just want to use jme as main screen and show some informations
  // like batterie / errors / onreconized text etc ...
  // i01.VinmoovMonitorActivated=1 before to start vinmoov

  public Boolean VinmoovMonitorActivated = false;

  public void onListeningEvent() {
    if (vinMoovApp != null && VinmoovMonitorActivated && RobotIsStarted) {
      vinMoovApp.setMicro(true);
    }
  }

  public void onPauseListening() {
    if (vinMoovApp != null && VinmoovMonitorActivated && RobotIsStarted) {
      vinMoovApp.setMicro(false);
    }
  }

  public void onRecognized(String text) {
    if (vinMoovApp != null && VinmoovMonitorActivated && RobotIsStarted) {
      vinMoovApp.onRecognized(text);
    }
  }

  public void setBatteryLevel(Integer level) {
    if (vinMoovApp != null && VinmoovMonitorActivated && RobotIsStarted) {
      vinMoovApp.setBatteryLevel(level);

    }
  }

  public Boolean VinmoovFullScreen = false;
  public String VinmoovBackGroundColor = "Grey";

  public int VinmoovWidth = 800;
  public int VinmoovHeight = 600;
  private Boolean debugVinmoov = true;

  // show some infos to jme screen
  public void setLeftArduinoConnected(boolean param) {
    vinMoovApp.setLeftArduinoConnected(param);
  }

  public void setRightArduinoConnected(boolean param) {
    vinMoovApp.setRightArduinoConnected(param);
  }

  // end vinmoov cosmetics and optional vinmoov monitor

  public InMoov3DApp startVinMoov() throws InterruptedException {
    if (vinMoovApp == null) {
      vinMoovApp = new InMoov3DApp();
      if (VinmoovMonitorActivated) {
        // vinmoovFullScreen=true
        VinmoovBackGroundColor = "Black";
        debugVinmoov = false;
        vinMoovApp.VinmoovMonitorActivated = true;
        VinmoovWidth = 1067;
      }
      vinMoovApp.BackGroundColor = VinmoovBackGroundColor;
      AppSettings settings = new AppSettings(true);
      settings.setResolution(VinmoovWidth, VinmoovHeight);
      settings.setFullscreen(VinmoovFullScreen);
      // settings.setEmulateMouse(false);
      // settings.setUseJoysticks(false);
      settings.setUseInput(true);
      settings.setAudioRenderer(null);
      vinMoovApp.setSettings(settings);
      vinMoovApp.setShowSettings(false);
      vinMoovApp.setDisplayStatView(debugVinmoov);
      vinMoovApp.setPauseOnLostFocus(false);
      vinMoovApp.setService(this);
      vinMoovApp.start();
      synchronized (this) {
        wait(6000);
      }
      if (torso != null) {
        vinMoovApp.addServo("mtorso", torso.midStom);
        torso.midStom.addIKServoEventListener(this);
        vinMoovApp.addServo("ttorso", torso.topStom);
        torso.topStom.addIKServoEventListener(this);
        torso.midStom.moveTo(torso.midStom.targetPos + 0.2);
        torso.topStom.moveTo(torso.topStom.targetPos + 0.2);
      }
      if (rightArm != null) {
        vinMoovApp.addServo("Romoplate", rightArm.omoplate);
        rightArm.omoplate.addIKServoEventListener(this);
        vinMoovApp.addServo("Rshoulder", rightArm.shoulder);
        rightArm.shoulder.addIKServoEventListener(this);
        vinMoovApp.addServo("Rrotate", rightArm.rotate);
        rightArm.rotate.addIKServoEventListener(this);
        vinMoovApp.addServo("Rbicep", rightArm.bicep);
        rightArm.bicep.addIKServoEventListener(this);
        rightArm.omoplate.moveTo(rightArm.omoplate.targetPos + 0.2);
        rightArm.shoulder.moveTo(rightArm.shoulder.targetPos + 0.2);
        rightArm.rotate.moveTo(rightArm.rotate.targetPos + 0.2);
        rightArm.bicep.moveTo(rightArm.bicep.targetPos + 0.2);
      }
      if (leftArm != null) {
        vinMoovApp.addServo("omoplate", leftArm.omoplate);
        leftArm.omoplate.addIKServoEventListener(this);
        vinMoovApp.addServo("shoulder", leftArm.shoulder);
        leftArm.shoulder.addIKServoEventListener(this);
        vinMoovApp.addServo("rotate", leftArm.rotate);
        leftArm.rotate.addIKServoEventListener(this);
        vinMoovApp.addServo("bicep", leftArm.bicep);
        leftArm.bicep.addIKServoEventListener(this);
        leftArm.omoplate.moveTo(leftArm.omoplate.targetPos + 0.2);
        leftArm.shoulder.moveTo(leftArm.shoulder.targetPos + 0.2);
        leftArm.rotate.moveTo(leftArm.rotate.targetPos + 0.2);
        leftArm.bicep.moveTo(leftArm.bicep.targetPos + 0.2);
      }
      if (rightHand != null) {
        vinMoovApp.addServo("RWrist", rightHand.wrist);
        rightHand.wrist.addIKServoEventListener(this);
        rightHand.wrist.moveTo(rightHand.wrist.targetPos + 0.2);
      }
      if (leftHand != null) {
        vinMoovApp.addServo("LWrist", leftHand.wrist);
        leftHand.wrist.addIKServoEventListener(this);
        leftHand.wrist.moveTo(leftHand.wrist.targetPos + 0.2);
      }
      if (head != null) {
        vinMoovApp.addServo("neck", head.neck);
        head.neck.addIKServoEventListener(this);
        vinMoovApp.addServo("head", head.rothead);
        head.rothead.addIKServoEventListener(this);
        vinMoovApp.addServo("jaw", head.jaw);
        head.jaw.addIKServoEventListener(this);
        vinMoovApp.addServo("rollNeck", head.rollNeck);
        head.rollNeck.addIKServoEventListener(this);
        head.neck.moveTo(head.neck.targetPos + 0.2);
        head.rothead.moveTo(head.rothead.targetPos + 0.2);
        head.rollNeck.moveTo(head.rollNeck.targetPos + 0.2);
      }
    } else {
      log.info("VinMoov already started");
      return vinMoovApp;
    }

    return vinMoovApp;
  }

  public void onIKServoEvent(IKData data) {
    if (vinMoovApp != null) {
      vinMoovApp.updatePosition(data);
    }
  }

  public void stopVinMoov() {
    try {
      vinMoovApp.stop();
    } catch (NullPointerException e) {

    }
    vinMoovApp = null;
  }

  public void startIntegratedMovement() {
    integratedMovement = (IntegratedMovement) startPeer("integratedMovement");
    IntegratedMovement im = integratedMovement;
    // set the DH Links or each arms
    im.setNewDHRobotArm("leftArm");
    im.setNewDHRobotArm("rightArm");
    im.setNewDHRobotArm("kinect");
    if (torso != null) {
      im.setDHLink("leftArm", torso.midStom, 113, 90, 0, -90);
      im.setDHLink("rightArm", torso.midStom, 113, 90, 0, -90);
      im.setDHLink("kinect", torso.midStom, 113, 90, 0, -90);
      im.setDHLink("leftArm", torso.topStom, 0, 180, 292, 90);
      im.setDHLink("rightArm", torso.topStom, 0, 180, 292, 90);
      im.setDHLink("kinect", torso.topStom, 0, 180, 110, -90);
    } else {
      im.setDHLink("leftArm", "i01.torso.midStom", 113, 90, 0, -90);
      im.setDHLink("rightArm", "i01.torso.midStom", 113, 90, 0, -90);
      im.setDHLink("kinect", "i01.torso.midStom", 113, 90, 0, -90);
      im.setDHLink("leftArm", "i01.torso.topStom", 0, 180, 292, 90);
      im.setDHLink("rightArm", "i01.torso.topStom", 0, 180, 292, 90);
      im.setDHLink("kinect", "i01.torso.topStom", 0, 180, 110, -90);
    }
    im.setDHLink("leftArm", "leftS", 143, 180, 0, 90);
    im.setDHLink("rightArm", "rightS", -143, 180, 0, -90);
    if (arms.containsKey(LEFT)) {
      InMoovArm arm = arms.get(LEFT);
      im.setDHLink("leftArm", arm.omoplate, 0, -5.6, 45, -90);
      im.setDHLink("leftArm", arm.shoulder, 77, -30 + 90, 0, 90);
      im.setDHLink("leftArm", arm.rotate, 284, 90, 40, 90);
      im.setDHLink("leftArm", arm.bicep, 0, -7 + 24.4 + 90, 300, 90);
    } else {
      im.setDHLink("leftArm", "i01.leftArm.omoplate", 0, -5.6, 45, -90);
      im.setDHLink("leftArm", "i01.leftArm.shoulder", 77, -30 + 90, 0, 90);
      im.setDHLink("leftArm", "i01.leftArm.rotate", 284, 90, 40, 90);
      im.setDHLink("leftArm", "i01.leftArm.bicep", 0, -7 + 24.4 + 90, 300, 90);
    }
    if (arms.containsKey(RIGHT)) {
      InMoovArm arm = arms.get(RIGHT);
      im.setDHLink("rightArm", arm.omoplate, 0, -5.6, 45, 90);
      im.setDHLink("rightArm", arm.shoulder, -77, -30 + 90, 0, -90);
      im.setDHLink("rightArm", arm.rotate, -284, 90, 40, -90);
      im.setDHLink("rightArm", arm.bicep, 0, -7 + 24.4 + 90, 300, 0);
    } else {
      im.setDHLink("rightArm", "i01.rightArm.omoplate", 0, -5.6, 45, 90);
      im.setDHLink("rightArm", "i01.rightArm.shoulder", -77, -30 + 90, 0, -90);
      im.setDHLink("rightArm", "i01.rightArm.rotate", -284, 90, 40, -90);
      im.setDHLink("rightArm", "i01.rightArm.bicep", 0, -7 + 24.4 + 90, 300, 0);
    }
    if (hands.containsKey(LEFT)) {
      InMoovHand hand = hands.get(LEFT);
      im.setDHLink("leftArm", hand.wrist, 00, -90, 0, 0);
      im.setDHLinkType("i01.leftHand.wrist", DHLinkType.REVOLUTE_ALPHA);
    } else {
      im.setDHLink("leftArm", "i01.leftHand.wrist", 00, -90, 0, 0);
    }
    if (hands.containsKey(RIGHT)) {
      InMoovHand hand = hands.get(RIGHT);
      im.setDHLink("rightArm", hand.wrist, 00, -90, 0, 0);
      im.setDHLinkType("i01.rigtHand.wrist", DHLinkType.REVOLUTE_ALPHA);
    } else {
      im.setDHLink("rightArm", "i01.rightHand.wrist", 00, -90, 0, 0);
    }
    im.setDHLink("leftArm", "wristup", 0, -5, 90, 0);
    im.setDHLink("leftArm", "wristdown", 0, 0, 125, 45);
    im.setDHLink("leftArm", "finger", 5, -90, 5, 0);
    im.setDHLink("rightArm", "Rwristup", 0, 5, 90, 0);
    im.setDHLink("rightArm", "Rwristdown", 0, 0, 125, -45);
    im.setDHLink("rightArm", "Rfinger", 5, 90, 5, 0);
    im.setDHLink("kinect", "camera", 0, 90, 10, 90);

    // log.info("{}",im.createJointPositionMap("leftArm").toString());
    // start the kinematics engines 

    // define object, each dh link are set as an object, but the
    // start point and end point will be update by the ik service, but still
    // need
    // a name and a radius
    im.clearObject();
    im.addObject(0.0, 0.0, 0.0, 0.0, 0.0, -150.0, "base", 150.0, false);
    im.addObject("i01.torso.midStom", 150.0);
    im.addObject("i01.torso.topStom", 10.0);
    im.addObject("i01.leftArm.omoplate", 10.0);
    im.addObject("i01.rightArm.omoplate", 10.0);
    im.addObject("i01.leftArm.shoulder", 50.0);
    im.addObject("i01.rightArm.shoulder", 50.0);
    im.addObject("i01.leftArm.rotate", 50.0);
    im.addObject("i01.rightArm.rotate", 50.0);
    im.addObject("i01.leftArm.bicep", 60.0);
    im.addObject("i01.rightArm.bicep", 60.0);
    im.addObject("i01.leftHand.wrist", 70.0);
    im.addObject("i01.rightHand.wrist", 70.0);
    im.objectAddIgnore("i01.rightArm.omoplate", "i01.leftArm.rotate");
    im.objectAddIgnore("i01.rightArm.omoplate", "i01.rightArm.rotate");
    im.addObject("leftS", 10);
    im.addObject("rightS", 10);
    im.objectAddIgnore("leftS", "rightS");
    im.objectAddIgnore("rightS", "i01.leftArm.shoulder");
    im.objectAddIgnore("leftS", "i01.rightArm.shoulder");
    im.addObject("wristup", 70);
    im.addObject("wristdown", 70);
    im.objectAddIgnore("i01.leftArm.bicep", "wristup");
    im.addObject("Rwristup", 70);
    im.addObject("Rwristdown", 70);
    im.objectAddIgnore("i01.rightArm.bicep", "Rwristup");

    im.startEngine("leftArm");
    im.startEngine("rightArm");
    im.startEngine("kinect");

    im.cog = new GravityCenter(im);
    im.cog.setLinkMass("i01.torso.midStom", 2.832, 0.5);
    im.cog.setLinkMass("i01.torso.topStom", 5.774, 0.5);
    im.cog.setLinkMass("i01.leftArm.omoplate", 0.739, 0.5);
    im.cog.setLinkMass("i01.rightArm.omoplate", 0.739, 0.5);
    im.cog.setLinkMass("i01.leftArm.rotate", 0.715, 0.5754);
    im.cog.setLinkMass("i01.rightArm.rotate", 0.715, 0.5754);
    im.cog.setLinkMass("i01.leftArm.shoulder", 0.513, 0.5);
    im.cog.setLinkMass("i01.rightArm.shoulder", 0.513, 0.5);
    im.cog.setLinkMass("i01.leftArm.bicep", 0.940, 0.4559);
    im.cog.setLinkMass("i01.rightArm.bicep", 0.940, 0.4559);
    im.cog.setLinkMass("i01.leftHand.wrist", 0.176, 0.7474);
    im.cog.setLinkMass("i01.rightHand.wrist", 0.176, 0.7474);

    im.setJmeApp(vinMoovApp);
    im.setOpenni(openni);

  }

  //extra services used inside gestures, todo inmoov refactor attach things...
  public Relay LeftRelay1;
  public Relay RightRelay1;
  public NeoPixel neopixel;
  public Arduino neopixelArduino;
  public UltrasonicSensor ultrasonicSensor;

  public Double getUltrasonicSensorDistance() {
    if (ultrasonicSensor != null) {
      return ultrasonicSensor.range();
    } else {
      warn("No UltrasonicSensor attached");
      return 0.0;
    }
  }

  public void setNeopixelAnimation(String animation, Integer red, Integer green, Integer blue, Integer speed) {
    if (neopixel != null && neopixelArduino != null) {
      neopixel.setAnimation(animation, red, green, blue, speed);
    } else {
      warn("No Neopixel attached");
    }
  }

  public void stopNeopixelAnimation() {
    if (neopixel != null && neopixelArduino != null) {
      neopixel.animationStop();
    } else {
      warn("No Neopixel attached");
    }
  }

  @Override
  public void stopService() {
    super.stopService();

    RobotCanMoveRandom = false;
    stopTracking();
    halfSpeed();

    // if relay used, we switch on power
    if (LeftRelay1 != null) {
      LeftRelay1.on();
    }
    if (RightRelay1 != null) {
      RightRelay1.on();
    }
    rest();
    if (eyelids != null) {
      eyelids.autoBlink(false);
      eyelids.moveTo(180, 180);
    }
    setMute(false);
    speakBlocking(lang_shutDown);
    stopVinMoov();
    if (neopixel != null && neopixelArduino != null) {
      neopixel.animationStop();
      sleep(500);
      neopixel.detach(neopixelArduino);
      sleep(100);
      neopixelArduino.serial.disconnect();
      neopixelArduino.serial.stopRecording();
      neopixelArduino.disconnect();
    }
    sleep(4500);
    disable();
    if (LeftRelay1 != null) {
      LeftRelay1.off();
    }
    if (RightRelay1 != null) {
      RightRelay1.off();
    }
    // TODO better thing to detect connected arduinos
    // we cant use arduino.stopService()
    if (rightHand != null) {
      rightHand.arduino.serial.disconnect();
      rightHand.arduino.serial.stopRecording();
      rightHand.arduino.disconnect();
    }
    if (leftHand != null) {
      leftHand.arduino.serial.disconnect();
      leftHand.arduino.serial.stopRecording();
      leftHand.arduino.disconnect();
    }
  }
}
