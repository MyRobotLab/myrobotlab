package org.myrobotlab.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.kinematics.Action;
import org.myrobotlab.kinematics.Gesture;
import org.myrobotlab.kinematics.Pose;
import org.myrobotlab.kinematics.PoseMove;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServoMixerConfig;
import org.myrobotlab.service.interfaces.SelectListener;
import org.myrobotlab.service.interfaces.ServiceLifeCycleListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

/**
 * ServoMixer - a service which can control multiple servos. The position of the
 * servos can be saved into multiple poses, and the poses in turn can be saved
 * in a gesture file to play back later.
 * 
 * @author GroG, kwatters, others...
 * 
 */
public class ServoMixer extends Service<ServoMixerConfig> implements ServiceLifeCycleListener, SelectListener {

  public class PlayingGesture {
    public String name;
    public Gesture gesture;
    public int startIndex = 0;

    public PlayingGesture(String name, Gesture gesture) {
      this(name, gesture, 0);
    }

    public PlayingGesture(String name, Gesture gesture, int index) {
      this.name = name;
      this.gesture = gesture;
      this.startIndex = index;
    }

    public String toString() {
      int actionCnt = 0;
      if (gesture != null && gesture.actions != null) {
        actionCnt = gesture.actions.size();
      }
      return String.format("name:%s actionCnt:%d index:%d", name, actionCnt, startIndex);
    }

  }

  /**
   * The Player plays a requested gesture, which is a sequence of Poses. Poses
   * can be positions, delays, or speech. It publishes when it starts a gesture
   * and when its finished with a gesture. All poses within the gesture are
   * published as well. This potentially will provide some measure of safety if
   * servos should not accept other input when doing a gesture.
   *
   */
  public class Player implements Runnable {
    protected String playingGesture = null;
    protected boolean running = false;
    transient private ExecutorService executor;
    transient Deque<PlayingGesture> playStack = new ArrayDeque<>();

    // FIXME - add optional start index to start playing at a midpoint
    private void play() {
      try {
        PlayingGesture current = playStack.pop();
        invoke("publishGestureStarted", current.name);
        while (current != null) {
          playingGesture = current.name;

          if (current.gesture.actions != null) {
            for (int i = current.startIndex; i < current.gesture.actions.size(); ++i) {
              if (!running) {
                break;
              }

              processAction(current, i);

            } // poses
          }
          if (!playStack.isEmpty()) {
            current = playStack.pop();
          } else {
            current = null;
          }
        }
        invoke("publishGestureStopped", playingGesture);
      } catch (Exception e) {
        error(e);
      }
      running = false;
    }

    public void processAction(PlayingGesture current, int i) {
      Action action = current.gesture.actions.get(i);
      invoke("publishPlayingAction", action);
      invoke("publishPlayingActionIndex", i);
      switch (action.type) {
        case "moveTo": {
          // process a move
          Map<String, Map<String, Object>> moves = (Map) action.value;

          // do the moves
          for (String servoName : moves.keySet()) {
            Map<String, Object> move = moves.get(servoName);
            moveTo(servoName, move);
          }

          // Boolean blocking = (Boolean) move.get("blocking");
          boolean anyServoMoving = true;

          // wait until all servos stop
          while (anyServoMoving) {
            // start by assuming all are not moving
            anyServoMoving = false;
            log.error("======starting moving=========");
            for (String servoName : moves.keySet()) {
              ServoControl servo = (ServoControl) Runtime.getService(servoName);
              if (servo == null || !servo.isMoving()) {
                log.error("{} not moving", servoName);
              } else {
                log.error("{} moving sleeping", servoName);
                anyServoMoving = true;
              }
            }

            if (anyServoMoving) {
              log.error("sleeping 20 ms");
              sleep(20);
            } else {
              log.error("======done with move=========");              
            }
          }
        }
          break;
        case "gesture": {

          // save current place
          current.startIndex = i + 1; // Obiwan error prolly
          playStack.add(current);

          // read in new gesture
          String gestureName = (String) action.value;
          // Gesture embedded = getGesture(gestureName);
          Gesture embedded = (Gesture) invoke("getGesture", gestureName);
          if (embedded == null) {
            error("embedded gesture %s was not found", gestureName);
            break;
          }
          // insert actions ?
          // TODO - check for infinite recursion ... history, if your
          // new gesture is already on the playstack

          // replace current with new embedded gesture
          // index reset
          i = -1;
          current = new PlayingGesture(gestureName, embedded, i);
        }
          break;
        case "sleep": {
          sleep(Math.round((double) action.value * 1000));
        }
          break;
        case "speak": {
          speak((Map) action.value);
        }
          break;
        default: {
          error("do not know how to handle gesture part of type %s", action.type);
        }
      }
    }

