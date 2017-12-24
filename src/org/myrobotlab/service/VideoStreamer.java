package org.myrobotlab.service;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.MjpegServer;
import org.myrobotlab.service.abstracts.AbstractVideoSink;
import org.myrobotlab.service.interfaces.VideoSource;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         Refeences of cool code snippets etc :
 * 
 *         http://www.java2s.com/Code/Java/Network-Protocol/
 *         AsimpletinynicelyembeddableHTTP10serverinJava.htm
 * 
 *         and most importantly Wireshark !!! cuz it ROCKS for getting the truth
 *         !!!
 * 
 */

public class VideoStreamer extends AbstractVideoSink /*extends Service implements VideoSink*/ {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(VideoStreamer.class.getCanonicalName());
  public int listeningPort = 9090;
  transient private MjpegServer server;
  public boolean mergeSteams = true;

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {

      VideoStreamer streamer = (VideoStreamer) Runtime.createAndStart("streamer", "VideoStreamer");
      Vision opencv = (Vision) Runtime.createAndStart("opencv", "OpenCV");

      // streamer.start();
      streamer.attach(opencv);

      opencv.addFilter("pyramidDown", "PyramidDown");
      opencv.capture();

      Runtime.createAndStart("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public VideoStreamer(String name) {
    super(name);
  }

  public void attach(String videoSource) {
    try {
      VideoSource vs = (VideoSource) Runtime.getService(videoSource);
      attach(vs);
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public void onDisplay(SerializableImage si) {
    /*
     * if (mergeSteams) { si.setSource("output"); }
     */

    if (!server.videoFeeds.containsKey(si.getSource())) {
      server.videoFeeds.put(si.getSource(), new LinkedBlockingQueue<SerializableImage>());
    }

    BlockingQueue<SerializableImage> buffer = server.videoFeeds.get(si.getSource());
    // if its backed up over 10 frames we are dumping it
    if (buffer.size() < 1) {
      buffer.add(si);
    }
  }

  @Override
  public void releaseService() {
    super.releaseService();
  }

  /*
   * sets port for mjpeg feed - default is 9090
   * 
   */
  public void setPort(int port) {
    listeningPort = port;
  }

  public void start() {
    start(listeningPort);
  }

  /**
   * starts video streamer
   * 
   * @param port
   *          default is 9090
   */
  public void start(int port) {
    stop();
    listeningPort = port;
    try {
      server = new MjpegServer(listeningPort);
      server.start();
    } catch (IOException e) {
      Logging.logError(e);
    }
  }

  @Override
  public void startService() {
    super.startService();
    start();
  }

  /**
   * Stops the video streamer
   */
  public void stop() {
    if (server != null) {
      server.stop();
    }
    server = null;
  }

  @Override
  public void stopService() {
    super.stopService();
    stop();
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

    ServiceType meta = new ServiceType(VideoStreamer.class.getCanonicalName());
    meta.addDescription("Video streaming service");
    meta.addCategory("video", "display");
    return meta;
  }

}
