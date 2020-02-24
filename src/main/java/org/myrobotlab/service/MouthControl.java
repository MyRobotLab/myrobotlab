package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
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
  public int mouthClosedPos = 20;
  public int mouthOpenedPos = 4;
  public int delaytime = 75;
  public int delaytimestop = 150;
  public int delaytimeletter = 45;
  transient private ServoControl jaw;
  transient private SpeechSynthesis mouth;

  public MouthControl(String n, String id) {
    super(n, id);
  }

  public void attach(Attachable attachable) {
    if (attachable instanceof SpeechSynthesis) {
      mouth = (SpeechSynthesis) attachable;
      subscribe(mouth.getName(), "publishStartSpeaking");
      subscribe(mouth.getName(), "publishEndSpeaking");
    } else if (attachable instanceof ServoControl) {
      jaw = (ServoControl) attachable;
    } else {
      log.error("MouthControl can't attach : {}", attachable);
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
          jaw.moveTo((double)mouthOpenedPos); // # move the servo to the
          // open spot
          ison = true;
          sleep(delaytime);
          jaw.moveTo((double)mouthClosedPos);// #// close the servo
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
    jaw.moveTo((double)mouthClosedPos);
  }

  public void setdelays(Integer d1, Integer d2, Integer d3) {
    delaytime = d1;
    delaytimestop = d2;
    delaytimeletter = d3;
  }

  public void setmouth(Integer closed, Integer opened) {
    // jaw.setMinMax(closed, opened);
    mouthClosedPos = closed;
    mouthOpenedPos = opened;

    // jaw.setMinMax(closed, opened);
    // if (closed < opened) {
    // jaw.map(closed, opened, closed, opened);
    // } else {
    // jaw.map(opened, closed, opened, closed);
    // }
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

    ServiceType meta = new ServiceType(MouthControl.class.getCanonicalName());
    meta.addDescription("Mouth movements based on spoken text");
    meta.addCategory("control");
    return meta;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.DEBUG);
    try {
      // LoggingFactory.getInstance().setLevel(Level.INFO);
      MouthControl MouthControl = (MouthControl)Runtime.start("MouthControl", "MouthControl");
      MouthControl.startService();

      Runtime.createAndStart("gui", "SwingGui");

      MouthControl.onStartSpeaking("test on");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }
}
