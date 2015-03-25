package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class InMoovTorso extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(InMoovTorso.class);

	transient public Servo topStom;
	transient public Servo midStom;
	transient public Servo lowStom;
	transient public Arduino arduino;

	// static in Java are not overloaded but overwritten - there is no
	// polymorphism for statics
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("topStom", "Servo", "Top Stomach servo");
		peers.put("midStom", "Servo", "Mid Stomach servo");
		peers.put("lowStom", "Servo", "Low Stomach servo");
		peers.put("arduino", "Arduino", "Arduino controller for this arm");
		return peers;
	}

	static public void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		try {
			InMoovTorso torso = (InMoovTorso) Runtime.createAndStart("torso", "InMoovTorso");
			torso.connect("COM4");
			torso.test();
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public InMoovTorso(String n) {
		super(n);
		// createReserves(n); // Ok this might work but IT CANNOT BE IN SERVICE
		// FRAMEWORK !!!!!
		topStom = (Servo) createPeer("topStom");
		midStom = (Servo) createPeer("midStom");
		lowStom = (Servo) createPeer("lowStom");
		arduino = (Arduino) createPeer("arduino");

		// connection details
		topStom.setPin(27);
		midStom.setPin(28);
		lowStom.setPin(29);

		topStom.setController(arduino);
		midStom.setController(arduino);
		lowStom.setController(arduino);

		topStom.setMinMax(60, 120);
		midStom.setMinMax(0, 180);
		lowStom.setMinMax(0, 180);

		topStom.setRest(90);
		midStom.setRest(90);
		lowStom.setRest(90);
	}

	/**
	 * attach all the servos - this must be re-entrant and accomplish the
	 * re-attachment when servos are detached
	 * 
	 * @return
	 */
	public boolean attach() {
		boolean result = true;
		sleep(InMoov.attachPauseMs);
		result &= topStom.attach();
		sleep(InMoov.attachPauseMs);
		result &= midStom.attach();
		sleep(InMoov.attachPauseMs);
		result &= lowStom.attach();
		sleep(InMoov.attachPauseMs);
		return result;
	}

	@Override
	public void broadcastState() {
		// notify the gui
		topStom.broadcastState();
		midStom.broadcastState();
		lowStom.broadcastState();
	}

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

		attach();
		setSpeed(0.7f, 0.7f, 0.7f);
		rest();
		sleep(4000);
		setSpeed(1.0f, 1.0f, 1.0f);
		broadcastState();
		return true;
	}

	public void detach() {
		topStom.detach();
		sleep(InMoov.attachPauseMs);
		midStom.detach();
		sleep(InMoov.attachPauseMs);
		lowStom.detach();
		sleep(InMoov.attachPauseMs);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "robot" };
	}

	@Override
	public String getDescription() {
		return "the InMoov Arm Service";
	}

	public long getLastActivityTime() {
		long minLastActivity = Math.max(topStom.getLastActivityTime(), midStom.getLastActivityTime());
		minLastActivity = Math.max(minLastActivity, lowStom.getLastActivityTime());
		return minLastActivity;
	}

	public String getScript(String inMoovServiceName) {
		return String.format("%s.moveTorso(%d,%d,%d)\n", inMoovServiceName, topStom.getPos(), midStom.getPos(), lowStom.getPos());
	}

	public boolean isAttached() {
		boolean attached = false;

		attached |= topStom.isAttached();
		attached |= midStom.isAttached();
		attached |= lowStom.isAttached();

		return attached;
	}

	public void moveTo(Integer topStom, Integer midStom, Integer lowStom) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("%s moveTo %d %d %d", getName(), topStom, midStom, lowStom));
		}
		this.topStom.moveTo(topStom);
		this.midStom.moveTo(midStom);
		this.lowStom.moveTo(lowStom);

	}

	// FIXME - releasePeers()
	public void release() {
		detach();
		if (topStom != null) {
			topStom.releaseService();
			topStom = null;
		}
		if (midStom != null) {
			midStom.releaseService();
			midStom = null;
		}
		if (lowStom != null) {
			lowStom.releaseService();
			lowStom = null;
		}
	}

	public void rest() {

		setSpeed(1.0f, 1.0f, 1.0f);

		topStom.rest();
		midStom.rest();
		lowStom.rest();
	}

	@Override
	public boolean save() {
		super.save();
		topStom.save();
		midStom.save();
		lowStom.save();
		return true;
	}

	public void setLimits(int bicepMin, int bicepMax, int rotateMin, int rotateMax, int shoulderMin, int shoulderMax) {
		topStom.setMinMax(bicepMin, bicepMax);
		midStom.setMinMax(rotateMin, rotateMax);
		lowStom.setMinMax(shoulderMin, shoulderMax);
	}

	// ------------- added set pins
	public void setpins(Integer topStom, Integer midStom, Integer lowStom) {
		// createPeers();
		this.topStom.setPin(topStom);
		this.midStom.setPin(midStom);
		this.lowStom.setPin(lowStom);
	}

	public void setSpeed(Float topStom, Float midStom, Float lowStom) {
		this.topStom.setSpeed(topStom);
		this.midStom.setSpeed(midStom);
		this.lowStom.setSpeed(lowStom);
	}

	/*
	 * public boolean load() { super.load(); topStom.load(); midStom.load();
	 * lowStom.load(); return true; }
	 */

	@Override
	public void startService() {
		super.startService();
		topStom.startService();
		midStom.startService();
		lowStom.startService();
		arduino.startService();
	}

	@Override
	public Status test() {
		Status status = Status.info("starting %s %s test", getName(), getType());
		try {
			if (arduino == null) {
				throw new Exception("arduino is null");
			}

			if (!arduino.isConnected()) {
				throw new Exception("arduino not connected");
			}

			topStom.moveTo(topStom.getPos() + 2);
			midStom.moveTo(midStom.getPos() + 2);
			lowStom.moveTo(lowStom.getPos() + 2);

			moveTo(35, 45, 55);
			String move = getScript("i01");
			log.info(move);

		} catch (Exception e) {
			status.addError(e);
		}

		status.addInfo("test completed");
		return status;
	}
}
