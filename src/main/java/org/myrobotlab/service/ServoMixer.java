package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.kinematics.Pose;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServoControl;

public class ServoMixer extends Service {

  private static final long serialVersionUID = 1L;

  protected String posesDirectory = getDataDir() + fs + "poses";

  protected Pose currentPose = null;

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
    allServos.add(name);
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
   * @param servo
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
   * @param name
   * @throws IOException
   */
  public void savePose(String name) throws IOException {
    savePose(name, null);
  }

  public void savePose(String name, List<ServoControl> servos) throws IOException {
    
    if (servos == null) {
      servos = listAllServos();
    }
    
    File poseDirectory = new File(posesDirectory);
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

  public Pose loadPose(String name) {
    try {
      if (!checkDir(posesDirectory)) {
        error("invalid poses directory %s", posesDirectory);
        return null;
      }
      String filename = new File(posesDirectory).getAbsolutePath() + File.separator + name + ".pose";
      log.info("Loading Pose name {}", filename);
      currentPose = Pose.loadPose(filename);
      broadcastState();
    } catch (Exception e) {
      error(e);
    }
    return currentPose;
  }

  public void moveToPose(Pose p) {
    try {
      for (String sc : p.getPositions().keySet()) {
        ServoControl servo = (ServoControl) Runtime.getService(sc);
        if (servo == null) {
          warn("servo (%s) cannot move to pose because it does not exist", sc);
          continue;
        }
        Double speed = p.getSpeeds().get(sc);
        Double position = p.getPositions().get(sc);
        servo.setSpeed(speed);
        servo.moveTo(position);
      }
    } catch (Exception e) {
      error(e);
    }
  }

  public void moveToPose(String name) throws IOException {
    Pose p = loadPose(name);
    moveToPose(p);
  }

  public String getPosesDirectory() {
    return posesDirectory;
  }

  public void setPosesDirectory(String posesDirectory) {
    File dir = new File(posesDirectory);
    if (!dir.exists()) {
      dir.mkdirs();
    }
    this.posesDirectory = posesDirectory;
    invoke("getPoseFiles");
    broadcastState();
  }

  public List<String> getPoseFiles() {

    List<String> files = new ArrayList<>();
    if (!checkDir(posesDirectory)) {
      return files;
    }

    File dir = new File(posesDirectory);

    if (!dir.exists() || !dir.isDirectory()) {
      error("%s not a valid directory", posesDirectory);
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

  public void startService() {
    try {
      List<String> all = Runtime.getServiceNamesFromInterface(ServoControl.class);
      for (String sc : all) {
        attach(sc);
      }

      File poseDirectory = new File(posesDirectory);
      if (!poseDirectory.exists()) {
        poseDirectory.mkdirs();
      }
      super.startService();
    } catch (Exception e) {
      error(e);
    }
  }

  public static void main(String[] args) throws Exception {

    LoggingFactory.init("INFO");
    WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
    webgui.autoStartBrowser(false);
    Runtime.start("python", "Python");
    webgui.startService();

    Servo servo1 = (Servo) Runtime.start("servo1", "Servo");
    servo1.setPin(1);
    Servo servo2 = (Servo) Runtime.start("servo2", "Servo");
    servo2.setPin(2);
    Servo servo3 = (Servo) Runtime.start("servo3", "Servo");
    servo3.setPin(3);

    for (int i = 0; i < 20; ++i) {
      Runtime.start(String.format("servo%d", i), "Servo");
    }
    
    /*

    VirtualArduino virt = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
    virt.connect("VRPORT");
    Arduino ard = (Arduino) Runtime.start("ard", "Arduino");
    ard.connect("VRPORT");
    ard.attach(servo1);
    ard.attach(servo2);
    ard.attach(servo3);
    */

    ServoMixer mixer = (ServoMixer) Runtime.start("mixer", "ServoMixer");

  }

}
