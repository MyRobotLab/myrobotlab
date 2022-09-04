package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.MouthControlConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.NeoPixelControl;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.SpeechListener;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

/**
 * 
 * MouthControl - This service will animate a jaw servo to move as its speaking
 * It's peers are the jaw servo, speech service and an arduino.
 *
 */
public class MouthControl extends Service implements SpeechListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(MouthControl.class);

  int mouthClosedPos;

  int mouthOpenedPos;

  int delaytime;

  int delaytimestop;

  int delaytimeletter;

  /**
   * name of servo to use for jaw movement
   */
  protected String jaw;

  /**
   * name of neopixel for chappie or equalizer like mouth lights
   */
  protected String neoPixel;

  /**
   * This variable isn't used - its just a place holder to remember what
   * SpeechSynthesis its attached to
   */
  String mouth;

  public MouthControl(String n, String id) {
    super(n, id);
    registerForInterfaceChange(ServoControl.class);
    registerForInterfaceChange(SpeechSynthesis.class);
    registerForInterfaceChange(NeoPixelControl.class);
  }

  public void attach(Attachable attachable) {
    if (attachable instanceof SpeechSynthesis) {
      ((SpeechSynthesis) attachable).attachSpeechListener(getName());
      mouth = attachable.getName();
    } else if (attachable instanceof ServoControl) {
      jaw = attachable.getName();
      broadcastState();
    } else if (attachable instanceof NeoPixel) {
      neoPixel = attachable.getName();
      broadcastState();
    } else {
      error("MouthControl can't attach : %s", attachable);
    }
  }

  @Override
  public void detach(Attachable attachable) {
    if (attachable instanceof SpeechSynthesis) {
      ((SpeechSynthesis) attachable).detachSpeechListener(getName());
      if (mouth != null) {
        send(mouth, "detach", getName());
      }
      mouth = null;
    } else if (attachable instanceof ServoControl) {
      // jaw = null;
      jaw = null;
      broadcastState();
    } else {
      error("MouthControl can't detach from : %s", attachable);
    }
  }

  @Override
  public void detach() {
    jaw = null;
    neoPixel = null;
    if (mouth != null) {
      send(mouth, "detach", getName());
    }
    mouth = null;
  }

  public String[] getCategories() {
    return new String[] { "control", "mouth" };
  }

  @Override
  public String getDescription() {
    return "mouth movements or light flashing based on spoken text";
  }

  public synchronized void onStartSpeaking(String text) {
    if (neoPixel != null) {
      startMouthAnimation();
    }
    if (jaw != null) {
      moveMouthServo(text);
    }
  }

  public void startMouthAnimation() {
    send(neoPixel, "playAnimation", "Equalizer");
  }

  public void stopMouthAnimation() {
    send(neoPixel, "clear");
  }

  private void moveJaw(double pos) {
    if (jaw != null) {
      send(jaw, "moveTo", pos);
    }
  }

  // FIXME - this will tie up the calling thread which will be coming from
  // the SpeechSynthesis service (not nice) it should manage its own
  // loop in its own thread
  public void moveMouthServo(String text) {
    log.info("move moving to :" + text);

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
          // jaw.moveTo((double) mouthOpenedPos); // # move the servo to the
          moveJaw((double) mouthOpenedPos);
          // open spot
          ison = true;
          sleep(delaytime);
          // jaw.moveTo((double) mouthClosedPos);// #// close the servo
          moveJaw((double) mouthClosedPos);

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
    if (neoPixel != null) {
      stopMouthAnimation();
    }

    if (jaw != null) {
      // FIXME should just stop
      moveJaw((double) mouthClosedPos);
    }
  }

  /**
   * Set the delays of the jaw servo
   * 
   * @param delaytime
   * @param delaytimestop
   * @param delaytimeletter
   */
  public void setDelays(Integer delaytime, Integer delaytimestop, Integer delaytimeletter) {
    this.delaytime = delaytime;
    this.delaytimestop = delaytimestop;
    this.delaytimeletter = delaytimeletter;
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

    config.jaw = jaw;
    config.mouth = mouth;
    config.mouthClosedPos = mouthClosedPos;
    config.mouthOpenedPos = mouthOpenedPos;
    config.delaytime = delaytime;
    config.delaytimestop = delaytimestop;
    config.delaytimeletter = delaytimeletter;
    config.neoPixel = neoPixel;

    return config;
  }

  public ServiceConfig apply(ServiceConfig c) {
    MouthControlConfig config = (MouthControlConfig) c;

    mouthClosedPos = config.mouthClosedPos;
    mouthOpenedPos = config.mouthOpenedPos;
    delaytime = config.delaytime;
    delaytimestop = config.delaytimestop;
    delaytimeletter = config.delaytimeletter;
    jaw = config.jaw;
    neoPixel = config.neoPixel;

    // mouth needs to attach to us
    // it needs to create notify entries
    // so we fire a message to attach to us
    mouth = config.mouth;
    if (config.mouth != null) {
      mouth = config.mouth;
      send(mouth, "attach", getName());
    }

    return c;
  }

  public static void main(String[] args) {
    try {
      System.setProperty("java.version", "11.0");
      LoggingFactory.init(Level.INFO);

      // MouthControl mouthcontrol = (MouthControl) Runtime.start("mc",
      // "MouthControl");
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      // webgui.setSsl(true);
      webgui.autoStartBrowser(false);
      webgui.setPort(8889);
      webgui.startService();

      boolean done = true;
      if (done) {
        return;
      }

      Runtime.start("s1", "Servo");
      Runtime.start("mouth1", "LocalSpeech");
      Runtime.start("mega", "Arduino");
      Runtime.start("neo", "NeoPixel");

      // Runtime.start("python", "Python");

      // mouthcontrol.onStartSpeaking("test on");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}