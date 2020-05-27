package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OculusRiftMeta {
  public final static Logger log = LoggerFactory.getLogger(OculusRiftMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.OculusRift");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("The Oculus Rift Head Tracking Service");
    meta.addCategory("video", "control", "sensors", "telerobotics");
    // make sure the open cv instance share each others streamer..
    // meta.sharePeer("leftOpenCV.streamer", "streamer", "VideoStreamer",
    // "shared left streamer");
    // meta.sharePeer("rightOpenCV.streamer", "streamer", "VideoStreamer",
    // "shared right streamer");

    meta.addPeer("leftOpenCV", "OpenCV", "Left Eye Camera");
    // meta.sharePeer("rightOpenCV", "leftOpenCV", "OpenCV", "Right Eye sharing
    // left eye camera");
    meta.addPeer("rightOpenCV", "OpenCV", "Right Eye Camera");
    // compile(group: 'org.saintandreas', name: 'jovr', version: '0.7.0.0')

    meta.addDependency("slick-util", "slick-util", "1.0.0");
    meta.addDependency("org.saintandreas", "jovr", "1.8.0.0");
    meta.addDependency("org.saintandreas", "glamour-lwjgl", "1.0.8");
    meta.addDependency("org.saintandreas", "math", "1.0.4");
    meta.addDependency("org.saintandreas", "oria-resources", "1.0.4");
    meta.exclude("org.slf4j", "slf4j-api");
    meta.exclude("org.lwjgl.lwjgl", "lwjgl");
    meta.exclude("com.google.guava", "guava");
    return meta;
  }
  
}

