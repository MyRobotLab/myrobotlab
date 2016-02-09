package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.repo.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

/**
 * 
 * MouthControl - This service will animate a jaw servo to move as its speaking
 * It's peers are the jaw servo, speech service and an arduino.
 *
 */
public class MouthControl extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MouthControl.class.getCanonicalName());

	public int mouthClosedPos = 20;
	public int mouthOpenedPos = 4;
	public int delaytime = 100;
	public int delaytimestop = 200;
	public int delaytimeletter = 60;

	transient Servo jaw;
	transient Arduino arduino;
	transient SpeechSynthesis mouth;

	public boolean autoAttach = true;
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		try {
			// LoggingFactory.getInstance().setLevel(Level.INFO);
			MouthControl MouthControl = new MouthControl("MouthControl");
			MouthControl.startService();

			Runtime.createAndStart("gui", "GUIService");

			MouthControl.autoAttach = true;
			MouthControl.onSaying("test on");
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public MouthControl(String n) {
		super(n);
		jaw = (Servo) createPeer("jaw");
		arduino = (Arduino) createPeer("arduino");
		mouth = (SpeechSynthesis) createPeer("mouth");

		jaw.setPin(7);
		jaw.setController(arduino);
		subscribe(mouth.getName(), "saying");
	}

	// FIXME make interface
	public boolean connect(String port) throws Exception {
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

		// arduino.servoAttach(jaw);
		jaw.attach();
		return true;
	}

	public Arduino getArduino() {
		return arduino;
	}
	public Servo getJaw() {
		return jaw;
	}

	public void setJaw(Servo jaw) {
		this.jaw = jaw;
	}

	public SpeechSynthesis getMouth() {
		return mouth;
	}

	public void setMouth(SpeechSynthesis mouth) {
		this.mouth = mouth;
	}

	public void setArduino(Arduino arduino) {
		this.arduino = arduino;
	}

	public String[] getCategories() {
		return new String[] { "control" };
	}

	@Override
	public String getDescription() {
		return "mouth movements based on spoken text";
	}

	public synchronized void onSaying(String text) {
		log.info("move moving to :" + text);
		if (jaw != null) { // mouthServo.moveTo(Mouthopen);
			if (autoAttach) {
				if (!jaw.isAttached()) {
					// attach the jaw if it's not attached.
					jaw.attach();
				}
			}
			
			boolean ison = false;
			String testword;
			String[] a = text.split(" ");
			for (int w = 0; w < a.length; w++) {
				// String word = ;
				// log.info(String.valueOf(a[w].length()));

				if (a[w].endsWith("es")) {
					testword = a[w].substring(0, a[w].length() - 2);

				} else if (a[w].endsWith("e")) {
					testword = a[w].substring(0, a[w].length() - 1);
					// log.info("e gone");
				} else {
					testword = a[w];

				}

				char[] c = testword.toCharArray();

				for (int x = 0; x < c.length; x++) {
					char s = c[x];
					if ((s == 'a' || s == 'e' || s == 'i' || s == 'o' || s == 'u' || s == 'y') && !ison) {
						jaw.moveTo(mouthOpenedPos); // # move the servo to the
						// open spot
						ison = true;
						sleep(delaytime);
						jaw.moveTo(mouthClosedPos);// #// close the servo
					} else if (s == '.') {
						ison = false;
						sleep(delaytimestop);
					} else {
						ison = false;
						sleep(delaytimeletter); // # sleep half a second
					}

				}

				sleep(80);
			}

		} else {
			log.info("need to attach first");
		}
		
		//  We're done annimating, lets detach the jaw while not in use.
		if (autoAttach && jaw != null) {
			if (jaw.isAttached()) {
				// attach the jaw if it's not attached.
				jaw.detach();
			}
		}
	}

	public void setdelays(Integer d1, Integer d2, Integer d3) {
		delaytime = d1;
		delaytimestop = d2;
		delaytimeletter = d3;
	}

	public void setmouth(Integer closed, Integer opened) {
		// jaw.setMinMax(closed, opened);
		mouthClosedPos = closed;
		mouthOpenedPos = opened;

		if (closed < opened) {
			jaw.setMinMax(closed, opened);
		} else {
			jaw.setMinMax(opened, closed);
		}
	}

	@Override
	public void startService() {
		super.startService();
		jaw.startService();
		arduino.startService();
		// mouth.startService();
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

		ServiceType meta = new ServiceType(MouthControl.class.getCanonicalName());
		meta.addDescription("Mouth movements based on spoken text");
		meta.addCategory("control");
		
		meta.addPeer("jaw", "Servo", "shared Jaw servo instance");
		meta.addPeer("arduino", "Arduino", "shared Arduino instance");
		meta.addPeer("mouth", "AcapelaSpeech", "shared Speech instance");

		return meta;
	}


}
