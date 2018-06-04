package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;
import org.myrobotlab.service.interfaces.ServoControl;

/**
 * InMoovHand2 WIP - The Hand sub service for the InMoov Robot. 
 */
public class InMoovHand2 extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoovHand2.class);

  /**
   * Servos current collection for InMoovHand
   */
  ArrayList<ServoControl> servos = new ArrayList<ServoControl>();
  /**
   * Because we need it, just for backward compatibility informations... 
   */
  List<String> servosConventionalNames = Arrays.asList("Thumb", "Index", "Majeur", "RingFinger", "Pinky", "Wrist");

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    InMoovHand2 leftHand = (InMoovHand2) Runtime.start("leftHand", "InMoovHand2");

    VirtualArduino virtualArduino = (VirtualArduino) Runtime.start("virtualArduino", "VirtualArduino");
    try {
      virtualArduino.connect("COM42");
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
    arduino.connect("COM42");

    // virtual arduino can't simulate velocity at this time
    // i2c service connected onto virtual arduino will do the job
    // https://github.com/MyRobotLab/myrobotlab/issues/99
    Adafruit16CServoDriver adafruit16CServoDriver = (Adafruit16CServoDriver) Runtime.start("adafruit16CServoDriver", "Adafruit16CServoDriver");
    adafruit16CServoDriver.attach(arduino, "0", "0x40");

    Servo servo = (Servo) Runtime.start("servo", "Servo");

    leftHand.attach(servo);

    leftHand.moveTo(10.0);
    leftHand.moveTo(90.0, 90.0, 90.0, 90.0, 90.0);
  }

  public InMoovHand2(String n) {
    super(n);
  }

  public void attach(Attachable attachable) {
    if (attachable instanceof ServoControl) {
      if (!servos.contains((ServoControl) attachable)) {
        servos.add((ServoControl) attachable);
        if (servosConventionalNames.size() >= servos.size() - 1) {
          info(attachable.getClass().getSimpleName() + " " + attachable.getName() + " attached as : " + servosConventionalNames.get(servos.size() - 1));
        }
      }
    } else {
      error("don't know how to attach a {}", attachable.getName());
    }
  }

  /** 
   * move a group of Inmoov Hand servo
   * moveTo order is based on attach order, very important !
   * Please note the InMoov1 syntax for backward compatibility :
   * Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist
   */
  public void moveTo(Double... servoPos) {
    if (servoPos.length > servos.size()) {
      warn("Too many parameters for moveTo(), not enough elements to move ...");
    }
    for (int i = 0; i < servoPos.length && i < servos.size(); i++) {
      servos.get(i).moveTo(servoPos[i]);
    }
  }

}
