package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.MyoData;
import org.myrobotlab.service.interfaces.MyoDataListener;
import org.myrobotlab.service.interfaces.MyoDataPublisher;
import org.slf4j.Logger;

import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.FirmwareVersion;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.enums.Arm;
import com.thalmic.myo.enums.PoseType;
import com.thalmic.myo.enums.UnlockType;
import com.thalmic.myo.enums.VibrationType;
import com.thalmic.myo.enums.WarmupResult;
import com.thalmic.myo.enums.WarmupState;
import com.thalmic.myo.enums.XDirection;

/**
 * 
 * MyoThalmic - This service provides connectivity to the Myo band.
 * https://www.myo.com/ It provides orientation tracking infromation such as
 * roll,pitch and yaw. In addition it can detect a "pose" or gesture made by the
 * hand while it's worn.
 * 
 * REST, FIST, WAVE_IN, WAVE_OUT, FINGERS_SPREAD, DOUBLE_TAP, UNKNOWN
 * 
 * The addPoseListener will wire data the orientation and pose data to another
 * service.
 * 
 *  https://developer.thalmic.com/downloads
 *  
 *  https://github.com/NicholasAStuart/myo-java
 *  
 *  https://github.com/NicholasAStuart/myo-java-JNI-Library
 * 
 */
public class MyoThalmic extends Service implements DeviceListener, MyoDataListener, MyoDataPublisher {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(MyoThalmic.class);

  static int scale = 180;
  double rollW = 0;
  double pitchW = 0;
  double yawW = 0;
  transient Pose currentPose;
  transient Arm whichArm;

  transient Myo thalmicMyo = null;
  transient Hub thalmicHub = null;
  
  transient MyoProxy myo = null;
  transient HubProxy hub = null;
  
  transient HubThread hubThread = null;

  MyoData myodata = new MyoData();
  boolean delta = false;
  boolean locked = true;
  int batterLevel = 0;

  boolean isConnected = false;

  class HubThread extends Thread {
    public boolean running = false;
    MyoThalmic myService = null;

    public HubThread(MyoThalmic myService) {
      this.myService = myService;
    }

    public void run() {
      running = true;
      while (running) {
        hub.run(1000 / 20);
        // log.info(myService.toString());
      }
    }
  }
  
  public class HubProxy {
    
    public HubProxy() {
      if (!isVirtual && hub == null){
        thalmicHub = new Hub("com.example.hello-myo");
      }
    }
    
    public void run(int x){
      if (!isVirtual){
        thalmicHub.run(x);
      }else {
        sleep(1000);
      }
    }

    public void removeListener(DeviceListener myoThalmic) {
      if (!isVirtual){
        thalmicHub.removeListener(myoThalmic);
      }
    }

    public MyoProxy waitForMyo(int i) {
      if (!isVirtual){
        thalmicMyo =  thalmicHub.waitForMyo(i);
      } 
      
      myo = new MyoProxy();
      
      return myo;
    }

    public void addListener(DeviceListener myoThalmic) {
      if (!isVirtual){
        thalmicHub.addListener(myoThalmic);
      }
    }
  }

  public class MyoProxy {

    public void requestBatteryLevel() {
      if (!isVirtual){
        thalmicMyo.requestBatteryLevel();
      }
    }

    public void lock() {
      if (!isVirtual){
        thalmicMyo.lock();
      }
    }

    public void unlock(UnlockType unlockTimed) {
      if (!isVirtual){
        thalmicMyo.unlock(unlockTimed);
      }
    }
    
  }
  
  public void disconnect() {

    if (hubThread != null) {
      hubThread.running = false;
      hubThread = null;
    }

    hub.removeListener(this);

    isConnected = false;
    broadcastState();
  }

