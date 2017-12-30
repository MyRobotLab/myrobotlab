package org.myrobotlab.service;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.net.InetSocketAddress;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import us.sosia.video.stream.agent.StreamClientAgent;
import us.sosia.video.stream.agent.StreamServerAgent;
import us.sosia.video.stream.agent.ui.SingleVideoDisplayWindow;
import us.sosia.video.stream.handler.StreamFrameListener;

public class Webcam extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Webcam.class);

  transient com.github.sarxos.webcam.Webcam webcam;
  transient Dimension dimension = new Dimension(640, 480);
  transient StreamClientAgent clientAgent;
  transient SingleVideoDisplayWindow displayWindow = new SingleVideoDisplayWindow("Stream example", dimension);

  public Webcam(String n) {
    super(n);
  }

  protected class StreamFrameListenerIMPL implements StreamFrameListener {
    private volatile long count = 0;

    @Override
    public void onFrameReceived(BufferedImage image) {
      log.info("frame received :{}", count++);
      displayWindow.updateImage(image);
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
    setAutoOpenMode(true);
    ServiceType meta = new ServiceType(Webcam.class.getCanonicalName());
    meta.addDescription("used as a general webcam");
    meta.addCategory("video");
    return meta;
  }

  public void startStreamServer(String host, int port) {

    webcam.setViewSize(dimension);

    StreamServerAgent serverAgent = new StreamServerAgent(webcam, dimension);
    serverAgent.start(new InetSocketAddress(host, port));
  }

  // FIXME - can't be swing !!!
  public void startStreamClient(String host, int port) {
    // setup the videoWindow
    displayWindow.setVisible(true);

    // setup the connection
    log.info("setup dimension :{}", dimension);
    clientAgent = new StreamClientAgent(new StreamFrameListenerIMPL(), dimension);
    clientAgent.connect(new InetSocketAddress(host, port));
  }

  // heh .. I wonder if com.github.sarxos.webcam.Webcam is serializable ? I give
  // it 20 to 1...
  public List<com.github.sarxos.webcam.Webcam> getWebcams() {
    List<com.github.sarxos.webcam.Webcam> webcams = com.github.sarxos.webcam.Webcam.getWebcams();
    return webcams;
  }

  public void startService() {

    webcam = com.github.sarxos.webcam.Webcam.getDefault();
  }

  static public void setAutoOpenMode(boolean b) {
    com.github.sarxos.webcam.Webcam.setAutoOpenMode(b);
  }

  public void setDimension(int width, int height) {
    dimension = new Dimension(640, 480);
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Webcam webcam = (Webcam) Runtime.start("webcam", "Webcam");
      // Runtime.start("webgui", "WebGui");
      webcam.startStreamServer("0.0.0.0", 22222);
      Runtime.start("webgui", "WebGui");
      webcam.startStreamClient("127.0.0.1", 22222);

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
