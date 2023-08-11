package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.GoProConfig;
import org.slf4j.Logger;

public class GoPro extends Service<GoProConfig> {

  transient public HttpClient http;

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(GoPro.class);

  String cameraModel;
  String password;
  String ipAddress = "10.5.5.9";

  public GoPro(String n, String id) {
    super(n, id);
  }

  @Override
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