  public void connect() {
    if (hub == null) {
      // FIXME - put in connect
      try {
        currentPose = new Pose();
        hub = new HubProxy();
      } catch (Exception e) {
        Logging.logError(e);
      }
    }

    if (myo == null) {
      info("Attempting to find a Myo...");
      myo = hub.waitForMyo(10000);
    }

    if (myo == null) {
      error("Unable to find a Myo");
      isConnected = false;
      return;
    }

    info("Connected to a Myo armband!");
    hub.addListener(this);

    if (hubThread == null) {
      hubThread = new HubThread(this);
      hubThread.start();
    }

    isConnected = true;
    myo.requestBatteryLevel();
    broadcastState();
  }

  public MyoThalmic(String n) {
    super(n);
  }

  @Override
  public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
    
    Quaternion normalized = rotation.normalized();

    double roll = Math.atan2(2.0f * (normalized.getW() * normalized.getX() + normalized.getY() * normalized.getZ()),
        1.0f - 2.0f * (normalized.getX() * normalized.getX() + normalized.getY() * normalized.getY()));
    double pitch = Math.asin(2.0f * (normalized.getW() * normalized.getY() - normalized.getZ() * normalized.getX()));
    double yaw = Math.atan2(2.0f * (normalized.getW() * normalized.getZ() + normalized.getX() * normalized.getY()),
        1.0f - 2.0f * (normalized.getY() * normalized.getY() + normalized.getZ() * normalized.getZ()));

    rollW = Math.round((roll + Math.PI) / (Math.PI * 2.0) * scale);
    pitchW = Math.round((pitch + Math.PI / 2.0) / Math.PI * scale);
    yawW = Math.round((yaw + Math.PI) / (Math.PI * 2.0) * scale);

    delta = (myodata.roll - rollW != 0) || (myodata.pitch - pitchW != 0) || (myodata.yaw - yawW != 0);

