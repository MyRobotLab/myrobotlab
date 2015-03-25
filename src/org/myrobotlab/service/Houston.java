package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Houston extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Houston.class.getCanonicalName());

	// create service for Houston

	transient Servo lshoulder;
	transient Servo lbicep;
	transient Servo lelbow;

	transient Servo rshoulder;
	transient Servo rbicep;
	transient Servo relbow;

	// 4 motors
	transient Motor lfmotor;
	transient Motor rfmotor;
	transient Motor lbmotor;
	transient Motor rbmotor;

	transient Sphinx ear;
	transient Speech mouth;

	transient OpenCV eye;

	// PointCloud openni;

	transient Arduino arduino;

	transient Tracking tracking;

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);
		try {

			Houston houston = new Houston("houston");
			houston.startService();
			/*
			 * GUIService gui = new GUIService("gui"); gui.startService();
			 */
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	public Houston(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "robot" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public void initialize(String boardType, String comPort) throws Exception {
		lshoulder = (Servo) Runtime.createAndStart("lshoulder", "Servo");
		lbicep = (Servo) Runtime.createAndStart("lbicep", "Servo");
		lelbow = (Servo) Runtime.createAndStart("lelbow", "Servo");

		rshoulder = (Servo) Runtime.createAndStart("rshoulder", "Servo");
		rbicep = (Servo) Runtime.createAndStart("rbicep", "Servo");
		relbow = (Servo) Runtime.createAndStart("relbow", "Servo");

		lfmotor = (Motor) Runtime.createAndStart("lfmotor", "Motor");// left
																		// front
		rfmotor = (Motor) Runtime.createAndStart("rfmotor", "Motor");// right
																		// front
		lbmotor = (Motor) Runtime.createAndStart("lbmotor", "Motor");// left
																		// back
		rbmotor = (Motor) Runtime.createAndStart("rbmotor", "Motor");// right
																		// back

		ear = (Sphinx) Runtime.createAndStart("ear", "Sphinx");// right back
		mouth = (Speech) Runtime.createAndStart("mouth", "Speech");// right back

		eye = (OpenCV) Runtime.createAndStart("eye", "OpenCV");// right back

		// openni = (PointCloud) Runtime.createAndStart("openni", "OpenNI");//
		// right
		// back

		arduino = (Arduino) Runtime.createAndStart("arduino", "Arduino");

		// set config for the services
		arduino.setBoard(boardType); // atmega168 | mega2560 | etc;
		arduino.connect(comPort);
		sleep(1); // give it a second for the serial device to get ready;

		// attach Servos & Motors to arduino;
		lshoulder.setController(arduino);
		lbicep.setController(arduino);
		lelbow.setController(arduino);
		rshoulder.setController(arduino);
		rbicep.setController(arduino);
		relbow.setController(arduino);

		lshoulder.setPin(46);
		lshoulder.setPin(47);
		lshoulder.setPin(48);
		lshoulder.setPin(50);
		lshoulder.setPin(51);
		lshoulder.setPin(52);

		arduino.motorAttach(lfmotor.getName(), 4, 30);
		arduino.motorAttach(rfmotor.getName(), 5, 31);
		arduino.motorAttach(lbmotor.getName(), 6, 32);
		arduino.motorAttach(rbmotor.getName(), 7, 33);

		// update the gui with configuration changes;
		arduino.publishState();

		lshoulder.publishState();
		lbicep.publishState();
		lelbow.publishState();
		rshoulder.publishState();
		rbicep.publishState();
		relbow.publishState();

		lfmotor.publishState();
		rfmotor.publishState();
		lbmotor.publishState();
		rbmotor.publishState();

	}

	public void systemTest() {
		int lfaencoder = 38;
		int analogSensorPin = 67;

		// system check - need to do checks to see all systems are go !
		// start the analog pin sample to display
		// in the oscope
		arduino.analogReadPollingStart(analogSensorPin);

		// change the pinMode of digital pin 13
		arduino.pinMode(lfaencoder, Arduino.OUTPUT);

		// begin tracing the digital pin 13
		arduino.digitalReadPollingStart(lfaencoder);

		// turn off the trace
		// arduino.digitalReadPollStop(lfaencoder)
		// turn off the analog sampling
		// arduino.analogReadPollingStop(analogSensorPin)

	}
}