    @Override
    public void run() {
      try {
        running = true;
        // FIXME - repeat needs to be handled outside of gesture
        // e.g. play(name, repeat)
        // if (runningGesture.repeat) {
        // while (running) {
        // play();
        // }
        // } else {
        play();
        // }
      } catch (Exception e) {
        error(e);
      }
      running = false;
    }

    public void start(String name, Gesture seq) {
      // FIXME - make Copy ? because gestures within gestures will be mods
      playStack.add(new PlayingGesture(name, seq));
      executor = Executors.newSingleThreadExecutor();
      executor.execute(this::run);
    }

    public void stop() {
      if (executor != null) {
        executor.shutdownNow();
      }
      running = false;
      invoke("publishGestureStopped", playingGesture);
    }
  }

  public final static Logger log = LoggerFactory.getLogger(InMoov2.class);

  private static final long serialVersionUID = 1L;

  /**
   * Set of servo names kept in sync with current registry
   */
  protected Set<String> allServos = new TreeSet<>();

  // TODO selected servos

  /**
   * current gesture being edited
   */
  protected Gesture currentGesture = null;

  /**
   * gesture player
   */
  final protected transient Player player = new Player();

  /**
   * gesture name of the currentGesture
   */
  protected String currentEditGestureName = null;

  public ServoMixer(String n, String id) {
    super(n, id);
  }

  /**
   * Explicitly saving a new gesture file. This will error if the file already
   * exists. The gesture part moves will be empty.
   * 
   * @param filename
   * @return
   */
  public String addNewGestureFile(String name) {
    if (name == null) {
      error("name cannot be null");
      return null;
    }

    String filename = null;
    if (!name.toLowerCase().endsWith(".yml")) {
      filename = name + ".yml";
    } else {
      filename = name;
    }

    String path = config.gesturesDir + fs + filename;

    if (FileIO.checkFile(path)) {
      error("file %s already exists", filename);
      return null;
    }
    saveGesture(filename, new Gesture());
    invoke("getGesture", name);
    return filename;
  }

  public void saveGesture(String name) {
    // FIXME - warn if overwrite
    // FIXME - don't warn if opened
    saveGesture(name, currentGesture);
  }

  /**
   * general interface attach
   */
  @Override
  public void attach(Attachable attachable) {
    if (attachable instanceof Servo) {
      attachServo((Servo) attachable);
    }
  };

  /**
   * name attach "the best"
   */
  @Override
  public void attach(String name) {

    // FIXME - check type in registry, describe, or query ... make sure Servo
    // type..
    // else return error - should be type checking
    ServiceInterface si = Runtime.getService(name);
    if (si != null & "Servo".equals(si.getSimpleName())) {
      Servo servo = (Servo) Runtime.getService(name);
      if (config.autoDisable) {
        servo.setAutoDisable(true);
      }
      allServos.add(name);
    } else {
      log.info("do not know how to attach {}", name);
    }
  }

  /**
   * typed attach
   * 
   * @param servo
   *          the servo to attach
   * 
   */
  public void attachServo(Servo servo) {
    attach(servo.getName());
  }

