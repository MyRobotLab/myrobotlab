package org.myrobotlab.service;

import java.util.Arrays;
import java.util.List;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractMotor;
import org.myrobotlab.service.config.MotorDualPwmConfig;

public class MotorDualPwm extends AbstractMotor<MotorDualPwmConfig> {
  private static final long serialVersionUID = 1L;

  protected String leftPwmPin;
  protected String rightPwmPin;
  protected Integer pwmFreq;

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

  @Override
  public MotorDualPwmConfig getConfig() {
    // FIXME - may need to do call super.config for config that has parent :(
    super.getConfig();
    config.leftPwmPin = leftPwmPin;
    config.rightPwmPin = rightPwmPin;
    config.pwmFreq = pwmFreq;
    return config;
  }

  public MotorDualPwmConfig apply(MotorDualPwmConfig c) {
    super.apply(c);
    if (c.leftPwmPin != null) {
      setLeftPwmPin(c.leftPwmPin);
    }
    if (c.rightPwmPin != null) {
      setRightPwmPin(c.rightPwmPin);
    }
    if (c.pwmFreq != null) {
      setPwmFreq(c.pwmFreq);
    }
    return c;
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);
      String arduinoPort = "COM5";

      Runtime.getInstance().setVirtual(true);
      Runtime.startConfig("dev");
      Runtime.start("webgui", "WebGui");
      MotorDualPwm motor = (MotorDualPwm) Runtime.start("motor", "MotorDualPwm");
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.connect(arduinoPort);
      motor.setPwmPins(10, 11);

      motor.attach(arduino);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void attachMotorController(String controller) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void detachMotorController(String controller) {
    // TODO Auto-generated method stub
    
  }
}
