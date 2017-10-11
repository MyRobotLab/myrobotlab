package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractMotor;
import org.myrobotlab.service.interfaces.MotorController;

public class MotorHat4Pi extends AbstractMotor {
  private static final long serialVersionUID = 1L;
  
  Integer leftDirPin;
  Integer rightDirPin;
  Integer pwmPin;
  Integer pwmFreq;
  String motorId;
  
  public List<String> motorList = Arrays.asList("M1", "M2", "M3", "M4");
  
  public MotorHat4Pi(String n) {
    super(n);
    refreshControllers();
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
  }
  
  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();
  }
  
  public List<String> refreshControllers() {
    controllers = new ArrayList<String>();
    for (String serviceName : Runtime.getServiceNamesFromInterface(MotorController.class)) {
      if (Runtime.getService(serviceName).getClass() == AdafruitMotorHat4Pi.class) {
        controllers.add(serviceName);
      }
    }
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
  
  public void setMotor(String motorId){
    if (motorId == "M1"){
      pwmPin = 8;
      leftDirPin = 9;
      rightDirPin = 10;
    }
    else if (motorId == "M2"){
      pwmPin = 13;
      leftDirPin = 12;
      rightDirPin = 11;      
    }
    else if (motorId == "M3"){
      pwmPin = 2;
      leftDirPin = 3;
      rightDirPin = 4;     
    }
    else if  (motorId == "M4"){
      pwmPin = 7;
      leftDirPin = 6;
      rightDirPin = 5;      
    }
    else {
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
  
  public static void main(String[] args) {

    LoggingFactory.getInstance().configure();
    LoggingFactory.getInstance().setLevel(Level.DEBUG);

    SwingGui swing = (SwingGui) Runtime.start("gui", "SwingGui");
    RasPi raspi = (RasPi) Runtime.start("raspi", "RasPi");
    AdafruitMotorHat4Pi hat = (AdafruitMotorHat4Pi) Runtime.start("hat", "AdafruitMotorHat4Pi");
    MotorHat4Pi motor = (MotorHat4Pi) Runtime.start("motor", "MotorHat4Pi");
    hat.attach(raspi, "1", "0x60");
  }
  
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(MotorHat4Pi.class.getCanonicalName());
    meta.addDescription("Motor service for the Raspi Motor HAT");
    meta.addCategory("motor");
    meta.addPeer("hat","AdafruitMotorHat4Pi", "Motor HAT");
    meta.setAvailable(true);

    return meta;
  }

}
