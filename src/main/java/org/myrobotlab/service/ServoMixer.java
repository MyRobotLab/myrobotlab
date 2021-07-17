package org.myrobotlab.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.kinematics.Pose;
import org.myrobotlab.kinematics.PoseSequence;
import org.myrobotlab.kinematics.Sequence;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

/**
 * ServoMixer - a service which can control multiple servos. The position of the
 * servos can be saved into multiple poses, and the poses in turn can be saved
 * in a sequence file to play back later.
 */
public class ServoMixer extends Service {

  public final static Logger log = LoggerFactory.getLogger(InMoov2.class);

  private static final long serialVersionUID = 1L;

  protected String servoMixerDirectory = getDataDir() + fs + "poses";

  /**
   * set autoDisable on "all" servos .. true - will make all servos autoDisable
   * false - will make all servos autoDisable false null - will make no changes
   */
  protected Boolean autoDisable = null;;

  /**
   * sequence player
   */
  protected transient Player player = new Player();

  /**
   * Set of name kept in sync with current registry
   */
  protected TreeSet<String> allServos = new TreeSet<>();

  public ServoMixer(String n, String id) {
    super(n, id);
  }

  /**
   * name attach "the best"
   */
  public void attach(String name) {
    // FIXME - check type in registry, describe, or query ... make sure Servo
    // type..
    // else return error - should be type checking
    ServiceInterface si = Runtime.getService(name);
    if (si != null & "Servo".equals(si.getSimpleName())) {
      Servo servo = (Servo) Runtime.getService(name);
      if (autoDisable != null) {
        servo.setAutoDisable(autoDisable);
      }
      allServos.add(name);
    }
  }

  /**
   * general interface attach
   */
  public void attach(Attachable attachable) {
    if (attachable instanceof Servo) {
      attachServo((Servo) attachable);
    }
  }

  /**
   * typed attach
   * 
   */
  public void attachServo(Servo servo) {
    attach(servo.getName());
  }

  /**
   * Part of service life cycle - a new servo has been started
   */
  public void onStarted(String name) {
    try {
      attach(name);
    } catch (Exception e) {
      log.error("onStarted threw", e);
    }
  }

