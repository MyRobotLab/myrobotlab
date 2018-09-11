package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.jme3.interfaces.Jme3App;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.interfaces.Simulator;
import org.myrobotlab.virtual.VirtualMotor;
import org.myrobotlab.virtual.VirtualServo;
import org.slf4j.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;

public class JMonkeyEngine extends Service implements Simulator {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(JMonkeyEngine.class);

  // TODO - make intermediate class - which has common interface to grab shapes/boxes
  transient Jme3App currentApp;
  
  transient Map<String, Jme3App> apps = new HashMap<String, Jme3App>();
  
  String defaultAppType = "DefaultApp";
  
  public JMonkeyEngine(String n) {
    super(n);
  }
  
  public Jme3App start(){
    return start(defaultAppType, defaultAppType);
  }
  
  // dynamic create of type... TODO fix name start --> create
  public Jme3App start(String appName, String appType){
    if (!apps.containsKey(appType)){
      // create app
      Jme3App jme3 = (Jme3App)Instantiator.getNewInstance(String.format("org.myrobotlab.jme3.%s", appType));
      if (jme3 == null){
        error("could not instantiate %s", appType);
        return jme3;
      }
      
      SimpleApplication app = jme3.getApp();
      
      // start it
      AppSettings settings = new AppSettings(true);
      settings.setResolution(640,480);
      //settings.setEmulateMouse(false);
      // settings.setUseJoysticks(false);
      settings.setUseInput(true);
      settings.setAudioRenderer(null);
      app.setSettings(settings);
      app.setShowSettings(false);
      app.start();
      
      apps.put(appName, jme3);
      currentApp = jme3;
      return currentApp;
    }
    warn("already started app %s", appType);
    return null;
  }
  

  
  @Override
  public void startService(){
    super.startService();
    start();
  }

  @Override
  public void stopService(){

    for(String name : apps.keySet()){
      try {
      Jme3App jme3 = apps.get(name);
      SimpleApplication app = jme3.getApp();
      app.getRootNode().detachAllChildren();
      app.getGuiNode().detachAllChildren();
      app.stop();
      //app.destroy();
      } catch(Exception e){
        log.error("releasing jme3 app threw", e);
      }
    }
    super.stopService();
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

    ServiceType meta = new ServiceType(JMonkeyEngine.class.getCanonicalName());
    meta.addDescription("is a 3d game engine, used for simulators");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    // TODO: extract version numbers like this into a constant/enum   
    String jmeVersion = "3.2.0-stable";
    meta.addDependency("org.jmonkeyengine", "jme3-core", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-desktop", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-lwjgl", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-jogg", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-niftygui", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-bullet", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-bullet-native", jmeVersion);
    meta.addDependency("org.jmonkeyengine", "jme3-niftygui", jmeVersion);
    // jbullet ==> org="net.sf.sociaal" name="jME3-jbullet" rev="3.0.0.20130526"
    meta.addCategory("simulator");
    return meta;
  }

  public Jme3App getApp() {
    return currentApp;
  }

  public static void main(String[] args) {
    try {

      Runtime.start("gui", "SwingGui");
      Servo servo = (Servo)Runtime.start("servo", "Servo");
      VirtualArduino virtual = (VirtualArduino)Runtime.start("virtual", "VirtualArduino");
      Arduino arduino = (Arduino)Runtime.start("arduino", "Arduino");
      
      // create the virtual hardware
      virtual.connect("COM5");
      
      // connect the service to the port
      arduino.connect("COM5");
      //jme3.create(servo);
      arduino.attach(servo, 7);
      
      
      JMonkeyEngine jmonkey = (JMonkeyEngine)Runtime.start("jmonkey", "JMonkeyEngine");
      virtual.attachSimulator(jmonkey);
      servo.moveTo(30);
      
      Thread.sleep(2000);

      
      jmonkey.releaseService();
      
      // NO NO NO - listen to Runtime , startup create all that can be created
      // listen to Runtime - create new for anything which appears to be new and can be created
      // attach a simulator to a virtual device
      // which listens on a serial port
      // virtual.attach(jme3);
      
      // NO NO NO X 2 - there is no spatial information @ Runtime nor when a service is newly created
      // So it would be better if the "Simulator" could ingest configuration and bind its objects by name
      // 'binding' - bind by name, but a typed reference is a good start
      
      // jme3.create(servo);
      

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @Override
  public VirtualServo createVirtualServo(String name) {
    return currentApp.createVirtualServo(name);
  }
/*
  @Override
  public Object create(ServiceInterface service) {
    // TODO Auto-generated method stub
    return null;
  }
*/

  @Override
  public VirtualMotor createVirtualMotor(String name) {
    // TODO Auto-generated method stub
    return null;
  }

}
