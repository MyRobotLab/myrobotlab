package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.leap.LeapMotionListener;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.thalmic.*;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.example.DataCollector;

public class MyoThalmic extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MyoThalmic.class);

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			MyoThalmic myo = (MyoThalmic) Runtime.start("myo", "MyoThalmic");
			myo.test();
			
			Hub hub = new Hub("com.example.hello-myo");

			System.out.println("Attempting to find a Myo...");
			log.info("Attempting to find a Myo");
			
			Myo myodevice = hub.waitForMyo(10000);

			if (myodevice == null) {
				throw new RuntimeException("Unable to find a Myo!");
			}

			System.out.println("Connected to a Myo armband!");
			log.info("Connected to a Myo armband");
			DeviceListener dataCollector = new DataCollector();
			hub.addListener(dataCollector);

			while (true) {
				hub.run(1000 / 20);
				System.out.print(dataCollector);

			Runtime.start("gui", "GUIService");

		}} catch (Exception e) {
			Logging.logError(e);
		}
	}
	
	public void connect() {
		
		Hub hub = new Hub("com.example.hello-myo");

		System.out.println("Attempting to find a Myo...");
		log.info("Attempting to find a Myo");
		
		Myo myodevice = hub.waitForMyo(10000);

		if (myodevice == null) {
			//throw new RuntimeException("Unable to find a Myo!");
			log.info("Unable to find a Myo");
		}

		System.out.println("Connected to a Myo armband!");
		log.info("Connected to a Myo armband");
		DeviceListener dataCollector = new DataCollector();
		hub.addListener(dataCollector);
		
	}

	public MyoThalmic(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "general" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}
}
