package org.myrobotlab.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.kinematics.Pose;
import org.myrobotlab.kinematics.PoseMove;
import org.myrobotlab.kinematics.GesturePart;
import org.myrobotlab.kinematics.Gesture;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ServoMixerConfig;
import org.myrobotlab.service.interfaces.ServiceLifeCycleListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * ServoMixer - a service which can control multiple servos. The position of the
 * servos can be saved into multiple poses, and the poses in turn can be saved
 * in a gesture file to play back later.
 * 
 * TODO: refresh button
 * 
 * @author GroG, kwatters, others...
 * 
 */
public class ServoMixer extends Service implements ServiceLifeCycleListener {

  public class Player implements Runnable {
    boolean running = false;
    Gesture runningSeq = null;
    int seqCnt = 0;
    Thread thread = null;

    private void play() {
      if (runningSeq.parts != null) {
        for (int i = 0; i < runningSeq.parts.size(); ++i) {
          GesturePart sp = runningSeq.parts.get(i);
          invoke("publishPlayingGesturePart", sp);
          invoke("publishPlayingGesturePartIndex", i);
          switch(sp.type) {
            case "Pose":{
              Pose pose = getPose(sp.name);
              if (pose == null) {
                warn("Pose %s not found", sp.name);
                continue;
              }          
              // move to positions
              moveToPose(sp.name, pose, sp.blocking);

            }
            break;
            case "Delay":{
                sleep((Integer)sp.value);
            }
            break;
            case "Speech":{
              invoke("publishText", (String)sp.value);
            }
          break;            
            default:{
              error("do not know how to handle gesture part of type %s", sp.type);
            }
          }
        } // poses
      }
    }

    @Override
    public void run() {
      try {
        running = true;
        if (runningSeq.repeat) {
          while (running) {
            play();
          }
        } else {
          play();
        }
      } catch (Exception e) {
        error(e);
      }
      running = false;
    }

    public void start(Gesture seq) {
      runningSeq = seq;
      seqCnt++;
      thread = new Thread(this, String.format("%s-player-%d", getName(), seqCnt));
      thread.start();
    }

    public void stop() {
      running = false;
    }
  }
  
  /**
   * Speech publishes here - a SpeechSynthesis service could subscribe,
   * or ProgramAB
   * @param text
   * @return
   */
  public String publishText(String text) {
    return text;
  }

  /**
   * Current index of gesture being played
   * @param i
   * @return
   */
  public int publishPlayingGesturePartIndex(int i) {
    return i;
  }

  /**
   * Current gesture part being processed by player 
   * @param sp
   * @return
   */
  public GesturePart publishPlayingGesturePart(GesturePart sp) {
    return sp;
  }

  
  public final static Logger log = LoggerFactory.getLogger(InMoov2.class);

  private static final long serialVersionUID = 1L;

  /**
   * Set of servo names kept in sync with current registry
   */
  protected Set<String> allServos = new TreeSet<>();;

  /**
   * gesture player
   */
  protected transient Player player = new Player();

  protected String posesDir = getDataDir() + fs + "poses";

  protected String gesturesDir = getDataDir() + fs + "gestures";

  public ServoMixer(String n, String id) {
    super(n, id);
    new File(posesDir).mkdirs();
    new File(gesturesDir).mkdirs();
  }

  /**
   * general interface attach
   */
  @Override
  public void attach(Attachable attachable) {
    if (attachable instanceof Servo) {
      attachServo((Servo) attachable);
    }
  }

