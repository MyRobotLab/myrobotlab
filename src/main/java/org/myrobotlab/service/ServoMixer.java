package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
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

    // FIXME - make this part of framework !!!!
    //subscribe("runtime", "started");
    // subscribe("runtime", "registered");
    // subscribe("runtime", "released");

    // FIXME - incorporate into framework
    // FIXME - this "should" be calling onStarted :(
    List<String> all = Runtime.getServiceNamesFromInterface(ServoControl.class);
    for (String sc : all) {
      allServos.add(Runtime.getFullName(sc));
    }

  }

  public void onStarted(String name) {
    ServiceInterface si = Runtime.getService(name);
    if (si instanceof ServoControl) {
        allServos.add(Runtime.getFullName(name));
    }
    broadcastState();
  }

  // FIXME - part of the service life-cycle framework - this method should be in
  // Abstract Service
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

  public void savePose(String name) throws IOException {
    // This assumes all servos will be used for the pose.
    List<ServoControl> servos = listAllServos();
    savePose(name, servos);
    broadcast("getPoseFiles");
  }

  public void savePose(String name, List<ServoControl> servos) throws IOException {
    // TODO: save this pose somewhere!
    // we should make a directory
    File poseDirectory = new File(posesDirectory);
    if (!poseDirectory.exists()) {
      poseDirectory.mkdirs();
    }

    log.info("Saving pose name {}", name);
    Pose p = new Pose(name, servos);
    // p.save()
    String filename = poseDirectory.getAbsolutePath() + File.separator + name + ".pose";
    p.savePose(filename);
  }

  public Pose loadPose(String name) throws IOException {
    String filename = new File(posesDirectory).getAbsolutePath() + File.separator + name + ".pose";
    log.info("Loading Pose name {}", filename);
    currentPose = Pose.loadPose(filename);
    broadcastState();
    return currentPose;
  }

  public void moveToPose(Pose p) throws IOException {
    // TODO: look up the pose / load it
    // then move the servos to the positions
    for (String sc : p.getPositions().keySet()) {
      ServoControl servo = (ServoControl) Runtime.getService(sc);
      Double speed = p.getSpeeds().get(sc);
      Double position = p.getPositions().get(sc);
      servo.setSpeed(speed);
      servo.moveTo(position);
    }
  }

  public void moveToPose(String name) throws IOException {
    // TODO: look up the pose / load it
    // then move the servos to the positions
    Pose p = loadPose(name);
    moveToPose(p);
  }

  public String getPosesDirectory() {
    return posesDirectory;
  }

  public void setPosesDirectory(String posesDirectory) {
    this.posesDirectory = posesDirectory;
  }

  public List<String> getPoseFiles() {
    File dir = new File(posesDirectory);
    List<String> files = new ArrayList<>();
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

    VirtualArduino virt = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
    virt.connect("VRPORT");
    Arduino ard = (Arduino) Runtime.start("ard", "Arduino");
    ard.connect("VRPORT");
    ard.attach(servo1);
    ard.attach(servo2);
    ard.attach(servo3);

    ServoMixer mixer = (ServoMixer) Runtime.start("mixer", "ServoMixer");

  }

}
