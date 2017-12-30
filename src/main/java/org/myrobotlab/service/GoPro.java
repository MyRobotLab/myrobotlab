package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class GoPro extends Service {

  transient public HttpClient http;

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(GoPro.class);

  String cameraModel;
  String password;
  String ipAddress = "10.5.5.9";

  public GoPro(String n) {
    super(n);
    http = (HttpClient) createPeer("http");
  }

  public void startService() {
    super.startService();
    http = (HttpClient) startPeer("http");
    return;
  }

  public void setIpAddress(String address) {
    ipAddress = address;
  }

  public void setCameraModel(String model) {
    cameraModel = model;
  }

  public void setWifiPassword(String passwordWifi) {
    password = passwordWifi;
  }

  public void turnCameraOff() {
    if (cameraModel == "HERO4") {
      sendHttpGet("/gp/gpControl/command/system/sleep");
    } else {
      System.out.println("Select your Camera Before");
    }
  }

  public void shutterOn() {
    if (cameraModel == "HERO4") {
      sendHttpGet("/gp/gpControl/command/shutter?p=1");
    } else if (cameraModel == "HERO3" && password != null) {
      sendHttpGet("/bacpac/SH?t=" + password + "&p=%01");
    } else {
      log.error("Select your Camera model and insert your wifi password before");
    }
  }

  public void sendHttpGet(String path) {
    try {
      String getResult = http.get(String.format("http://%s%s", ipAddress, path));
      log.debug(getResult);
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(GoPro.class.getCanonicalName());
    meta.addDescription("controls a GoPro camera over wifi");
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("video");
    meta.addPeer("http", "HttpClient", "Http for GoPro control");
    return meta;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Runtime.start("gopro", "GoPro");
      Runtime.start("gui", "SwingGui");
      Runtime.start("python", "Python");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
