package org.myrobotlab.jme3.interfaces;

import org.myrobotlab.service.interfaces.Simulator;

import com.jme3.app.SimpleApplication;

public interface Jme3App extends Simulator {

  Jme3Object get(String name);
  SimpleApplication getApp();
  // Jme3Object create(ServiceInterface service);
  
}
