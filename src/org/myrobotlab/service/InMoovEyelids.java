package org.myrobotlab.service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * InMoovEyelids - The inmoov eyelids. This will allow control of the eyelids servo 
 * common both eyelids use only one servo ( left )
 */
public class InMoovEyelids extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InMoovEyelids.class);

  transient public static Servo eyelidleft;
  transient public static Servo eyelidright;
  transient public Arduino arduino;

  static Timer blinkEyesTimer = new Timer();

  
  public static void blink()
  {
    
    if (!InMoov.RobotIsTrackingSomething() && !InMoov.RobotIsSleeping){
      int tmpVelo = ThreadLocalRandom.current().nextInt(40, 100 + 1);
      eyelidleft.setVelocity(tmpVelo);
      eyelidright.setVelocity(tmpVelo);
      if (eyelidleft != null) {
       eyelidleft.moveTo(180);
      }
     if (eyelidright != null) {
       eyelidright.moveTo(180);
      }
     sleep(ThreadLocalRandom.current().nextInt(500, 1000 + 1));
     if (eyelidleft != null) {
       eyelidleft.moveTo(0);
      }
     if (eyelidright != null) {
       eyelidright.moveTo(0);
      }
     }
  }
  
  static class blinkEyesTimertask extends TimerTask {
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

 

  static public void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      VirtualArduino v = (VirtualArduino)Runtime.start("virtual", "VirtualArduino");
      
      v.connect("COM4");
      InMoovEyelids eyelids = (InMoovEyelids) Runtime.start("i01.eyelids", "InMoovEyelids");
      eyelids.connect("COM4");
      Runtime.start("webgui", "WebGui");
      eyelids.test();
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
    
    arduino = (Arduino) createPeer("arduino");

    eyelidleft.setRest(0);
    eyelidright.setRest(0);
 
    setVelocity(50.0,50.0);
    
    enableAutoEnable(true);
    
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

  /*
   * attach all the servos - this must be re-entrant and accomplish the
   * re-attachment when servos are detached
   */
  @Deprecated
  public boolean attach() {
    return enable();
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
  
  public boolean connect(String port) throws Exception {
	  return connect(port,22,24);
	  
  }
  
  public boolean connect(String port,Integer eyelidleftPin,Integer eyelidrightPin) throws Exception {
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

    eyelidleft.attach(arduino, eyelidleftPin, eyelidleft.getRest(), eyelidleft.getVelocity());
    eyelidright.attach(arduino, eyelidrightPin, eyelidright.getRest(), eyelidright.getVelocity());
  

    broadcastState();
    return true;
  }

  @Deprecated
  public void detach() {
    if (eyelidleft != null) {
      eyelidleft.disable();
      sleep(InMoov.attachPauseMs);
    } 
    if (eyelidright != null) {
      eyelidright.disable();
      sleep(InMoov.attachPauseMs);
    }
   
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
    return String.format("%s.moveEyelids(%d,%d)\n", inMoovServiceName, eyelidleft.getPos(), eyelidright.getPos());
  }

  public boolean isAttached() {
    boolean attached = false;

    attached |= eyelidleft.isAttached();
    attached |= eyelidright.isAttached();
   

    return attached;
  }

  public void moveTo(Integer eyelidleft, Integer eyelidright) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("%s moveTo %d %d", getName(), eyelidleft, eyelidright));
    }
    InMoovEyelids.eyelidleft.moveTo(eyelidleft);
    InMoovEyelids.eyelidright.moveTo(eyelidright);
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

    //setVelocity(50.0, 50.0);
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

  public void setLimits(int eyelidleftMin, int eyelidleftMax, int eyelidrightMin, int eyelidrightMax) {
    eyelidleft.setMinMax(eyelidleftMin, eyelidleftMax);
    eyelidright.setMinMax(eyelidrightMin, eyelidrightMax);
    
  }

  // ------------- added set pins
  public void setpins(Integer eyelidleftPin, Integer eyelidrightPin) {
    // createPeers();
	  /*
    this.eyelidleft.setPin(eyelidleft);
    this.eyelidright.setPin(eyelidright);
    */
	  
    //Calamity: this seem incorrect. I think the pin should be set on the Servo service, not on the arduino directly
	    eyelidleft.enable(eyelidleftPin);
	    eyelidright.enable(eyelidrightPin);
  }



  /*
   * public boolean load() { super.load(); eyelidleft.load(); eyelidright.load();
   * lowStom.load(); return true; }
   */

  @Override
  public void startService() {
    super.startService();
    eyelidleft.startService();
    eyelidright.startService();
    arduino.startService();
  }

  
 
  
  public void test() {

    if (arduino == null) {
      error("arduino is null");
    }

    if (!arduino.isConnected()) {
      error("arduino not connected");
    }

    eyelidleft.moveTo(eyelidleft.getPos() + 2);
    eyelidright.moveTo(eyelidright.getPos() + 2);
    

    moveTo(35, 45);
    String move = getScript("i01");
    log.info(move);
  }

  /*
   * TODO: move this java doc, it's lost
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * return ServiceType - returns all the data
   * 
   */
  
  public void enableAutoEnable(Boolean param) {
    eyelidleft.enableAutoEnable(param);
    eyelidright.enableAutoEnable(param);
  }
  
  public void enableAutoDisable(Boolean param) {
    eyelidleft.enableAutoDisable(param);
    eyelidright.enableAutoDisable(param);
  }
  
  public void temporaryStopAutoDisable(Boolean param) {
    eyelidleft.temporaryStopAutoDisable(param);
    eyelidright.temporaryStopAutoDisable(param);
  }
  
   
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(InMoovEyelids.class.getCanonicalName());
    meta.addDescription("InMoov Eyelids");
    meta.addCategory("robot");

    meta.addPeer("eyelidleft", "Servo", "eyelidleft or both servo");
    meta.addPeer("eyelidright", "Servo", "Eyelid right servo");
    meta.addPeer("arduino", "Arduino", "Arduino controller for eyelids");

    return meta;
  }

  public void setVelocity(Double eyelidleft, Double eyelidright) {
    InMoovEyelids.eyelidleft.setVelocity(eyelidleft);
    InMoovEyelids.eyelidright.setVelocity(eyelidright);
   }
}
