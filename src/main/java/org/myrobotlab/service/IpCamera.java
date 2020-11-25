package org.myrobotlab.service;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * IPCamera - a service to allow streaming of video from an IP based camera.
 */
public class IpCamera extends Service implements ServoController /*implements EncoderListener*/ {

  public final static Logger log = LoggerFactory.getLogger(IpCamera.class);

  private static final long serialVersionUID = 1L;

  protected String baseUrl;

  // vertical patrol = 26 stop 27 / horizontal patrol 28 stop 29
  // TODO - use security service to protect these fields
  protected String user;
  protected String pwd;

  protected Integer x;
  protected Integer y;

  protected String status;
  
  protected Map<String, Timer> moves = new HashMap<>();
  
  public IpCamera(String n, String id) {
    super(n, id);
  }

  public String center() {
    return processCommand("decoder_control", 25);
  }

  public String processCommand(String cmd) {
    return processCommand(cmd, null);
  }

  public String processCommand(String cmd, Integer param) {
    StringBuilder sb = new StringBuilder(baseUrl);
    sb.append(String.format("/%s.cgi?", cmd));
    if (user != null) {
      sb.append("user=");
      sb.append(user);
    }
    if (pwd != null) {
      sb.append("&");
      sb.append("pwd=");
      sb.append(pwd);
    }
    if (param != null) {
      sb.append("&");
      sb.append("command=");
      sb.append(param);
    }

    // FIXME - Http.get should throw - and this should report the error
    byte[] bytes = Http.get(sb.toString());
    if (bytes == null) {
      return null;
    }

    return new String(bytes);
  }

  /**
   * Relative move from "current" position. The interface to the foscom will
   * center the device by dead reckoning from moving to extreme - knowing that
   * after a certain amount of time the camera would be pushed to maximum in a
   * corner. After it "centers" all increments are dead reckoning with some
   * constant time interval
   * 
   * right /decoder_control.cgi?command=4 left /decoder_control.cgi?command=6 up
   * /decoder_control.cgi?command=0 down /decoder_control.cgi?command=2 stop
   * /decoder_control.cgi?command=1
   * 
   * @param param
   * @return
   */
  public String moveTo(int x, int y) {
    // if (x < 0)
    // return new String(Http.get(getControlUrl(x)));
    return null;
  }
  
  class StopEvent extends TimerTask {
    Timer timer;
    
    public StopEvent() {
      processCommand("decoder_control", 1);
    }
    
    @Override
    public void run() {
      // TODO Auto-generated method stub
      
    }
    
  }
  
  protected synchronized void scheduleStop(String name, long ms) {
    if (moves.containsKey(name)) {
      // already have a stop scheduled - cancel it re-schedule
      Timer t = moves.get(name);
      t.cancel();
    }
    
    TimerTask task = new TimerTask() {
      public void run() {
        
      }
    };
    
    Timer timer = new Timer("Timer");
    timer.schedule(task, ms);
    moves.put(name, timer);
  }

  /**
   * relative move
   * @param x
   * @return
   */
  public String moveX(int x) {
    scheduleStop("x", 1000);
    // if (x < 0)
    // return new String(Http.get(getControlUrl(x)));
    return null;
  }

  public String setUrl(String url) {
    baseUrl = url;
    return url;
  }

  // FIXME Http.get should throw the error and we should report it
  public String getStatus() {
    return processCommand("get_status");
  }

  public void setUser(String user) {
    this.user = user;
  }

  public void setPwd(String pwd) {
    this.pwd = pwd;
  }

  public final static SerializableImage publishFrame(String source, BufferedImage img) {
    SerializableImage si = new SerializableImage(img, source);
    return si;
  }

  @Override
  public void attach(ServoControl servo, int pinOrAddress) throws Exception {
    log.info("attach");
    
  }

  @Override
  public void attachServoControl(ServoControl sc) {
    log.info("attachServoControl");
    
  }

  @Override
  public void onServoMoveTo(ServoControl servo) {
    log.info("onServoMoveTo");    
  }

  @Override
  public void onServoStop(ServoControl servo) {
   log.info("onServoStop");
  }

  @Override
  public void onServoWriteMicroseconds(ServoControl servo, int uS) {
    log.info("onServoWriteMicroseconds");    
  }

  @Override
  public void onServoSetSpeed(ServoControl servo) {
    log.info("onServoSetSpeed");    
  }

  @Override
  public void onServoEnable(ServoControl servo) {
    log.info("onServoEnable");        
  }

  @Override
  public void onServoDisable(ServoControl servo) {
    log.info("onServoDisable");        
    
  }

  public void attachServoY(String string) {
    // TODO Auto-generated method stub
    
  }

  public void attachServoX(String string) {
    // TODO Auto-generated method stub
    
  }



  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);

      // IpCamera foscam = (IpCamera) Runtime.start("foscam", "IpCamera");
      
      OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      Servo x = (Servo) Runtime.start("x", "Servo");
      Servo y = (Servo) Runtime.start("y", "Servo");
      x.setPin(3);
      y.setPin(9);
      
      // TODO - test - should be string based - does string base work?
      arduino.attach(x);
      arduino.attach(y);
      
      /*
      foscam.attachServoX("x");
      foscam.attachServoY("x");
      
      foscam.setUrl("http://192.168.0.37");
      foscam.setUser("admin");
      foscam.setPwd("admin");
      log.info("status {}", foscam.getStatus());
      foscam.center();


      for (int i = 0; i < 255; ++i) {
        foscam.moveTo(-10, 0);
        foscam.moveTo(0, 0);
        foscam.moveTo(10, 0);
      }
      */

    } catch (Exception e) {
      Logging.logError(e);
    }

  }


}
