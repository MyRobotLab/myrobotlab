package org.myrobotlab.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractMotor;

public class MotorDualPwm extends AbstractMotor {
  private static final long serialVersionUID = 1L;

  public String leftPwmPin;
  public String rightPwmPin;
  Integer pwmFreq;

  public List<String> pwmPinList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15");

  public String getLeftPwmPin() {
    return leftPwmPin;
  }

  public void setLeftPwmPin(Integer leftPwmPin) {
    // log.info("leftPwmPin set");
    this.leftPwmPin = leftPwmPin + "";
    broadcastState();
  }
  
  public void setLeftPwmPin(String leftPwmPin) {
    // log.info("leftPwmPin set");
    this.leftPwmPin = leftPwmPin;
    broadcastState();
  }

  public String getRightPwmPin() {
    return rightPwmPin;
  }

  public void setRightPwmPin(Integer rightPwmPin) {
    // log.info("rightPwmPin set");
    this.rightPwmPin = rightPwmPin + "";
    broadcastState();
  }
  
  public void setRightPwmPin(String rightPwmPin) {
    // log.info("rightPwmPin set");
    this.rightPwmPin = rightPwmPin;
    broadcastState();
  }

  public MotorDualPwm(String n, String id) {
    super(n, id);
  }

  public void setPwmPins(int leftPwmPin, int rightPwmPin) {
    setLeftPwmPin(leftPwmPin);
    setRightPwmPin(rightPwmPin);
  }

  public Integer getPwmFreq() {
    return pwmFreq;
  }

  public void setPwmFreq(Integer pwmfreq) {
    this.pwmFreq = pwmfreq;
  }

  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(MotorDualPwm.class.getCanonicalName());
    meta.addDescription("Motor service which support 2 pwr pwm pins clockwise and counterclockwise");
    meta.addCategory("motor");

    return meta;
  }

  public static void main(String[] args) throws InterruptedException {

    LoggingFactory.init(Level.INFO);
    String arduinoPort = "COM5";

    VirtualArduino virtual = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
    try {
      virtual.connect(arduinoPort);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    Runtime.start("gui", "SwingGui");
    Runtime.start("python", "Python");

    MotorDualPwm motor = (MotorDualPwm) Runtime.start("motor", "MotorDualPwm");
    Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
    arduino.connect(arduinoPort);
    motor.setPwmPins(10, 11);
    try {
      motor.attach(arduino);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

}