  /**
   * Part of service life cycle - a servo has been removed from the system
   */
  public void onReleased(String name) {
    allServos.remove(name);
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

  /**
   * Save a {name}.pose file to the current poses directory.
   * 
   */
  public void savePose(String name) throws IOException {
    savePose(name, null);
  }

  public void savePose(String name, List<ServoControl> servos) throws IOException {

    if (servos == null) {
      servos = listAllServos();
    }

    File poseDirectory = new File(servoMixerDirectory);
    poseDirectory.mkdirs();

    log.info("Saving pose name {}", name);
    Pose p = new Pose(name, servos);
    String filename = poseDirectory.getAbsolutePath() + File.separator + name + ".pose";
    p.savePose(filename);
    broadcast("getPoseFiles");
  }

  private boolean checkDir(String dir) {
    try {
      File check = new File(dir);
      return check.exists();
    } catch (Exception e) {
      error(e);
    }
    return false;
  }

  @Deprecated /* use getPose(name) */
  public Pose loadPose(String name) {
    return getPose(name);
  }

  /**
   * Get a pose by name - name corresponds to the filename of the file in the
   * servoMixerDirectory
   * 
   * @return the loaded pose object
   */
  public Pose getPose(String name) {
    Pose pose = null;
    try {
      if (!checkDir(servoMixerDirectory)) {
        error("invalid poses directory %s", servoMixerDirectory);
        return null;
      }
      String filename = new File(servoMixerDirectory).getAbsolutePath() + File.separator + name + ".pose";
      log.info("Loading Pose name {}", filename);
      pose = Pose.loadPose(filename);
      // broadcastState(); "maybe too chatty"
    } catch (Exception e) {
      error(e);
    }
    return pose;
  }

  public void removePose(String name) {
    try {
      if (!checkDir(servoMixerDirectory)) {
        error("invalid poses directory %s", servoMixerDirectory);
        return;
      }
      String filename = new File(servoMixerDirectory).getAbsolutePath() + File.separator + name + ".pose";
      File del = new File(filename);
      if (del.exists()) {
        del.delete();
      }
      invoke("getPoseFiles");
    } catch (Exception e) {
      error(e);
    }
  }

  public void removeSequence(String name) {
    try {
      if (!checkDir(servoMixerDirectory)) {
        error("invalid poses directory %s", servoMixerDirectory);
        return;
      }
      String filename = new File(servoMixerDirectory).getAbsolutePath() + File.separator + name + ".seq";
      File del = new File(filename);
      if (del.exists()) {
        del.delete();
      }
      invoke("getSequenceFiles");
    } catch (Exception e) {
      error(e);
    }
  }

  public Sequence getSequence(String name) {
    Sequence pose = null;
    try {
      if (!checkDir(servoMixerDirectory)) {
        error("invalid poses directory %s", servoMixerDirectory);
        return null;
      }
      String filename = new File(servoMixerDirectory).getAbsolutePath() + File.separator + name + ".seq";
      log.info("Loading Pose name {}", filename);
      pose = Sequence.loadSequence(filename);
      broadcastState();
    } catch (Exception e) {
      error(e);
    }
    return pose;
  }

  /**
   * Export Python representation of a sequence
   * 
   */
  public String exportSequence(String name) {
    return null;
  }

  public class Player implements Runnable {
    Thread thread = null;
    Sequence runningSeq = null;
    int seqCnt = 0;
    boolean running = false;

    @Override
    public void run() {
      try {
        running = true;
        if (runningSeq.cycle) {
          while (running) {
            play();
          }
        } else {
          play();
        }
      } catch (Exception e) {
      }
      running = false;
    }

    public void stop() {
      running = false;
    }

    private void play() {
      if (runningSeq.poses != null) {
        for (PoseSequence ps : runningSeq.poses) {
          Pose pose = getPose(ps.name);
          if (pose == null) {
            warn("Pose %s not found", ps.name);
            continue;
          }
          if (ps.waitTimeMs != null) {
            sleep(ps.waitTimeMs);
          }
          // move to positions
          Pose p = getPose(ps.name);
          moveToPose(p);
        } // poses
      }
    }

    public void start(Sequence seq) {
      runningSeq = seq;
      seqCnt++;
      thread = new Thread(this, String.format("%s-player-%d", getName(), seqCnt));
      thread.start();
    }
  }

  public String publishPlayingPose(String name) {
    return name;
  }

  public String publishStopPose(String name) {
    return name;
  }

  public void playSequence(String name) {
    Sequence seq = (Sequence) broadcast("getSequence", name);
    player.start(seq);
  }

  public void moveToPose(Pose p) {
    try {
      invoke("publishPlayingPose", p.name);
      for (String sc : p.getPositions().keySet()) {
        ServoControl servo = (ServoControl) Runtime.getService(sc);
        if (servo == null) {
          warn("servo (%s) cannot move to pose because it does not exist", sc);
          continue;
        }
        Double speed = p.getSpeeds().get(sc);
        Double position = p.getPositions().get(sc);
        servo.setSpeed(speed);
        // servo.broadcastState(); WAY TOO CHATTY
        // servo.moveToBlocking(position); // WOAH - sequential movements :P
        servo.moveTo(position);
      }
      invoke("publishStopPose", p.name);
    } catch (Exception e) {
      error(e);
    }
  }

  public void moveToPose(String name) throws IOException {
    Pose p = loadPose(name);
    moveToPose(p);
  }

  public String getPosesDirectory() {
    return servoMixerDirectory;
  }

  public void setPosesDirectory(String posesDirectory) {
    File dir = new File(posesDirectory);
    if (!dir.exists()) {
      dir.mkdirs();
    }
    this.servoMixerDirectory = posesDirectory;
    invoke("getPoseFiles");
    broadcastState();
  }

  public List<String> getPoseFiles() {

    List<String> files = new ArrayList<>();
    if (!checkDir(servoMixerDirectory)) {
      return files;
    }

    File dir = new File(servoMixerDirectory);

    if (!dir.exists() || !dir.isDirectory()) {
      error("%s not a valid directory", servoMixerDirectory);
      return files;
    }
    File[] all = dir.listFiles();
    Set<String> sorted = new TreeSet<>();
    for (File f : all) {
      if (f.getName().toLowerCase().endsWith(".pose")) {
        sorted.add(f.getName().substring(0, f.getName().lastIndexOf(".")));
      }
    }
    for (String s : sorted) {
      files.add(s);
    }
    return files;
  }

  public List<String> getSequenceFiles() {

    List<String> files = new ArrayList<>();
    if (!checkDir(servoMixerDirectory)) {
      return files;
    }

    File dir = new File(servoMixerDirectory);

    if (!dir.exists() || !dir.isDirectory()) {
      error("%s not a valid directory", servoMixerDirectory);
      return files;
    }
    File[] all = dir.listFiles();
    Set<String> sorted = new TreeSet<>();
    for (File f : all) {
      if (f.getName().toLowerCase().endsWith(".seq")) {
        sorted.add(f.getName().substring(0, f.getName().lastIndexOf(".")));
      }
    }
    for (String s : sorted) {
      files.add(s);
    }
    return files;
  }

  /**
   * Takes name of a file and a json encoded string of a sequence, saves it to
   * file and sets the "current" sequence to the data
   * 
   */
  public void saveSequence(String filename, String json) {
    try {
      if (filename == null) {
        error("save sequence file name cannot be null");
        return;
      }

      if (json == null) {
        error("sequence json cannot be null");
        return;
      }

      if (!filename.toLowerCase().endsWith(".seq")) {
        filename += ".seq";
      }

      Sequence seq = (Sequence) CodecUtils.fromJson(json, Sequence.class);
      if (seq != null) {
        String path = servoMixerDirectory + fs + filename;
        FileOutputStream fos = new FileOutputStream(path);
        fos.write(CodecUtils.toPrettyJson(seq).getBytes());
        fos.close();
      }
      invoke("getSequenceFiles");
    } catch (Exception e) {
      error(e);
    }
  }

  public void startService() {
    try {
      List<String> all = Runtime.getServiceNamesFromInterface(ServoControl.class);
      for (String sc : all) {
        attach(sc);
      }

      File poseDirectory = new File(servoMixerDirectory);
      if (!poseDirectory.exists()) {
        poseDirectory.mkdirs();
      }
      super.startService();
    } catch (Exception e) {
      error(e);
    }
  }

  public void setAutoDisable(Boolean b) {
    this.autoDisable = b;
    if (b == null) {
      return;
    }
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

  public Boolean getAudoDisable() {
    return autoDisable;
  }

  public static void main(String[] args) throws Exception {

    try {
      Runtime.main(new String[] { "--id", "admin", "--from-launcher" });
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
