package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.PinEvent;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinEventListener;
import org.slf4j.Logger;

public class Pir extends Service implements PinEventListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Pir.class);

	PinArrayControl pinControl;

	public Pir(String n) {
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

		ServiceType meta = new ServiceType(Pir.class.getCanonicalName());
		meta.addDescription("PIR - Passive Infrared Sensor");
		meta.setAvailable(true); // false if you do not want it viewable in a
									// gui
		// add dependency if necessary
		// meta.addDependency("org.coolproject", "1.0.0");
		meta.addCategory("sensor");
		return meta;
	}

	public void attach(PinArrayControl control, int pin) {
		this.pinControl = control;
		pinControl.attach(this, pin);
	}

	@Override
	public void onPinData(PinEvent pindata) {
		// TODO Auto-generated method stub

	}
	
	  public static void main(String[] args) {
		    try {

		      LoggingFactory.getInstance().configure();
		      LoggingFactory.getInstance().setLevel(Level.INFO);

		      Runtime.start("pir", "Pir");
		      Runtime.start("gui", "GUIService");

		    } catch (Exception e) {
		      Logging.logError(e);
		    }
		  }

}
