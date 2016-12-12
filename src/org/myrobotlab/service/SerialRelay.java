package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.SerialRelayData;
import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.myrobotlab.service.interfaces.SerialRelayListener;
import org.slf4j.Logger;


public class SerialRelay extends Service implements SerialDevice, DeviceControl {


  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(SerialRelay.class);
  
  transient Arduino controller = null;

  private transient SerialRelayListener listener;

  private int controllerAttachAs;
  

  public SerialRelay(String n) {
    super(n);
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

    ServiceType meta = new ServiceType(SerialRelay.class.getCanonicalName());
    meta.addDescription("Relaying Serial data to a different serial port on mega Arduino");
    meta.setAvailable(false); // false if you do not want it viewable in a gui
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("general");
    return meta;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("template", "SerialRelay");
      Runtime.start("gui", "GUIService");
      Runtime.start("python", "Python");
      
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.connect("COM15");
      Arduino arduino1 = (Arduino) Runtime.start("arduino1", "Arduino");
      arduino1.connect(arduino, "Serial1");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  @Override
  public void open(String name) throws Exception {
  	// TODO Auto-generated method stub
  	
  }
  
  @Override
  public int read() throws Exception {
  	// TODO Auto-generated method stub
  	return 0;
  }
  
  @Override
  public void write(byte[] data) throws Exception {
    int[] cdata = new int[data.length];
    for (int i=0; i<data.length; i++){
      cdata[i] = data[i];
    }
  	controller.msg.serialRelay(controller.getDeviceId(this), cdata);
  }
  
  @Override
  public void write(int data) throws Exception {
//    controller.msg.serialRelay(controller.getDeviceId(this), new byte[] {(byte)data});
    controller.msg.serialRelay(controller.getDeviceId(this), new int[] {data});
  }

  public void attach(Arduino controller, SerialRelayListener listener, int controllerAttachAs) {
    this.controller = controller;
    this.listener = listener;
    this.controllerAttachAs = controllerAttachAs;
    if (controller != null) {
      subscribe(controller.getName(),"publishSerialData");
      controller.serialAttach(this, controllerAttachAs);
    }
  }

  @Override
  public void setController(DeviceController controller) {
    // TODO Auto-generated method stub
    if (!isAttached()) {
      attach((Arduino)controller, listener, controllerAttachAs);
    }
  }

  @Override
  public DeviceController getController() {
    return controller;
  }

  @Override
  public boolean isAttached() {
    if (controller != null && controller.isConnected() && controller.getDeviceId(this) != null) {
      return true;
    }
    return false;
  }

  @Override
  public void unsetController() {
    controller.deviceDetach(this);
    controller = null;
  }

  public int[] onSerialData(SerialRelayData data) {
    if (data.deviceId == controller.getDeviceId(this)){
      if (listener instanceof Arduino){
        for(int newByte:data.data){
          ((Arduino) listener).onByte(((int) newByte & 0xFF));
        }
      }
      return data.data;
    }
    return new int[0];//new byte[] {0};
  }

}