  /**
   * name attach "the best"
   */
  @Override
  public void attach(String name) {
    ServoMixerConfig c = (ServoMixerConfig)config;
    // FIXME - check type in registry, describe, or query ... make sure Servo
    // type..
    // else return error - should be type checking
    ServiceInterface si = Runtime.getService(name);
    if (si != null & "Servo".equals(si.getSimpleName())) {
      Servo servo = (Servo) Runtime.getService(name);
      if (c.autoDisable) {
        servo.setAutoDisable(true);
      }
      allServos.add(name);
    } else {
      error("servo %s not found", name);
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

  /**
   * Get a pose by name - name corresponds to the filename of the file in the
   * servoMixerDirectory
   * 
   * @param name
   *          name of the post to load.
   * 
   * @return the loaded pose object
   */
  public Pose getPose(String name) {
    
    try {

      String filename = new File(posesDir).getAbsolutePath() + File.separator + name + ".yml";
      log.info("loading pose name {}", filename);
      String yml = FileIO.toString(filename);
      return CodecUtils.fromYaml(yml, Pose.class);
      // pose = Pose.loadPose(filename);
      // broadcastState(); "maybe too chatty"
    } catch (Exception e) {
      error(e);
    }
    return null;
  }

  public List<String> getPoseFiles() {

    List<String> files = new ArrayList<>();
    if (!FileIO.checkDir(posesDir)) {
      return files;
    }

    File dir = new File(posesDir);

    if (!dir.exists() || !dir.isDirectory()) {
      error("%s not a valid directory", posesDir);
      return files;
    }
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
    return posesDir;
  }

  public Gesture getGesture(String name) {
    Gesture gesture = null;
    try {
      if (!FileIO.checkDir(gesturesDir)) {
        error("invalid poses directory %s", gesturesDir);
        return null;
      }
      String filename = new File(gesturesDir).getAbsolutePath() + File.separator + name + ".yml";
      log.info("Loading Pose name {}", filename);
      gesture = CodecUtils.fromYaml(FileIO.toString(filename), Gesture.class);
      return gesture;
    } catch (Exception e) {
      error(e);
    }
    return gesture;
  }

  public List<String> getGestureFiles() {

    List<String> files = new ArrayList<>();
    if (!FileIO.checkDir(gesturesDir)) {
      error("gestures %s directory does not exist", gesturesDir);
      return files;
    }

    File dir = new File(gesturesDir);
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

  public List<ServoControl> listAllServos() {
    ArrayList<ServoControl> servos = new ArrayList<ServoControl>();
    // TODO: get a list of all servos
    for (ServiceInterface service : Runtime.getServices()) {
      if (service instanceof ServoControl) {
        servos.add((ServoControl) service);
      }
    }
    return servos;
  }

  public void moveToPose(String name, Pose p, boolean blocking) {
    try {
      
      if (p.moves == null) {
        error("no moves within pose file %s", name);
        return;
      }
      
      for (String sc : p.moves.keySet()) {
        PoseMove pm = p.moves.get(sc);
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

  public void moveToPose(String name) throws IOException {
    Pose p = getPose(name);
    if (p == null) {
      error("cannot find pose %s", name);
    }
    moveToPose(name, p, false);
  }

  @Override
  public void onCreated(String name) {
  }

  @Override
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
   * handles incoming selected events and publishes
   * search events
   * @param selected
   */
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
    Gesture seq = (Gesture) broadcast("getGesture", name);
    player.start(seq);
  }

  public String publishPlayingPose(String name) {
    return name;
  }

  public String publishStopPose(String name) {
    return name;
  }

  public void removePose(String name) {
    try {
      if (!FileIO.checkDir(posesDir)) {
        error("invalid poses directory %s", posesDir);
        return;
      }
      String filename = new File(posesDir).getAbsolutePath() + File.separator + name + ".yml";
      File del = new File(filename);
      if (del.exists()) {
        del.delete();
      } else {
        error("could not delete file %s", filename);
      }
      invoke("getPoseFiles");
    } catch (Exception e) {
      error(e);
    }
  }

  public void removeGesture(String name) {
    try {
      if (!FileIO.checkDir(gesturesDir)) {
        error("invalid poses directory %s", gesturesDir);
        return;
      }
      String filename = new File(gesturesDir).getAbsolutePath() + File.separator + name + ".yml";
      File del = new File(filename);
      if (del.exists()) {
        del.delete();
      }
      invoke("getGestureFiles");
    } catch (Exception e) {
      error(e);
    }
  }

  public void rest() {
    for(String servo : allServos) {
      Servo s = (Servo)Runtime.getService(servo);
      s.rest();
    }
  }

  /**
   * Save a {name}.yml file to the current poses directory.
   * 
   * @param name
   *          name to save pose as
   * @throws IOException
   *           boom
   * 
   */
  public void savePose(String name) throws IOException {
    savePose(name, allServos);
  }
  
  public void savePose(String name, Set<String> servos) throws IOException {

    if (servos == null) {
      log.error("cannot save %s null servos");
      return;
    }
        
    Pose pose = new Pose();
    
    for (String servo : servos) {
      ServoControl sc = (ServoControl)Runtime.getService(servo);
      if (sc == null) {
        error("servo %s null", name);
        continue;
      } else {
        pose.moves.put(CodecUtils.shortName(servo), new PoseMove(sc.getCurrentInputPos(), sc.getSpeed()));
      }
    }

    log.info("saving pose name {}", name);

    String filename = posesDir + File.separator + name + ".yml";
    FileIO.toFile(filename, CodecUtils.toYaml(pose).getBytes());
    invoke("getPoseFiles");
  }
  
  /**
   * Explicitly saving a new gesture file. This will error if the
   * file already exists. The gesture part moves will be empty.
   * @param filename
   * @return
   */
  public String addNewGestureFile(String filename) {
    if (filename == null) {
      error("filename cannot be null");
      return null;
    }
    if (!filename.toLowerCase().endsWith(".yml")) {
      filename += ".yml"; 
    }
    if (FileIO.checkFile(filename)) {
      error("file %s already exists", filename);
      return null;
    }
    saveGesture(filename, new Gesture());    
    return filename;
  }
  
  /**
   * Takes name of a file and a json encoded string of a gesture, saves it to
   * file and sets the "current" gesture to the data
   * 
   * @param filename
   *          the filename to save the gesture as
   * @param json
   *          the json to save
   */
  public void saveGesture(String filename, Gesture gesture) {
    try {
      if (filename == null) {
        error("save gesture file name cannot be null");
        return;
      }

      if (gesture == null) {
        error("gesture json cannot be null");
        return;
      }

      if (!filename.toLowerCase().endsWith(".yml")) {
        filename += ".yml";
      }

      // Gesture seq = CodecUtils.fromJson(json, Gesture.class);
      if (gesture != null) {
        String path = gesturesDir + fs + filename;
        FileOutputStream fos = new FileOutputStream(path);
        fos.write(CodecUtils.toYaml(gesture).getBytes());
        fos.close();
      } 
      invoke("getGestureFiles");
    } catch (Exception e) {
      error(e);
    }
  }

  /**
   * publishing a search to allow UI searching based on other
   * service selections
   * @param servos
   * @return
   */
  public String search(String servos) {
    return servos;
  }

  public void setAutoDisable(boolean b) {
    ServoMixerConfig c = (ServoMixerConfig)config;
    c.autoDisable = b;
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
    this.posesDir = posesDirectory;
    invoke("getPoseFiles");
    broadcastState();
  }

  @Override
  public void startService() {
    try {
      Runtime.getInstance().attachServiceLifeCycleListener(getName());
      
      List<String> all = Runtime.getServiceNamesFromInterface(ServoControl.class);
      for (String sc : all) {
        attach(sc);
      }

      File poseDirectory = new File(posesDir);
      if (!poseDirectory.exists()) {
        poseDirectory.mkdirs();
      }
      super.startService();
    } catch (Exception e) {
      error(e);
    }
  }
  
  public static void main(String[] args) throws Exception {

    try {
      Runtime.main(new String[] { "--id", "admin"});
      LoggingFactory.init("INFO");

      Runtime.start("i01.head.rothead", "Servo");
      Runtime.start("i01.head.neck", "Servo");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      Python python = (Python) Runtime.start("python", "Python");
      ServoMixer mixer = (ServoMixer) Runtime.start("mixer", "ServoMixer");
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
