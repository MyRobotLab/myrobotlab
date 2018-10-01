package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.kinematics.Pose;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServoControl;

public class ServoMixer extends Service {

  private static final String POSES_DIRECTORY = "poses";
  private static final long serialVersionUID = 1L;

  public ServoMixer(String reservedKey) {
    super(reservedKey);
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

  public void savePose(String name, List<ServoControl> servos) throws IOException {
    // TODO: save this pose somewhere!
    // we should make a directory
    File poseDirectory = new File(POSES_DIRECTORY);
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
    String filename = new File(POSES_DIRECTORY).getAbsolutePath() + File.separator + name + ".pose";
    log.info("Loading Pose name {}", filename);
    Pose p = Pose.loadPose(filename);
    return p;

  }

  public void moveToPose(Pose p) throws IOException {
    // TODO: look up the pose / load it
    // then move the servos to the positions
    for (String sc : p.getPositions().keySet()) {
      ServoControl servo = (ServoControl) Runtime.getService(sc);
      Double d = p.getPositions().get(sc);
      servo.moveTo(d);
    }
  }

  public void moveToPose(String name) throws IOException {
    // TODO: look up the pose / load it
    // then move the servos to the positions
    Pose p = loadPose(name);
    for (String sc : p.getPositions().keySet()) {
      ServoControl servo = (ServoControl) Runtime.getService(sc);
      Double d = p.getPositions().get(sc);
      servo.moveTo(d);
    }
  }

  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(ServoMixer.class.getCanonicalName());
    meta.addDescription("ServoMixer - most just a swing gui that allows for simple movements of all servos in one gui panel.");
    return meta;
  }

  public static void main(String[] args) throws Exception {

    LoggingFactory.init("INFO");
    Runtime.start("gui", "SwingGui");
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

    ServoMixer mixer = (ServoMixer) Runtime.start("servomixer", "ServoMixer");

  }

}