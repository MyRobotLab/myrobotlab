package org.myrobotlab.service;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.service.abstracts.AbstractMotor;

public class MotorDualPwm extends AbstractMotor {
  private static final long serialVersionUID = 1L;
  

  Integer leftPwmPin;
  Integer rightPwmPin;
  Integer pwmFreq;

  
  public Integer getLeftPwmPin() {
    return leftPwmPin;
  }

  public void setLeftPwmPin(Integer leftPwmPin) {
    this.leftPwmPin = leftPwmPin;
  }

  public Integer getRightPwmPin() {
    return rightPwmPin;
  }

  public void setRightPwmPin(Integer rightPwmPin) {
    this.rightPwmPin = rightPwmPin;
  }

  public MotorDualPwm(String n) {
    super(n);
  }
  
  public void setPwmPins(int leftPwmPin, int rightPwmPin) {
    this.leftPwmPin = leftPwmPin;
    this.rightPwmPin = rightPwmPin;
    broadcastState();
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