  public Gesture getGesture(String name) {

    Gesture gesture = null;

    try {
      // if name is null or currentEditGestureName return current gesture
      if (name == null || name.equals(currentEditGestureName)) {
        gesture = currentGesture;
        return gesture;
      }

      // switching gestures

      if (!FileIO.checkDir(config.gesturesDir)) {
        error("invalid poses directory %s", config.gesturesDir);
        return null;
      }
      String filename = new File(config.gesturesDir).getAbsolutePath() + File.separator + name + ".yml";
      log.info("loading gesture {}", filename);
      gesture = CodecUtils.fromYaml(FileIO.toString(filename), Gesture.class);

      // FIXME - replacing current gesture .. if DIRTY edits - warn !!!
      if (currentEditGestureName != null) {
        saveGesture(currentEditGestureName);
      }

      currentGesture = gesture;
      currentEditGestureName = name;

      return gesture;
    } catch (FileNotFoundException e) {
      info("file %s.yml not found", name);
    } catch (Exception e) {
      error(e);
    }
    return gesture;
  }

  public List<String> getGestureFiles() {

    List<String> files = new ArrayList<>();
    if (!FileIO.checkDir(config.gesturesDir)) {
      error("gestures %s directory does not exist", config.gesturesDir);
      return files;
    }

    File dir = new File(config.gesturesDir);
    File[] all = dir.listFiles();
    Set<String> sorted = new TreeSet<>();
    for (File f : all) {
      if (f.getName().toLowerCase().endsWith(".yml")) {
        sorted.add(f.getName().substring(0, f.getName().lastIndexOf(".")));
      }
    }
    for (String s : sorted) {
      files.add(s);
    }
    return files;
  }

  public String getPosesDirectory() {
    return config.posesDir;
  }

  public List<ServoControl> listAllServos() {
    ArrayList<ServoControl> servos = new ArrayList<ServoControl>();
    for (ServiceInterface service : Runtime.getServices()) {
      if (service instanceof ServoControl) {
        servos.add((ServoControl) service);
      }
    }
    return servos;
  }

  public void step(int index) {
    step(currentEditGestureName, index);
  }

  public void step(String gestureName, int index) {

    if (gestureName == null) {
      error("gesture name cannot be null");
      return;
    }

    if (!gestureName.equals(currentEditGestureName)) {
      // load gesture
      getGesture(gestureName);
    }

    if (currentGesture == null) {
      error("gesture cannot be nulle");
      return;
    }

    player.processAction(new PlayingGesture(gestureName, currentGesture), index);
    // step to next action
    index++;
    if (index < currentGesture.actions.size()) {
      Action action = currentGesture.actions.get(index);
      invoke("publishPlayingAction", action);
      invoke("publishPlayingActionIndex", index);
    }
  }

