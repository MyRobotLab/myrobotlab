package org.myrobotlab.service;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.bytedeco.javacv.IPCameraFrameGrabber;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * IPCamera - a service to allow streaming of video from an IP based camera.
 *
 * Android related -
 * http://stackoverflow.com/questions/8301543/android-bitmap-to-bufferedimage
 * Bitmap to BufferedImage - conversion once Bitmap class is serialized
 */
public class IpCamera extends Service {

  public class VideoProcess implements Runnable {
    @Override
    public void run() {
      try {
        grabber.start();
        capturing = true;
        while (capturing) {
          BufferedImage bi = grabber.grabBufferedImage();
          log.debug("grabbed");
          if (bi != null) {
            log.debug("publishDisplay");
            invoke("publishDisplay", new Object[] { getName(), bi });
          }
        }
      } catch (Exception e) {        
      }
    }
  }

  private static final long serialVersionUID = 1L;

  transient private IPCameraFrameGrabber grabber = null;

  transient private Thread videoProcess = null;

  public String controlURL;

  private boolean capturing = false;

  private boolean enableControls = true;

  public final static Logger log = LoggerFactory.getLogger(IpCamera.class.getCanonicalName());
  public final static int FOSCAM_MOVE_UP = 0;
  public final static int FOSCAM_MOVE_STOP_UP = 1;
  public final static int FOSCAM_MOVE_DOWN = 2;
  public final static int FOSCAM_MOVE_STOP_DOWN = 3;
  public final static int FOSCAM_MOVE_LEFT = 4;
  public final static int FOSCAM_MOVE_STOP_LEFT = 5;
  public final static int FOSCAM_MOVE_RIGHT = 6;
  public final static int FOSCAM_MOVE_STOP_RIGHT = 7;
  public final static int FOSCAM_MOVE_CENTER = 25;
  public final static int FOSCAM_MOVE_VERTICLE_PATROL = 26;
  public final static int FOSCAM_MOVE_STOP_VERTICLE_PATROL = 27;
  public final static int FOSCAM_MOVE_HORIZONTAL_PATROL = 28;
  public final static int FOSCAM_MOVE_STOP_HORIZONTAL_PATROL = 29;
  public final static int FOSCAM_MOVE_IO_OUTPUT_HIGH = 94;

  public final static int FOSCAM_MOVE_IO_OUTPUT_LOW = 95;
  public final static int FOSCAM_ALARM_MOTION_ARMED_DISABLED = 0;
  public final static int FOSCAM_ALARM_MOTION_ARMED_ENABLED = 1;
  public final static int FOSCAM_ALARM_MOTION_SENSITIVITY_HIGH = 0;
  public final static int FOSCAM_ALARM_MOTION_SENSITIVITY_MEDIUM = 1;
  public final static int FOSCAM_ALARM_MOTION_SENSITIVITY_LOW = 2;
  public final static int FOSCAM_ALARM_MOTION_SENSITIVITY_ULTRALOW = 3;
  public final static int FOSCAM_ALARM_INPUT_ARMED_DISABLED = 0;
  public final static int FOSCAM_ALARM_INPUT_ARMED_ENABLED = 1;
  public final static int FOSCAM_ALARM_MAIL_DISABLED = 0;

  public final static int FOSCAM_ALARM_MAIL_ENABLED = 1;

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {
      IpCamera foscam = new IpCamera("foscam");
      foscam.startService();

      foscam.startService();

      SwingGui gui = new SwingGui("gui");
      gui.startService();

    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  public final static SerializableImage publishFrame(String source, BufferedImage img) {
    SerializableImage si = new SerializableImage(img, source);
    return si;
  }

  public IpCamera(String n) {
    super(n);
  }

  /*
   * method to determine connectivity of a valid host, user &amp; password to a
   * foscam camera.
   * 
   * @return
   */
  /*
   * public String getStatus() { StringBuffer ret = new StringBuffer(); try {
   * 
   * URL url = new URL("http://" + host + "/get_status.cgi?user=" + user +
   * "&pwd=" + password); log.debug("getStatus " + url); URLConnection con =
   * url.openConnection(); BufferedReader in = new BufferedReader(new
   * InputStreamReader(con.getInputStream())); String inputLine;
   * 
   * // TODO - parse for good info
   * 
   * while ((inputLine = in.readLine()) != null) { ret.append(inputLine); }
   * in.close();
   * 
   * log.debug(String.format("%d",ret.indexOf("var id")));
   * 
   * if (ret.indexOf("var id") != -1) { ret = new StringBuffer("connected"); }
   * else { } } catch (Exception e) { ret.append(e.getMessage());
   * logException(e); } return ret.toString(); }
   */

  public void capture() {
    if (videoProcess != null) {
      capturing = false;
      videoProcess = null;
    }
    videoProcess = new Thread(new VideoProcess(), getName() + "_videoProcess");
    videoProcess.start();
  }

  // "http://" + host + "/videostream.cgi?user=" + user + "&pwd=" + password
  public boolean connectVideoStream(String url) throws MalformedURLException {
    grabber = new IPCameraFrameGrabber(url);
    // invoke("getStatus");
    capture();
    return true;
  }

  /*
   * public String setAlarm(int armed, int sensitivity, int inputArmed, int
   * ioLinkage, int mail, int uploadInterval) { StringBuffer ret = new
   * StringBuffer(); try {
   * 
   * URL url = new URL("http://" + host + "/set_alarm.cgi?motion_armed=" + armed
   * + "user=" + user + "&pwd=" + password); URLConnection con =
   * url.openConnection(); BufferedReader in = new BufferedReader(new
   * InputStreamReader(con.getInputStream())); String inputLine;
   * 
   * while ((inputLine = in.readLine()) != null) { ret.append(inputLine); }
   * in.close(); } catch (Exception e) { logException(e); } return
   * ret.toString(); }
   */

  public String move(Integer param) {
    if (!enableControls) {
      return null;
    }

    log.info("move " + param);
    StringBuffer ret = new StringBuffer();
    try {
      // TODO - re-use connection optimization

      // URL url = new URL("http://" + host +
      // "/decoder_control.cgi?command=" + param + "&user=" + user +
      // "&pwd=" + password);
      URL url = new URL(controlURL + param);
      URLConnection con = url.openConnection();
      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      String inputLine;

      while ((inputLine = in.readLine()) != null) {
        ret.append(inputLine);
      }
      in.close();
    } catch (Exception e) {
    	log.error("move threw", e);
    }
    return ret.toString();
  }

  public SerializableImage publishDisplay(String source, BufferedImage img) {
    return new SerializableImage(img, source);
  }

  public void setControlURL(String url) {
    controlURL = url;
  }

  public Boolean setEnableControls(Boolean v) {
    enableControls = v;
    return v;
  }

  public void stopCapture() {
    capturing = false;
    if (videoProcess != null) {
      capturing = false;
      videoProcess = null;
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

    ServiceType meta = new ServiceType(IpCamera.class.getCanonicalName());
    meta.addDescription("control and video stream capture for generic ip camera");
    meta.addCategory("video");
    // FIXME - should be webcam dependency not opencv !
    // meta.addDependency("org.bytedeco.javacpp","1.1");
    meta.addDependency("org.bytedeco.javacv", "1.3");
    return meta;
  }

}
