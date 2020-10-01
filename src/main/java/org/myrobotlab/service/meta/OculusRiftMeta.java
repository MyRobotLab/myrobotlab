package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class OculusRiftMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OculusRiftMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   */
  public OculusRiftMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("The Oculus Rift Head Tracking Service");
    addCategory("video", "control", "sensors", "telerobotics");
    // make sure the open cv instance share each others streamer..
    // sharePeer("leftOpenCV.streamer", "streamer", "VideoStreamer",
    // "shared left streamer");
    // sharePeer("rightOpenCV.streamer", "streamer", "VideoStreamer",
    // "shared right streamer");

    addPeer("leftOpenCV", "OpenCV", "Left Eye Camera");
    // sharePeer("rightOpenCV", "leftOpenCV", "OpenCV", "Right Eye sharing
    // left eye camera");
    addPeer("rightOpenCV", "OpenCV", "Right Eye Camera");
    // compile(group: 'org.saintandreas', name: 'jovr', version: '0.7.0.0')

    addDependency("slick-util", "slick-util", "1.0.0");
    addDependency("org.saintandreas", "jovr", "1.8.0.0");
    addDependency("org.saintandreas", "glamour-lwjgl", "1.0.8");
    addDependency("org.saintandreas", "math", "1.0.4");
    addDependency("org.saintandreas", "oria-resources", "1.0.4");
    
    // adding to pin dep at 2.9.3
    addDependency("org.lwjgl.lwjgl", "lwjgl-platform", "2.9.3", "pom");

    exclude("org.slf4j", "slf4j-api");
    
    // exclude("org.lwjgl.lwjgl", "lwjgl");
    exclude("com.google.guava", "guava");

  }

}
