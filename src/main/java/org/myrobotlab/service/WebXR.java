package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MapperSimple;
import org.myrobotlab.service.config.WebXRConfig;
import org.myrobotlab.service.data.Pose;
import org.slf4j.Logger;

public class WebXR extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(WebXR.class);

  public WebXR(String n, String id) {
    super(n, id);
  }
  
  public Pose publishPose(Pose pose) {
    log.warn("publishPose {}", pose);
    System.out.println(pose.toString());
    
    // process mappings config into joint angles
    Map<String, Double> map = new HashMap<>();  

    WebXRConfig c = (WebXRConfig)config;
    String path = String.format("%s.orientation.roll", pose.name);
    if (c.mappings.containsKey(path)) {
      Map<String, MapperSimple> mapper = c.mappings.get(path);
      for (String name: mapper.keySet()) {
        map.put(name, mapper.get(name).calcOutput(pose.orientation.roll));
      }
    }
    
    path = String.format("%s.orientation.pitch", pose.name);
    if (c.mappings.containsKey(path)) {
      Map<String, MapperSimple> mapper = c.mappings.get(path);
      for (String name: mapper.keySet()) {
        map.put(name, mapper.get(name).calcOutput(pose.orientation.pitch));
      }
    }

    path = String.format("%s.orientation.yaw", pose.name);
    if (c.mappings.containsKey(path)) {
      Map<String, MapperSimple> mapper = c.mappings.get(path);
      for (String name: mapper.keySet()) {
        map.put(name, mapper.get(name).calcOutput(pose.orientation.yaw));
      }
    }
        
    invoke("publishJointAngles", map);
    
    return pose;
  }
  
  // TODO publishQuaternion

  public Map<String, Double> publishJointAngles(Map<String, Double> map){    
    return map;    
  }
  
  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("webxr", "WebXr");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      // webgui.setSsl(true);
      webgui.autoStartBrowser(false);
      webgui.startService();
      Runtime.start("vertx", "Vertx");
      InMoov2 i01 = (InMoov2)Runtime.start("i01", "InMoov2");
      i01.startPeer("simulator");


    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
