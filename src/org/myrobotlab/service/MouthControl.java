package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class MouthControl extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MouthControl.class.getCanonicalName());

	public int moutClosedPos = 20;
	public int mouthOpenedPos = 4;
	public int delaytime = 100;
	public int delaytimestop = 200;
	public int delaytimeletter = 60;

	transient public Servo jaw;
	transient public Arduino arduino;
	transient public Speech mouth;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("jaw", "Servo", "shared Jaw servo instance");
		peers.put("arduino", "Arduino", "shared Arduino instance");
		peers.put("mouth", "Speech", "shared Speech instance");

		return peers;
	}

	public MouthControl(String n) {
		super(n);
		jaw = (Servo) createPeer("jaw");
		arduino = (Arduino) createPeer("arduino");
		mouth = (Speech) createPeer("mouth");

		jaw.setPin(7);
		jaw.setController(arduino);
		mouth.addListener(getName(), "saying");
	}

	public void setdelays(Integer d1, Integer d2, Integer d3) {
		delaytime = d1;
		delaytimestop = d2;
		delaytimeletter = d3;
	}

	public void setmouth(Integer closed, Integer opened) {
		//jaw.setMinMax(closed, opened);
		moutClosedPos = closed;
		mouthOpenedPos = opened;
		
		if (closed < opened){
			jaw.setMinMax(closed, opened);
		} else {
			jaw.setMinMax(opened, closed);
		}
	}

	public void startService() {
		super.startService();
		jaw.startService();
		arduino.startService();
		mouth.startService();
	}

	// FIXME make interface
	public boolean connect(String port) {
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

		//arduino.servoAttach(jaw);
		jaw.attach();
		return true;
	}

	public synchronized void saying(String text) {
		log.info("move moving to :" + text);
		if (jaw != null) { // mouthServo.moveTo(Mouthopen);
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
						jaw.moveTo(moutClosedPos);// #// close the servo
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
	}

	@Override
	public String getDescription() {
		return "mouth movements based on spoken text";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		// LoggingFactory.getInstance().setLevel(Level.INFO);
		MouthControl MouthControl = new MouthControl("MouthControl");
		MouthControl.startService();

		Runtime.createAndStart("gui", "GUIService");

	}

}
