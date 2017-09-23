package org.myrobotlab.service;

import java.util.Arrays;
import java.util.List;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.service.abstracts.AbstractMotor;

public class MotorDualPwm extends AbstractMotor {
  private static final long serialVersionUID = 1L;
  

  public Integer leftPwmPin;
  public Integer rightPwmPin;
  Integer pwmFreq;

  public List<String> pwmPinList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8","9","10","11","12","13","14","15");
  
  public Integer getLeftPwmPin() {
    return leftPwmPin;
  }

  public void setLeftPwmPin(Integer leftPwmPin) {
    log.info("leftPwmPin set");
    this.leftPwmPin = leftPwmPin;
    broadcastState();
  }

  public Integer getRightPwmPin() {
    return rightPwmPin;
  }

  public void setRightPwmPin(Integer rightPwmPin) {
    log.info("rightPwmPin set");
    this.rightPwmPin = rightPwmPin;
    broadcastState();
  }

  public MotorDualPwm(String n) {
    super(n);
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

}
