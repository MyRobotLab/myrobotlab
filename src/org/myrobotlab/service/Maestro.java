package org.myrobotlab.service;

import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.DeviceControl;
import org.myrobotlab.service.interfaces.Microcontroller;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * @author GroG http://www.pololu.com/product/1352
 *         http://www.pololu.com/product/1350
 *
 */
public class Maestro extends Service implements Microcontroller, ServoController {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Maestro.class);

  public static void main(String[] args) {
    LoggingFactory.getInstance().configure();
    LoggingFactory.getInstance().setLevel(Level.WARN);

    try {

      Maestro template = new Maestro("template");
      template.startService();

      Runtime.createAndStart("gui", "GUIService");
      /*
       * GUIService gui = new GUIService("gui"); gui.startService();
       */

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Maestro(String n) {
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

    ServiceType meta = new ServiceType(Maestro.class.getCanonicalName());
    meta.addDescription("Maestro USB Servo Controllers ");
    meta.addCategory("microcontroller");
    meta.addPeer("serial", "Serial", "Serial service is needed for Pololu");

    return meta;
 }


@Override
public void disconnect() {
	// TODO Auto-generated method stub
	
}

@Override
public boolean isConnected() {
	// TODO Auto-generated method stub
	return false;
}

@Override
public String getBoardType() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Integer getVersion() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public List<Pin> getPinList() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void sensorPollingStart(String name) {
	// TODO Auto-generated method stub
	
}

@Override
public void sensorPollingStop(String name) {
	// TODO Auto-generated method stub
	
}


@Override
public void servoDetach(Servo servo) {
	// TODO Auto-generated method stub
	
}

@Override
public void servoSweepStart(Servo servo) {
	// TODO Auto-generated method stub
	
}

@Override
public void servoSweepStop(Servo servo) {
	// TODO Auto-generated method stub
	
}

@Override
public void servoWrite(Servo servo) {
	// TODO Auto-generated method stub
	
}

@Override
public void servoWriteMicroseconds(Servo servo) {
	// TODO Auto-generated method stub
	
}

@Override
public boolean servoEventsEnabled(Servo servo, boolean enabled) {
	// TODO Auto-generated method stub
	return false;
}

@Override
public void setServoSpeed(Servo servo) {
	// TODO Auto-generated method stub
	
}


@Override
public void detachDevice(DeviceControl device) {
	// TODO Auto-generated method stub
	
}

@Override
public void attachDevice(DeviceControl device, Object... config) {
	// TODO Auto-generated method stub
	
}

@Override
public void servoAttach(Servo servo) {
	// TODO Auto-generated method stub
	
}

@Override
public void attach(Servo servo, int pin) {
	attachDevice(servo, new int[]{pin});
}

@Override
public void detach(Servo servo) {
	// TODO Auto-generated method stub
	
}

@Override
public Integer getPin(Servo servo) {
	// TODO Auto-generated method stub
	return null;
}


@Override
public void connect(String port, int rate, int databits, int stopbits, int parity) throws Exception {
	// TODO Auto-generated method stub
	
}


@Override
public void connect(String port) {
	// TODO Auto-generated method stub
	
}


}