    if (delta) {
      myodata.roll = rollW;
      myodata.pitch = pitchW;
      myodata.yaw = yawW;

      myodata.timestamp = timestamp;
      invoke("publishMyoData", myodata);
    }
  }

  @Override
  public void onPose(Myo myo, long timestamp, Pose pose) {
    currentPose = pose;
    myodata.currentPose = pose.getType().toString();
    if (currentPose.getType() == PoseType.FIST) {
      myo.vibrate(VibrationType.VIBRATION_MEDIUM);
    }
    invoke("publishPose", pose);
    invoke("publishMyoData", myodata);
  }

  public void addPoseListener(Service service) {
    addListener("publishPose", service.getName(), "onPose");
  }

  public Pose publishPose(Pose pose) {
    return pose;
  }

  /*
   * @Override public void onArmSync(Myo myo, long timestamp, Arm arm,
   * XDirection xDirection) { whichArm = arm; }
   */

  @Override
  public void onArmUnsync(Myo myo, long timestamp) {
    whichArm = null;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("\r");

    String xDisplay = String.format("[%s%s]", repeatCharacter('*', (int) rollW), repeatCharacter(' ', (int) (scale - rollW)));
    String yDisplay = String.format("[%s%s]", repeatCharacter('*', (int) pitchW), repeatCharacter(' ', (int) (scale - pitchW)));
    String zDisplay = String.format("[%s%s]", repeatCharacter('*', (int) yawW), repeatCharacter(' ', (int) (scale - yawW)));

    String armString = null;
    if (whichArm != null) {
      armString = String.format("[%s]", whichArm == Arm.ARM_LEFT ? "L" : "R");
    } else {
      armString = String.format("[?]");
    }
    String poseString = null;
    if (currentPose != null) {
      String poseTypeString = currentPose.getType().toString();
      poseString = String.format("[%s%" + (scale - poseTypeString.length()) + "s]", poseTypeString, " ");
    } else {
      poseString = String.format("[%14s]", " ");
    }
    builder.append(xDisplay);
    builder.append(yDisplay);
    builder.append(zDisplay);
    builder.append(armString);
    builder.append(poseString);
    return builder.toString();
  }

  public String repeatCharacter(char character, int numOfTimes) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < numOfTimes; i++) {
      builder.append(character);
    }
    return builder.toString();
  }

  @Override
  public void onPair(Myo myo, long timestamp, FirmwareVersion firmwareVersion) {
    info("onPair");
  }

  @Override
  public void onUnpair(Myo myo, long timestamp) {
    info("onUnpair");
  }

  @Override
  public void onConnect(Myo myo, long timestamp, FirmwareVersion firmwareVersion) {
    info("onConnect");
  }

  @Override
  public void onDisconnect(Myo myo, long timestamp) {
    info("onDisconnect");
  }

  @Override
  public void onUnlock(Myo myo, long timestamp) {
    // info("onUnlock");
    locked = false;
    invoke("publishLocked", locked);
  }

  @Override
  public void onLock(Myo myo, long timestamp) {
    // info("onLock");
    locked = true;
    invoke("publishLocked", locked);
  }

  public Boolean publishLocked(Boolean b) {
    return b;
  }

  @Override
  public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel) {
    // info("onAccelerometerData");
  }

  @Override
  public void onGyroscopeData(Myo myo, long timestamp, Vector3 gyro) {
    // info("onGyroscopeData");
  }

  @Override
  public void onRssi(Myo myo, long timestamp, int rssi) {
    info("onRssi");
  }

  @Override
  public void onEmgData(Myo myo, long timestamp, byte[] emg) {
    info("onEmgData");
  }

  public void lock() {
    myo.lock();
  }

  public void unlock() {
    myo.unlock(UnlockType.UNLOCK_TIMED);
  }

  /*
   * public void setLockingPolicy(String policy){ myo.setL
   * myo.setLockingPolicy("none") ; }
   */

  // //

  @Override
  public MyoData onMyoData(MyoData myodata) {
    return myodata;
  }

  @Override
  public MyoData publishMyoData(MyoData myodata) {

    return myodata;
  }

  public void addMyoDataListener(Service service) {
    addListener("publishMyoData", service.getName(), "onMyoData");
  }



  @Override
  public void onArmSync(Myo myo, long arg1, Arm arm, XDirection direction, WarmupState warmUpState) {
    log.info("onArmSync {}", arm);
    whichArm = arm;
    invoke("publishArmSync", arm);
  }

  public Arm publishArmSync(Arm arm) {
    return arm;
  }

  @Override
  public void onBatteryLevelReceived(Myo myo, long timestamp, int level) {
    batterLevel = level;
    log.info("onBatteryLevelReceived {} {}", timestamp, batterLevel);
    invoke("publishBatteryLevel", batterLevel);
  }

  public Integer publishBatteryLevel(Integer level) {
    return level;
  }

  @Override
  public void onWarmupCompleted(Myo myo, long unkown, WarmupResult warmUpResult) {
    log.info("onWarmupCompleted {} {}", unkown, warmUpResult);
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

    ServiceType meta = new ServiceType(MyoThalmic.class.getCanonicalName());
    meta.addDescription("Myo service to control with the Myo armband");
    meta.addCategory("control", "sensor");
    meta.addDependency("com.thalmic.myo", "0.9.0");
    return meta;
  }
  
  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      MyoThalmic myo = (MyoThalmic) Runtime.start("myo", "MyoThalmic");
      // myo.setVirtual(true);
      myo.connect();
      Runtime.start("webgui", "WebGui");

      /*
       * Hub hub = new Hub("com.example.hello-myo");
       * 
       * log.info("Attempting to find a Myo..."); log.info(
       * "Attempting to find a Myo");
       * 
       * Myo myodevice = hub.waitForMyo(10000);
       * 
       * if (myodevice == null) { throw new RuntimeException(
       * "Unable to find a Myo!"); }
       * 
       * log.info("Connected to a Myo armband!"); log.info(
       * "Connected to a Myo armband");
       * 
       * //DeviceListener dataCollector = new DataCollector();
       * //hub.addListener(myo);
       * 
       * while (true) { hub.run(1000 / 20); //System.out.print(dataCollector);
       * 
       * 
       * 
       * }
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
