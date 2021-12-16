package org.myrobotlab.service;

import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.MouthControlConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

/**
 * 
 * MouthControl - This service will animate a jaw servo to move as its speaking
 * It's peers are the jaw servo, speech service and an arduino.
 *
 */
public class MouthControl extends Service {

  // TODO: remove Peer & Make it attachable between generic servoControl &
  // SpeechSynthesis

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MouthControl.class);
  @Deprecated /* future releases members will be protected use appropriate methods instead */
  public int mouthClosedPos = 20;
  @Deprecated /* future releases members will be protected use appropriate methods instead */
  public int mouthOpenedPos = 4;
  @Deprecated /* future releases members will be protected use appropriate methods instead */
  public int delaytime = 75;
  @Deprecated /* future releases members will be protected use appropriate methods instead */
  public int delaytimestop = 150;
  @Deprecated /* future releases members will be protected use appropriate methods instead */
  public int delaytimeletter = 45;
  
  transient private ServoControl jaw;
  transient private SpeechSynthesis mouth;
  protected String jawName;
  protected String mouthName;

  public MouthControl(String n, String id) {
    super(n, id);
    registerForInterfaceChange(ServoControl.class);
    registerForInterfaceChange(SpeechSynthesis.class);
  }
  
  public void attach(Attachable attachable) {
    if (attachable instanceof SpeechSynthesis) {
      mouth = (SpeechSynthesis) attachable;
      subscribe(mouth.getName(), "publishStartSpeaking");
      subscribe(mouth.getName(), "publishEndSpeaking");
      mouthName = mouth.getName();
      broadcastState();
    } else if (attachable instanceof ServoControl) {
      jaw = (ServoControl) attachable;
      jawName = jaw.getName();
      broadcastState();
    } else {
      error("MouthControl can't attach : %s", attachable);
    }
  }

  @Override
  public void detach(Attachable attachable) {
    if (attachable instanceof SpeechSynthesis) {
      unsubscribe(mouth.getName(), "publishStartSpeaking");
      unsubscribe(mouth.getName(), "publishEndSpeaking");
      mouth = null;
      mouthName = null;
      broadcastState();
    } else if (attachable instanceof ServoControl) {
      jaw = null;
      jawName = null;
      broadcastState();
    } else {
      error("MouthControl can't detach from : %s", attachable);
    }
  }

  
  @Override
  public void detach() {
    if (jaw != null) {
      detach(jaw);
    }
    if (mouth != null) {
      detach(Runtime.getService(mouth.getName()));
    }
  }

  public String[] getCategories() {
    return new String[] { "control" };
  }

  @Override
  public String getDescription() {
    return "mouth movements based on spoken text";
  }

  public synchronized void onStartSpeaking(String text) {
    log.info("move moving to :" + text);
    if (jaw == null) {
      return;
    }
    boolean ison = false;
    String testword;
    String[] a = text.split(" ");
    for (int w = 0; w < a.length; w++) {
      // String word = ;
      // log.info(String.valueOf(a[w].length()));

      if (a[w].endsWith("es")) {
        testword = a[w].substring(0, a[w].length() - 2);

      } else if (a[w].endsWith("e")) {
        testword = a[w].substring(0, a[w].length() - 1);
        // log.info("e gone");
      } else {
        testword = a[w];

      }

      char[] c = testword.toCharArray();

      for (int x = 0; x < c.length; x++) {
        char s = c[x];
        // russian а ... <> a
        if ((s == 'a' || s == 'e' || s == 'i' || s == 'o' || s == 'u' || s == 'y' || s == 'é' || s == 'è' || s == 'û' || s == 'и' || s == 'й' || s == 'У' || s == 'я' || s == 'э'
            || s == 'Ы' || s == 'ё' || s == 'ю' || s == 'е' || s == 'а' || s == 'о') && !ison) {
          jaw.moveTo((double) mouthOpenedPos); // # move the servo to the
          // open spot
          ison = true;
          sleep(delaytime);
          jaw.moveTo((double) mouthClosedPos);// #// close the servo
        } else if (s == '.') {
          ison = false;
          sleep(delaytimestop);
        } else {
          ison = false;
          sleep(delaytimeletter); // # sleep half a second
        }

      }

      sleep(80);
    }

  }

  public synchronized void onEndSpeaking(String utterance) {
    log.info("Mouth control recognized end speaking.");
    // TODO: consider a jaw move to closed position
    // this will only work if the mouth animation ends before it end playing the
    // voice.
    jaw.moveTo((double) mouthClosedPos);
  }

  public void setDelays(Integer d1, Integer d2, Integer d3) {
    delaytime = d1;
    delaytimestop = d2;
    delaytimeletter = d3;
  }

  
  @Deprecated /* use setDelays */
  public void setdelays(Integer d1, Integer d2, Integer d3) {
    setDelays(d1, d2, d3);
  }

  public void setMouth(Integer closed, Integer opened) {
    mouthClosedPos = closed;
    mouthOpenedPos = opened;
  }
  
  @Deprecated /* use setMouth */
  public void setmouth(Integer closed, Integer opened) {
    setMouth(closed, opened);
  }
  
  @Override
  public ServiceConfig getConfig() {

    MouthControlConfig config = new MouthControlConfig();
    
    config.jaw = jawName;
    config.mouth = mouthName;
    config.mouthClosedPos = mouthClosedPos;
    config.mouthOpenedPos = mouthOpenedPos;
    config.delaytime = delaytime;
    config.delaytimestop = delaytimestop;
    config.delaytimeletter = delaytimeletter;
    
    return config;
  }

  @Override
  public ServiceConfig load(ServiceConfig c) {
    MouthControlConfig config = (MouthControlConfig) c;

    mouthClosedPos = config.mouthClosedPos;
    mouthOpenedPos = config.mouthOpenedPos;
    delaytime = config.delaytime;
    delaytimestop = config.delaytimestop;
    delaytimeletter = config.delaytimeletter;
    if (config.jaw != null) {
      try {
        attach(config.jaw);
      } catch(Exception e) {
        error(e);
      }
    }
    
    if (config.mouth != null) {
      try {
        attach(config.mouth);
      } catch(Exception e) {
        error(e);
      }
    }

    return c;
  }

  public Set<String> onSpeechSynthesis(Set<String> controllers){
    return controllers;
  }
  
  public Set<String> onServoControl(Set<String> controllers){
    return controllers;
  }

  public static void main(String[] args) {
    try {
      System.setProperty("java.version", "11.0");
      LoggingFactory.init(Level.INFO);

      Runtime.start("s1","Servo");
      Runtime.start("mouth1","LocalSpeech");
      
      MouthControl mouthcontrol = (MouthControl) Runtime.start("mouthcontrol", "MouthControl");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      // webgui.setSsl(true);
      webgui.autoStartBrowser(false);
      webgui.startService();
      
      Runtime.start("python","Python");

      mouthcontrol.onStartSpeaking("test on");
      
    } catch (Exception e) {
      Logging.logError(e);
    }
  }
}