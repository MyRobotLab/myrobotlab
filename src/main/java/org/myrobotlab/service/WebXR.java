package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MapperSimple;
import org.myrobotlab.service.config.WebXRConfig;
import org.myrobotlab.service.data.Event;
import org.myrobotlab.service.data.Pose;
import org.slf4j.Logger;

public class WebXR extends Service<WebXRConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(WebXR.class);

  public WebXR(String n, String id) {
    super(n, id);
  }
  
  public Event publishEvent(Event event) {
    // if (log.isDebugEnabled()) {
      log.info("publishEvent XRController {}", event);
    // }
        
    String path = String.format("event.%s.%s", event.meta.get("handedness"), event.type);
    if (event.value != null) {
      path = path + "." + event.value.toString();
    }
    
    if (config.eventMappings.containsKey(path)) {
      // TODO - future might be events -> message that goes to ServoMixer .e.g mixer.playGesture("closeHand")
      // or sadly Python execute script for total chaos :P
      invoke("publishJointAngles", config.eventMappings.get(path));
    }
    
    return event;
  }

  /**
   * Pose is the x,y,z and pitch, roll, yaw of all the devices WebXR found.
   * Hopefully, this includes headset, and hand controllers.
   * WebXRConfig processes a mapping between these values (usually in radians) to
   * servo positions, and will then publish JointAngles for servos.
   * 
   * @param pose
   * @return
   */
  public Pose publishPose(Pose pose) {
//    if (log.isDebugEnabled()) {
      log.error("publishPose {}", pose);
//    }    
    // process mappings config into joint angles
    Map<String, Double> map = new HashMap<>();

    String path = String.format("%s.orientation.roll", pose.name);
    if (config.controllerMappings.containsKey(path)) {
      Map<String, MapperSimple> mapper = config.controllerMappings.get(path);
      for (String name : mapper.keySet()) {
        map.put(name, mapper.get(name).calcOutput(pose.orientation.roll));
      }
    }

    path = String.format("%s.orientation.pitch", pose.name);
    if (config.controllerMappings.containsKey(path)) {
      Map<String, MapperSimple> mapper = config.controllerMappings.get(path);
      for (String name : mapper.keySet()) {
        map.put(name, mapper.get(name).calcOutput(pose.orientation.pitch));
      }
    }

    path = String.format("%s.orientation.yaw", pose.name);
    if (config.controllerMappings.containsKey(path)) {
      Map<String, MapperSimple> mapper = config.controllerMappings.get(path);
      for (String name : mapper.keySet()) {
        map.put(name, mapper.get(name).calcOutput(pose.orientation.yaw));
      }
    }
    
//    InverseKinematics3D ik = (InverseKinematics3D)Runtime.getService("ik3d");
//    if (ik != null && pose.name.equals("left")) {
//      ik.setCurrentArm("left", InMoov2Arm.getDHRobotArm("i01", "left"));
//
//      ik.centerAllJoints("left");
//
//      for (int i = 0; i < 1000; ++i) {
//        
//        ik.centerAllJoints("left");
//        ik.moveTo("left", 0, 0.0+ i * 0.02, 0.0);
//
//        
//        // ik.moveTo(pose.name, new Point(0, -200, 50));
//      }
//      
//      // map name
//      // and then map all position and rotation too :P
//      Point p = new Point(70 + pose.position.x, -550 + pose.position.y, pose.position.z);
//      
//      ik.moveTo(pose.name, p);
//    }

    if (map.size() > 0) {
      invoke("publishJointAngles", map);
    }

    // TODO - publishQuaternion
    // invoke("publishQuaternion", map);

    return pose;
  }

  public Map<String, Double> publishJointAngles(Map<String, Double> map) {
    for (String name: map.keySet()) {
      log.info("{}.moveTo {}", name, map.get(name));
    }
    return map;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
      
      // identical to command line start
      // Runtime.startConfig("inmoov2");
      
      
      // normal non-config launch
      // Runtime.main(new String[] { "--log-level", "info", "-s", "webgui", "WebGui", "intro", "Intro", "python", "Python" });
      
      
      // config launch
      Runtime.startConfig("webxr");
      
      boolean done = true;
      if (done)
        return;

      Runtime.startConfig("webxr");
      boolean done2 = true;
      if (done2)
        return;

      Runtime.start("webxr", "WebXR");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      // webgui.setSsl(true);
      webgui.autoStartBrowser(false);
      webgui.startService();
      Runtime.start("vertx", "Vertx");
      InMoov2 i01 = (InMoov2) Runtime.start("i01", "InMoov2");
      i01.startPeer("simulator");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
