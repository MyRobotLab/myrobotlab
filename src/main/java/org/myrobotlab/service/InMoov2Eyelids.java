package org.myrobotlab.service;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * InMoov2Eyelids - The InMoov eyelids. This will allow control of the eyelids
 * servo common both eyelids use only one servo ( left )
 */
public class InMoov2Eyelids extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoov2Eyelids.class);

  transient public ServoControl eyelidLeft;
  transient public ServoControl eyelidRight;
  private transient ServoController controller;
  // todo : make this deprecated

  transient Timer blinkEyesTimer = new Timer();

  public void blink() {

    // TODO: clean stop autoblink if tracking ...
    double tmpSpeed = ThreadLocalRandom.current().nextInt(40, 150 + 1);
    setSpeed(tmpSpeed, tmpSpeed);
    moveToBlocking(180, 180);
    moveToBlocking(0, 0);

  }

  class blinkEyesTimertask extends TimerTask {
    @Override
    public void run() {
      int delay = ThreadLocalRandom.current().nextInt(10, 40 + 1);
      blinkEyesTimer.schedule(new blinkEyesTimertask(), delay * 1000);

      blink();
      // random double blink
      if (ThreadLocalRandom.current().nextInt(0, 1 + 1) == 1) {
        sleep(ThreadLocalRandom.current().nextInt(1000, 2000 + 1));
        blink();
      }
    }
  }

  public void releaseService() {
    try {
      disable();
      releasePeers();
      super.releaseService();
    } catch (Exception e) {
      error(e);
    }
  }

  public void test() {
    if (controller == null) {
      error("servo controller is null");
    }
    eyelidLeft.moveTo(179.0);
    sleep(300);
    eyelidRight.moveToBlocking(1.0);
  }

  static public void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      VirtualArduino v = (VirtualArduino) Runtime.start("virtual", "VirtualArduino");
      v.connect("COM4");
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.connect("COM4");
      InMoov2Eyelids eyelids = (InMoov2Eyelids) Runtime.start("i01.eyelids", "InMoov2Eyelids");
      eyelids.attach(arduino, 2, 3);
      eyelids.setAutoDisable(true);
      eyelids.autoBlink(true);
      sleep(10000);
      eyelids.autoBlink(false);
    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  public InMoov2Eyelids(String n, String id) {
    super(n, id);
    if (eyelidLeft == null) {
      eyelidLeft = (ServoControl) createPeer("eyelidLeft");
    }
    if (eyelidRight == null) {
      eyelidRight = (ServoControl) createPeer("eyelidRight");
    }
  }

  public void autoBlink(boolean param) {
    if (blinkEyesTimer != null) {
      blinkEyesTimer.cancel();
      blinkEyesTimer = null;
    }
    if (param) {
      blinkEyesTimer = new Timer();
      new blinkEyesTimertask().run();
    }
  }

  public void attach(ServoController controller, Integer eyelidLeftPin, Integer eyelidRightPin) throws Exception {

    if (this.controller != null) {
      log.info("controller already attached - detaching {} before attaching {}", this.controller.getName(), controller.getName());
      eyelidLeft.detach();
      eyelidRight.detach();
    }
    this.controller = controller;

    eyelidLeft.setPin(eyelidLeftPin);
    eyelidRight.setPin(eyelidRightPin);

    eyelidLeft.attach(controller);
    eyelidRight.attach(controller);
  }

  public void detach(ServoController controller) throws Exception {

    if (this.controller == null) {
      log.info("controller not attached - ", this.controller.getName(), controller.getName());
      return;
    }
    eyelidLeft.detach();
    eyelidRight.detach();
  }

  public boolean enable() {

    sleep(InMoov2.attachPauseMs);
    eyelidLeft.enable();
    sleep(InMoov2.attachPauseMs);
    eyelidRight.enable();
    sleep(InMoov2.attachPauseMs);
    return true;
  }

  @Override
  public void broadcastState() {
    // notify the gui
    eyelidLeft.broadcastState();
    eyelidRight.broadcastState();

  }

  public void disable() {
    if (eyelidLeft != null) {
      eyelidLeft.disable();
      sleep(InMoov2.attachPauseMs);
    }
    if (eyelidRight != null) {
      eyelidRight.disable();
      sleep(InMoov2.attachPauseMs);
    }

  }

  public long getLastActivityTime() {
    long minLastActivity = Math.max(eyelidLeft.getLastActivityTime(), eyelidRight.getLastActivityTime());
    return minLastActivity;
  }

  public String getScript(String inMoovServiceName) {
    return String.format(Locale.ENGLISH, "%s.moveEyelids(%.2f,%.2f)\n", inMoovServiceName, eyelidLeft.getCurrentInputPos(), eyelidRight.getCurrentInputPos());
  }

  public void moveTo(double eyelidLeftPos, double eyelidRightPos) {
    if (log.isDebugEnabled()) {
      log.debug("{} moveTo {} {}", getName(), eyelidLeftPos, eyelidRightPos);
    }
    if (eyelidLeft != null) {
      eyelidLeft.moveTo(eyelidLeftPos);
    }
    if (eyelidRight != null) {
      eyelidRight.moveTo(eyelidRightPos);
    }
  }

  public void moveToBlocking(double eyelidLeftPos, double eyelidRightPos) {
    log.info("init {} moveToBlocking ", getName());
    moveTo(eyelidLeftPos, eyelidRightPos);
    waitTargetPos();
    log.info("end {} moveToBlocking ", getName());
  }

  public void waitTargetPos() {
    eyelidLeft.waitTargetPos();
    if (eyelidRight != null) {
      eyelidRight.waitTargetPos();
    }
  }

  // FIXME - releasePeers()
  public void release() {
    disable();
  }

  public void rest() {
    eyelidLeft.rest();
    eyelidRight.rest();
  }

  @Override
  public boolean save() {
    super.save();
    eyelidLeft.save();
    eyelidRight.save();
    return true;
  }

  @Override
  public void startService() {
    super.startService();
    eyelidLeft.setRest(0.0);
    eyelidRight.setRest(0.0);
    setSpeed(100.0, 100.0);
  }

  public void setAutoDisable(Boolean param) {
    eyelidLeft.setAutoDisable(param);
    eyelidRight.setAutoDisable(param);
  }

  public void setSpeed(Double eyelidLeftSpeed, Double eyelidRightSpeed) {
    if (eyelidLeft != null) {
      eyelidLeft.setSpeed(eyelidLeftSpeed);
    }
    if (eyelidLeft != null) {
      eyelidRight.setSpeed(eyelidRightSpeed);
    }
  }
}
