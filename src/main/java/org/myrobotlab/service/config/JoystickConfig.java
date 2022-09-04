package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.HashMap;

public class JoystickConfig extends ServiceConfig {

  public String controller;

  public HashMap<String, ArrayList<String>> analogListeners;

  public HashMap<String, ArrayList<String>> componentListeners;

}
