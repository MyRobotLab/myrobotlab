package org.myrobotlab.service;

import java.awt.Dimension;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamStreamer;

public class Webcam extends Service implements WebcamListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Webcam.class);

  transient com.github.sarxos.webcam.Webcam webcam;
  transient Dimension dimension = new Dimension(640, 480);

  private Object streamer;

  public Webcam(String n, String id) {
    super(n, id);
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
    // setAutoOpenMode(true);
    ServiceType meta = new ServiceType(Webcam.class);
    meta.addDescription("used as a general webcam");
    meta.addCategory("video");
    return meta;
  }

  public void start() {
    start(null);
  }

  public void start(Integer port) {
    if (webcam == null) {
      webcam = com.github.sarxos.webcam.Webcam.getDefault();
    }
    if (port == null) {
      port = 8080;
    }
    if (streamer == null) {
      streamer = new WebcamStreamer(port, webcam, 0.5, true);
    }
  }

  @Override
  public void webcamClosed(WebcamEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void webcamDisposed(WebcamEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void webcamImageObtained(WebcamEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void webcamOpen(WebcamEvent arg0) {
    // TODO Auto-generated method stub

  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Webcam webcam = (Webcam) Runtime.start("webcam", "Webcam");
      webcam.start(8080);
      // Runtime.start("webgui", "WebGui");
      // webcam.startStreamServer("0.0.0.0", 22222);
      // Runtime.start("webgui", "WebGui");
      // webcam.startStreamClient("127.0.0.1", 22222);

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