  public void moveToPose(String name, Pose p, boolean blocking) {
    try {

      if (p.getMoves() == null) {
        error("no moves within pose file %s", name);
        return;
      }

      for (String sc : p.getMoves().keySet()) {
        PoseMove pm = p.getMoves().get(sc);
        ServoControl servo = (ServoControl) Runtime.getService(sc);
        if (servo == null) {
          warn("servo (%s) cannot move to pose because it does not exist", sc);
          continue;
        }
        Double speed = pm.speed;
        Double position = pm.position;
        servo.setSpeed(speed);
        if (blocking) {
          servo.moveToBlocking(position);
        } else {
          servo.moveTo(position);
        }
      }
      invoke("publishStopPose", name);
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public void onCreated(String name) {
  }

  @Override /* FIXME have a simple onStarted onRelease */
  public void onRegistered(Registration registration) {
  }

  /**
   * Part of service life cycle - a servo has been removed from the system
   */
  @Override
  public void onReleased(String name) {
    allServos.remove(name);
  }

  /**
   * handles incoming selected events and publishes search events
   * 
   * @param selected
   */
  @Override
  public void onSelected(String selected) {
    invoke("search", selected);
  }

  /**
   * Part of service life cycle - a new servo has been started
   */
  @Override
  public void onStarted(String name) {
    try {
      attach(name);
    } catch (Exception e) {
      log.error("onStarted threw", e);
    }
  }

  @Override
  public void onStopped(String name) {
  }

  public void playGesture(String name) {
    // Gesture gesture = (Gesture) broadcast("getGesture", name);
    invoke("getGesture", name);
    Gesture gesture = getGesture(name);
    player.start(name, gesture);
  }

  /**
   * When a gesture starts its name will be published
   * 
   * @param name
   * @return
   */
  public String publishGestureStarted(String name) {
    return name;
  }

  /**
   * When a gesture stops its name will be published
   * 
   * @param name
   * @return
   */
  public String publishGestureStopped(String name) {
    return name;
  }

  /**
   * Current gesture part being processed by player
   * 
   * @param sp
   * @return
   */
  public Action publishPlayingAction(Action sp) {
    return sp;
  }

  /**
   * Current index of gesture being played
   * 
   * @param i
   * @return
   */
  public int publishPlayingActionIndex(int i) {
    return i;
  }

  public String publishPlayingPose(String name) {
    return name;
  }

  public String publishStopPose(String name) {
    return name;
  }

  /**
   * Speech publishes here - a SpeechSynthesis service could subscribe, or
   * ProgramAB
   * 
   * @param text
   * @return
   */
  public String publishText(String text) {
    return text;
  }

  public void removeGesture(String name) {

    try {
      if (!FileIO.checkDir(config.gesturesDir)) {
        error("invalid poses directory %s", config.gesturesDir);
        return;
      }
      String filename = new File(config.gesturesDir).getAbsolutePath() + File.separator + name + ".yml";
      File del = new File(filename);
      if (del.exists()) {
        if (del.delete()) {
          currentEditGestureName = null;
          currentGesture = null;
        }
      }
      invoke("getGestureFiles");
    } catch (Exception e) {
      error(e);
    }
  }

  public void moveActionUp(int index) {
    if (currentGesture == null) {
      error("cannot move gesture not set");
      return;
    }

    List<Action> list = currentGesture.actions;

    if (index > 0 && index < list.size()) {
      Action action = list.remove(index);
      list.add(index - 1, action);
    } else {
      error("index out of range or at the beginning of the list.");
    }
    invoke("getGesture");
  }

  public void moveActionDown(int index) {
    if (currentGesture == null) {
      error("cannot move: gesture not set");
      return;
    }

    List<Action> list = currentGesture.actions;

    if (index >= 0 && index < list.size() - 1) {
      Action action = list.remove(index);
      list.add(index + 1, action);
    } else {
      error("index out of range or at the end of the list.");
    }
    invoke("getGesture");
  }

  public void removeActionFromGesture(int index) {
    try {

      Gesture gesture = getGesture();
      if (gesture == null) {
        error("gesture not set");
        return;
      }
      if (gesture.actions.size() != 0 && index < gesture.actions.size()) {
        gesture.actions.remove(index);
      }

    } catch (Exception e) {
      error(e);
    }
  }

  public void rest() {
    for (String servo : allServos) {
      Servo s = (Servo) Runtime.getService(servo);
      s.rest();
    }
  }

  /**
   * Takes name of a file and a json encoded string of a gesture, saves it to
   * file and sets the "current" gesture to the data
   * 
   * @param filename
   *          the filename to save the gesture as
   * @param gesture
   *          the gesture to save
   */
  public void saveGesture(String filename, Gesture gesture) {
    try {

      if (filename == null) {
        error("save gesture file name cannot be null");
        return;
      }

      if (gesture == null) {
        log.info("creating empty gesture");
        gesture = new Gesture();
      }

      if (!filename.toLowerCase().endsWith(".yml")) {
        filename += ".yml";
      }

      if (gesture != null) {
        String path = config.gesturesDir + fs + filename;
        FileOutputStream fos = new FileOutputStream(path);
        fos.write(CodecUtils.toYaml(gesture).getBytes());
        fos.close();
      }
      info("saved gesture %s", filename);
      invoke("getGestureFiles");
    } catch (Exception e) {
      error(e);
    }
  }

  /**
   * publishing a search to allow UI searching based on other service selections
   * 
   * @param servos
   * @return
   */
  public String search(String servos) {
    return servos;
  }

  public void setAutoDisable(boolean b) {

    config.autoDisable = b;
    if (b) {
      List<String> servos = Runtime.getServiceNamesFromInterface(Servo.class);
      for (String name : servos) {
        Servo servo = (Servo) Runtime.getService(name);
        servo.setAutoDisable(true);
      }
    } else {
      List<String> servos = Runtime.getServiceNamesFromInterface(Servo.class);
      for (String name : servos) {
        Servo servo = (Servo) Runtime.getService(name);
        servo.setAutoDisable(false);
      }
    }
  }

  public void setPosesDirectory(String posesDirectory) {

    File dir = new File(posesDirectory);
    if (!dir.exists()) {
      dir.mkdirs();
    }
    config.posesDir = posesDirectory;
    broadcastState();
  }

  @Override
  public void startService() {
    try {
      new File(config.posesDir).mkdirs();
      new File(config.gesturesDir).mkdirs();

      Runtime.getInstance().attachServiceLifeCycleListener(getName());

      List<String> all = Runtime.getServiceNamesFromInterface(ServoControl.class);
      for (String sc : all) {
        attach(sc);
      }

      File poseDirectory = new File(config.posesDir);
      if (!poseDirectory.exists()) {
        poseDirectory.mkdirs();
      }
      super.startService();
    } catch (Exception e) {
      error(e);
    }
  }

  /**
   * stop the current running gesture
   */
  public void stop() {
    player.stop();
  }

  @Override
  public void stopService() {
    super.stopService();
    player.stop();
  }

  public static void main(String[] args) throws Exception {

    try {
      Runtime.main(new String[] { "--id", "admin" });
      LoggingFactory.init("INFO");

      Runtime.start("i01.head.rothead", "Servo");
      Runtime.start("i01.head.neck", "Servo");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      Python python = (Python) Runtime.start("python", "Python");
      ServoMixer mixer = (ServoMixer) Runtime.start("mixer", "ServoMixer");
      // mixer.playGesture("");

      boolean done = true;
      if (done) {
        return;
      }

      mixer.addNewGestureFile("test");
      Gesture gesture = mixer.getGesture("test");
      String gestureName = "aaa";
      String servoName = "i01.head.rothead";
      Double position = 90.0;
      Double speed = null;

      Map<String, Map<String, Object>> moves = new TreeMap<>();

      Map<String, Object> poseMove1 = new TreeMap<>();
      poseMove1.put("position", 90.0);

      Map<String, Object> poseMove2 = new TreeMap<>();
      poseMove2.put("position", 90);
      poseMove2.put("speed", 35.0);

      moves.put("i01.head.rothead", poseMove1);
      moves.put("i01.head.neck", poseMove2);

      mixer.openGesture(gestureName);
      mixer.addMoveToAction(moves); // autofill delay from keyframe ?
      mixer.saveGesture();

      mixer.addNewGestureFile("test2");

      // mixer.save(gestureName);
      mixer.addGestureToAction("test");

      mixer.saveGesture();

      // mixer.setPose("test", )
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public void addGestureToAction(String gestureName) {
    addGestureToAction(gestureName, null);
  }

  public void addGestureToAction(String gestureName, Integer index) {
    addAction(Action.createGestureToAction(gestureName), index);
  }

  public void saveGesture() {
    // FIXME check if exists - ask overwrite
    saveGesture(currentEditGestureName, currentGesture);
  }

  public Gesture openGesture(String name) {
    if (currentGesture != null) {
      warn("replacing current gesture");
      // prompt user return null etc.
    }

    Gesture gesture = getGesture(name);
    if (gesture == null) {
      // gesture not found make new
      gesture = new Gesture();
    }

    currentEditGestureName = name;
    currentGesture = gesture;

    return currentGesture;
  }

  public void addSpeakAction(Map<String, Object> speechCommand) {
    addSpeakAction(speechCommand, null);
  }

  public void addSpeakAction(Map<String, Object> speechCommand, Integer index) {
    addAction(Action.createSpeakAction(speechCommand), index);
  }

  public void addMoveToAction(List<String> servos) {
    addMoveToAction(servos, null);
  }

  /**
   * list of servos to add to an action - they're current speed and input
   * positions will be added at index
   * 
   * @param servos
   * @param index
   */
  public void addMoveToAction(List<String> servos, Integer index) {
    Map<String, Map<String, Object>> moves = new TreeMap<>();
    for (String servoName : servos) {
      ServoControl sc = (ServoControl) Runtime.getService(servoName);
      Map<String, Object> posAndSpeed = new TreeMap<>();
      if (sc == null) {
        error("%s not a valid service name", servoName);
        continue;
      }
      posAndSpeed.put("position", sc.getCurrentInputPos());
      posAndSpeed.put("speed", sc.getSpeed());
      moves.put(servoName, posAndSpeed);
    }
    addAction(Action.createMoveToAction(moves), index);
  }

  public void addMoveToAction(Map<String, Map<String, Object>> moves) {
    addMoveToAction(moves, null);
  }

  public void addMoveToAction(Map<String, Map<String, Object>> moves, Integer index) {
    addAction(Action.createMoveToAction(moves), index);
  }

  public void addAction(Action action, Integer index) {
    if (currentGesture == null) {
      error("current gesture not set");
      return;
    }
    if (index != null) {
      if (currentGesture.actions.size() == 0) {
        currentGesture.actions.add(action);
      } else {
        currentGesture.actions.add(index, action);
      }
    } else {
      currentGesture.actions.add(action);
    }
  }

  public void addSleepAction(double sleep) {
    addSleepAction(sleep, null);
  }

  public void addSleepAction(double sleep, Integer index) {
    addAction(Action.createSleepAction(sleep), index);
  }

  public Gesture getGesture() {
    return currentGesture;
  }

  private void moveTo(String servoName, Map<String, Object> move) {
    ServoControl servo = (ServoControl) Runtime.getService(servoName);
    if (servo == null) {
      warn("servo (%s) cannot move to pose because it does not exist", servoName);
      return;
    }

    Object speed = move.get("speed");
    if (speed != null) {
      if (speed instanceof Integer) {
        servo.setSpeed((Integer) speed);
      } else if (speed instanceof Double) {
        servo.setSpeed((Double) speed);
      }
    }

    Object position = move.get("position");
    if (position instanceof Integer) {
      servo.moveTo((Integer) position);
    } else if (position instanceof Double) {
      servo.moveTo((Double) position);
    }
  }

  private void speak(Map<String, Object> speechPart) {
    if (config.mouth == null) {
      warn("mouth configuration not set");
      return;
    }
    SpeechSynthesis mouth = (SpeechSynthesis) Runtime.getService(config.mouth);
    if (mouth == null) {
      error("%s speech synthesis service missing", config.mouth);
      return;
    }
    try {
      // makes it harder to block
      // FIXME if blocking send(mouthName, "speak")
      // TODO - show multiple SpeechSynthesis select like Servos
      Boolean blocking = (Boolean) speechPart.get("blocking");
      // if (blocking != null && blocking) {
      mouth.speakBlocking((String) speechPart.get("text")); // default blocking
      // } else {
      // mouth.speak((String) speechPart.get("text"));
      // }
    } catch (Exception e) {
      error(e);
    }

  }

}
