package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

/**
 * 
 * MouthControl - This service will animate a jaw servo to move as its speaking
 * It's peers are the jaw servo, speech service and an arduino.
 *
 */
public class MouthControl extends Service {

  // TODO: remove Peer & Make it attachable between generic servoControl & SpeechSynthesis
  
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MouthControl.class.getCanonicalName());
  public int mouthClosedPos = 20;
  public int mouthOpenedPos = 4;
  public int delaytime = 75;
  public int delaytimestop = 150;
  public int delaytimeletter = 45;
  transient Servo jaw;
  transient Arduino arduino;
  transient SpeechSynthesis mouth;
  
  @Deprecated
  public boolean autoAttach = true;

  public MouthControl(String n) {
    super(n);
    jaw = (Servo) createPeer("jaw");
    arduino = (Arduino) createPeer("arduino");
    mouth = (SpeechSynthesis) createPeer("mouth");

    // TODO: mouth should probably implement speech synthesis.
    // in a way of speaking, one day, people may be able to read the lips
    // of the inmoov.. so you're synthesising speech in a mechanical way.
    // similar to sign language maybe?
    subscribe(mouth.getName(), "publishStartSpeaking");
    subscribe(mouth.getName(), "publishEndSpeaking");
  }

  // FIXME make interface
  public boolean connect(String port) throws Exception {
    startService(); // NEEDED? I DONT THINK SO....

    if (arduino == null) {
      error("arduino is invalid");
      return false;
    }

    arduino.connect(port);

    if (!arduino.isConnected()) {
      error("arduino %s not connected", arduino.getName());
      return false;
    }

    jaw.attach(arduino, 26);
    
    return true;
  }

  public Arduino getArduino() {
    return arduino;
  }

  public Servo getJaw() {
    return jaw;
  }

  public void setJaw(Servo jaw) {
    this.jaw = jaw;
  }

  public SpeechSynthesis getMouth() {
    return mouth;
  }

  public void setMouth(SpeechSynthesis mouth) {
    this.mouth = mouth;
    subscribe(mouth.getName(), "publishStartSpeaking");
  }

  public void setArduino(Arduino arduino) {
    this.arduino = arduino;
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
      if (!jaw.isEnabled()) {
        log.warn("{} not enabled", jaw.getName());
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
          if ((s == 'a' || s == 'e' || s == 'i' || s == 'o' || s == 'u' || s == 'y' || s == 'é' || s == 'è' || s == 'û' || s == 'и' || s == 'й' || s == 'У' || s == 'я' || s == 'э' || s == 'Ы' || s == 'ё' || s == 'ю' || s == 'е' || s == 'а' || s == 'о') && !ison) {
            jaw.moveTo(mouthOpenedPos); // # move the servo to the
            // open spot
            ison = true;
            sleep(delaytime);
            jaw.moveTo(mouthClosedPos);// #// close the servo
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
    //this will only work if the mouth animation ends before it end playing the voice.
    jaw.moveTo(mouthClosedPos);
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
//    if (closed < opened) {
//      jaw.map(closed, opened, closed, opened);
//    } else {
//      jaw.map(opened, closed, opened, closed);
//    }
  }

  @Override
  public void startService() {
    super.startService();
    jaw.startService();
    arduino.startService();
    // mouth.startService();
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

    meta.addPeer("jaw", "Servo", "shared Jaw servo instance");
    meta.addPeer("arduino", "Arduino", "shared Arduino instance");
    meta.addPeer("mouth", "MarySpeech", "shared Speech instance");

    return meta;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.DEBUG);
    try {
      // LoggingFactory.getInstance().setLevel(Level.INFO);
      MouthControl MouthControl = new MouthControl("MouthControl");
      MouthControl.startService();

      Runtime.createAndStart("gui", "SwingGui");

      MouthControl.onStartSpeaking("test on");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @Deprecated
  public void enableAutoAttach(boolean enable) {
  }
  
}
