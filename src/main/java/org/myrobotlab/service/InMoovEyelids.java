package org.myrobotlab.service;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * InMoovEyelids - The inmoov eyelids. This will allow control of the eyelids servo 
 * common both eyelids use only one servo ( left )
 */
public class InMoovEyelids extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoovEyelids.class);

  transient public Servo eyelidleft;
  transient public Servo eyelidright;
  private transient ServoController controller;
  // todo : make this deprecated

  transient Timer blinkEyesTimer = new Timer();

  
  public void blink()
  {
    
    if (!InMoov.RobotIsTrackingSomething() && !InMoov.RobotIsSleeping){
      double tmpVelo = ThreadLocalRandom.current().nextInt(40, 150 + 1);
      setVelocity(tmpVelo,tmpVelo);     
      moveToBlocking(180,180);
      moveToBlocking(0,0);
  }
  }
  
  class blinkEyesTimertask extends TimerTask {
    @Override
    public void run() {
        int delay = ThreadLocalRandom.current().nextInt(10, 40 + 1);
        blinkEyesTimer.schedule(new blinkEyesTimertask(), delay*1000);
           
        blink();
        //random double blink
        if (ThreadLocalRandom.current().nextInt(0, 1 + 1)==1)
        {
          sleep(ThreadLocalRandom.current().nextInt(1000, 2000 + 1));
        blink(); 
        }
    }
  }

  public void test() {
    if (controller == null) {
      error("servo controller is null");
    }
    eyelidleft.moveTo(179);
    sleep(300);
    eyelidright.moveToBlocking(1);
  }

  static public void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      VirtualArduino v = (VirtualArduino)Runtime.start("virtual", "VirtualArduino");
      v.connect("COM4");
      Arduino arduino = (Arduino)Runtime.start("arduino", "Arduino");
      arduino.connect("COM4");
      InMoovEyelids eyelids = (InMoovEyelids) Runtime.start("i01.eyelids", "InMoovEyelids");
      eyelids.attach(arduino, 2, 3);
      eyelids.setAutoDisable(true);
      eyelids.autoBlink(true);
      sleep(10000);
      eyelids.autoBlink(false);
    } catch (Exception e) {
      Logging.logError(e);
    }
    
  }

  public InMoovEyelids(String n) {
    super(n);
    // createReserves(n); // Ok this might work but IT CANNOT BE IN SERVICE
    // FRAMEWORK !!!!!
    eyelidleft = (Servo) createPeer("eyelidleft");
    eyelidright = (Servo) createPeer("eyelidright");
    
    eyelidleft.setRest(0);
    eyelidright.setRest(0);
 
    setVelocity(50.0,50.0);

    
  }
  
 public void autoBlink(boolean param ) {
   if (blinkEyesTimer != null) {
     blinkEyesTimer.cancel();
     blinkEyesTimer = null;
   }
   if (param){
     blinkEyesTimer = new Timer();
     new blinkEyesTimertask().run();
   }
  }
 

 public void attach(ServoController controller, Integer eyeLidLeftPin, Integer eyeLidRightPin) throws Exception {
   
   if (this.controller != null) {
     log.info("controller already attached - detaching {} before attaching {}", this.controller.getName(), controller.getName());
     eyelidleft.detach();
     eyelidright.detach();
   }
   this.controller = controller;
   
   eyelidleft.attach(controller, eyeLidLeftPin);
   eyelidright.attach(controller, eyeLidRightPin);   
 }
 
 public void detach(ServoController controller) throws Exception {
   
   if (this.controller == null) {
     log.info("controller not attached - ", this.controller.getName(), controller.getName());
     return;
   } 
   eyelidleft.detach();
   eyelidright.detach();   
 }

  public boolean enable() {
    
    sleep(InMoov.attachPauseMs);
    eyelidleft.enable();
    sleep(InMoov.attachPauseMs);
    eyelidright.enable();
    sleep(InMoov.attachPauseMs);
    return true;
  } 
  

  @Override
  public void broadcastState() {
    // notify the gui
    eyelidleft.broadcastState();
    eyelidright.broadcastState();
   
  }  

  public void disable() {
    if (eyelidleft != null) {
      eyelidleft.disable();
      sleep(InMoov.attachPauseMs);
    } 
    if (eyelidright != null) {
      eyelidright.disable();
      sleep(InMoov.attachPauseMs);
    }
   
  }

  public long getLastActivityTime() {
    long minLastActivity = Math.max(eyelidleft.getLastActivityTime(), eyelidright.getLastActivityTime());
    return minLastActivity;
  }

  public String getScript(String inMoovServiceName) {
    return String.format(Locale.ENGLISH,"%s.moveEyelids(%.2f,%.2f)\n", inMoovServiceName, eyelidleft.getPos(), eyelidright.getPos());
  }

  public boolean isAttached() {
    boolean attached = false;

    attached |= eyelidleft.isAttached();
    attached |= eyelidright.isAttached();
   

    return attached;
  }

  public void moveTo(double eyelidleftPos, double eyelidrightPos) {
    if (log.isDebugEnabled()) {
      log.debug("{} moveTo {} {}", getName(), eyelidleftPos, eyelidrightPos);
    }
    if (eyelidleft != null) {
    eyelidleft.moveTo(eyelidleftPos);
    }
    if (eyelidright != null) {
    eyelidright.moveTo(eyelidrightPos);
    }
  }
  
  public void moveToBlocking(double eyelidleftPos, double eyelidrightPos) {
    log.info("init {} moveToBlocking ", getName() );
    moveTo(eyelidleftPos,eyelidrightPos);
    waitTargetPos();
    log.info("end {} moveToBlocking ", getName());
    }

  public void waitTargetPos() {
    eyelidleft.waitTargetPos();
    if (eyelidright != null) {
    eyelidright.waitTargetPos();
    }
    }

  // FIXME - releasePeers()
  public void release() {
    disable();
    if (eyelidleft != null) {
      eyelidleft.releaseService();
      eyelidleft = null;
    }
    if (eyelidright != null) {
      eyelidright.releaseService();
      eyelidright = null;
    }
  }

  public void rest() {
    eyelidleft.rest();
    eyelidright.rest();
  }

  @Override
  public boolean save() {
    super.save();
    eyelidleft.save();
    eyelidright.save();   
    return true;
  }


  @Override
  public void startService() {
    super.startService();
    eyelidleft.startService();
    eyelidright.startService();
  }

  
  
  public void setAutoDisable(Boolean param) {
    eyelidleft.setAutoDisable(param);
    eyelidright.setAutoDisable(param);
  }
  
  public void setOverrideAutoDisable(Boolean param) {
    eyelidleft.setOverrideAutoDisable(param);
    eyelidright.setOverrideAutoDisable(param);
  }
  
   
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(InMoovEyelids.class.getCanonicalName());
    meta.addDescription("InMoov Eyelids");
    meta.addCategory("robot");

    meta.addPeer("eyelidleft", "Servo", "eyelidleft or both servo");
    meta.addPeer("eyelidright", "Servo", "Eyelid right servo");

    return meta;
  }

  public void setVelocity(Double eyelidleftVelo, Double eyelidrightVelo) {
    if (eyelidleft != null) {
    eyelidleft.setVelocity(eyelidleftVelo);
    }
    if (eyelidleft != null) {
    eyelidright.setVelocity(eyelidrightVelo);
    }
   }
}
