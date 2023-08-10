package org.myrobotlab.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.Registration;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractMotor;
import org.myrobotlab.service.config.MotorConfig;
import org.myrobotlab.service.config.MotorHat4PiConfig;
import org.myrobotlab.service.config.ServiceConfig;

public class MotorHat4Pi extends AbstractMotor<MotorHat4PiConfig> {
  private static final long serialVersionUID = 1L;

  Integer leftDirPin;
  Integer rightDirPin;
  Integer pwmPin;
  Integer pwmFreq;
  String motorId;

  public List<String> motorList = Arrays.asList("M1", "M2", "M3", "M4");

  public MotorHat4Pi(String n, String id) {
    super(n, id);
    refreshControllers();
    subscribeToRuntime("registered");
  }

  @Override
  public void onRegistered(Registration s) {
    if (s.hasInterface(AdafruitMotorHat4Pi.class)) {
      controllers.add(s.getName());
      broadcastState();
    }
  }

  @Override
  public Set<String> refreshControllers() {
    controllers.clear();
    controllers.addAll(Runtime.getServiceNamesFromInterface(AdafruitMotorHat4Pi.class));
    return controllers;
  }

  public Integer getLeftDirPin() {
    return leftDirPin;
  }

  public Integer getRightDirPin() {
    return rightDirPin;
  }

  public Integer getPwmPin() {
    return pwmPin;
  }

  public void setMotor(String motorId) {
    if (motorId == "M1") {
      pwmPin = 8;
      leftDirPin = 9;
      rightDirPin = 10;
    } else if (motorId == "M2") {
      pwmPin = 13;
      leftDirPin = 12;
      rightDirPin = 11;
    } else if (motorId == "M3") {
      pwmPin = 2;
      leftDirPin = 3;
      rightDirPin = 4;
    } else if (motorId == "M4") {
      pwmPin = 7;
      leftDirPin = 6;
      rightDirPin = 5;
    } else {
      error("MotorId must be between M1 and M4 inclusive");
      return;
    }
    this.motorId = motorId;
    broadcastState();
  }

  public Integer getPwmFreq() {
    return pwmFreq;
  }

  public void setPwmFreq(Integer pwmfreq) {
    this.pwmFreq = pwmfreq;
  }

  public String getMotorId() {
    return motorId;
  }

  @Override
  public MotorHat4PiConfig getConfig() {
    // FIXME - may need to do call super.config for config that has parent :(
    config.motorId = motorId;
    return config;
  }

  public MotorHat4PiConfig apply(MotorHat4PiConfig c) {
    super.apply(c);
    setMotor(config.motorId);
    return c;
  }

  public static void main(String[] args) {

    LoggingFactory.init();

    RasPi raspi = (RasPi) Runtime.start("raspi", "RasPi");
    AdafruitMotorHat4Pi hat = (AdafruitMotorHat4Pi) Runtime.start("hat", "AdafruitMotorHat4Pi");
    MotorHat4Pi motor = (MotorHat4Pi) Runtime.start("motor", "MotorHat4Pi");
    hat.attach(raspi, "1", "0x60");
  }

}
