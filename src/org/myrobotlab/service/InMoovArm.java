package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class InMoovArm extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(InMoovArm.class);

	/**
	 * peer services
	 */
	transient public Servo bicep;
	transient public Servo rotate;
	transient public Servo shoulder;
	transient public Servo omoplate;
	transient public Arduino arduino;
	String side;

	// static in Java are not overloaded but overwritten - there is no
	// polymorphism for statics
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("bicep", "Servo", "Bicep servo");
		peers.put("rotate", "Servo", "Rotate servo");
		peers.put("shoulder", "Servo", "Shoulder servo");
		peers.put("omoplate", "Servo", "Omoplate servo");
		peers.put("arduino", "Arduino", "Arduino controller for this arm");
		return peers;
	}

	public InMoovArm(String n) {
		super(n);
		// createReserves(n); // Ok this might work but IT CANNOT BE IN SERVICE
		// FRAMEWORK !!!!!
		bicep = (Servo) createPeer("bicep");
		rotate = (Servo) createPeer("rotate");
		shoulder = (Servo) createPeer("shoulder");
		omoplate = (Servo) createPeer("omoplate");
		arduino = (Arduino) createPeer("arduino");

		// connection details
		bicep.setPin(8);
		rotate.setPin(9);
		shoulder.setPin(10);
		omoplate.setPin(11);

		bicep.setController(arduino);
		rotate.setController(arduino);
		shoulder.setController(arduino);
		omoplate.setController(arduino);

		bicep.setMinMax(5, 90);
		rotate.setMinMax(40, 180);
		shoulder.setMinMax(0, 180);
		omoplate.setMinMax(10, 80);

		bicep.setRest(5);
		rotate.setRest(90);
		shoulder.setRest(30);
		omoplate.setRest(10);
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
		result &= bicep.attach();
		sleep(InMoov.attachPauseMs);
		result &= rotate.attach();
		sleep(InMoov.attachPauseMs);
		result &= shoulder.attach();
		sleep(InMoov.attachPauseMs);
		result &= omoplate.attach();
		return result;
	}

	@Override
	public void broadcastState() {
		// notify the gui
		bicep.broadcastState();
		rotate.broadcastState();
		shoulder.broadcastState();
		omoplate.broadcastState();
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
		setSpeed(0.7f, 0.7f, 0.7f, 0.7f);
		rest();
		sleep(4000);
		setSpeed(1.0f, 1.0f, 1.0f, 1.0f);
		broadcastState();
		return true;
	}

	public void detach() {
		bicep.detach();
		sleep(InMoov.attachPauseMs);
		rotate.detach();
		sleep(InMoov.attachPauseMs);
		shoulder.detach();
		sleep(InMoov.attachPauseMs);
		omoplate.detach();
	}

	public Arduino getArduino() {
		return arduino;
	}

	public Servo getBicep() {
		return bicep;
	}

	/*
	 * public boolean load(){ super.load(); bicep.load(); rotate.load();
	 * shoulder.load(); omoplate.load(); return true; }
	 */
	@Override
	public String[] getCategories() {
		return new String[] { "robot" };
	}

	@Override
	public String getDescription() {
		return "the InMoov Arm Service";
	}

	public long getLastActivityTime() {
		long lastActivityTime = Math.max(bicep.getLastActivityTime(), rotate.getLastActivityTime());
		lastActivityTime = Math.max(lastActivityTime, shoulder.getLastActivityTime());
		lastActivityTime = Math.max(lastActivityTime, omoplate.getLastActivityTime());
		return lastActivityTime;
	}

	public Servo getOmoplate() {
		return omoplate;
	}

	public Servo getRotate() {
		return rotate;
	}

	public String getScript(String inMoovServiceName) {
		return String.format("%s.moveArm(\"%s\",%d,%d,%d,%d)\n", inMoovServiceName, side, bicep.getPos(), rotate.getPos(), shoulder.getPos(), omoplate.getPos());
	}

	public Servo getShoulder() {
		return shoulder;
	}

	public String getSide() {
		return side;
	}

	public boolean isAttached() {
		boolean attached = false;

		attached |= bicep.isAttached();
		attached |= rotate.isAttached();
		attached |= shoulder.isAttached();
		attached |= omoplate.isAttached();

		return attached;
	}

	public void moveTo(Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("%s moveTo %d %d %d %d", getName(), bicep, rotate, shoulder, omoplate));
		}
		this.bicep.moveTo(bicep);
		this.rotate.moveTo(rotate);
		this.shoulder.moveTo(shoulder);
		this.omoplate.moveTo(omoplate);
	}

	// FIXME - releasePeers()
	public void release() {
		detach();
		if (bicep != null) {
			bicep.releaseService();
			bicep = null;
		}
		if (rotate != null) {
			rotate.releaseService();
			rotate = null;
		}
		if (shoulder != null) {
			shoulder.releaseService();
			shoulder = null;
		}
		if (omoplate != null) {
			omoplate.releaseService();
			omoplate = null;
		}
	}

	public void rest() {

		setSpeed(1.0f, 1.0f, 1.0f, 1.0f);

		bicep.rest();
		rotate.rest();
		shoulder.rest();
		omoplate.rest();
	}

	@Override
	public boolean save() {
		super.save();
		bicep.save();
		rotate.save();
		shoulder.save();
		omoplate.save();
		return true;
	}

	public void setArduino(Arduino arduino) {
		this.arduino = arduino;
	}

	public void setBicep(Servo bicep) {
		this.bicep = bicep;
	}

	public void setLimits(int bicepMin, int bicepMax, int rotateMin, int rotateMax, int shoulderMin, int shoulderMax, int omoplateMin, int omoplateMax) {
		bicep.setMinMax(bicepMin, bicepMax);
		rotate.setMinMax(rotateMin, rotateMax);
		shoulder.setMinMax(shoulderMin, shoulderMax);
		omoplate.setMinMax(omoplateMin, omoplateMax);
	}

	public void setOmoplate(Servo omoplate) {
		this.omoplate = omoplate;
	}

	// ------------- added set pins
	public void setpins(Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {

		log.info(String.format("setPins %d %d %d %d %d %d", bicep, rotate, shoulder, omoplate));
		// createPeers();
		this.bicep.setPin(bicep);
		this.rotate.setPin(rotate);
		this.shoulder.setPin(shoulder);
		this.omoplate.setPin(omoplate);
	}

	public void setRotate(Servo rotate) {
		this.rotate = rotate;
	}

	public void setShoulder(Servo shoulder) {
		this.shoulder = shoulder;
	}

	public void setSide(String side) {
		this.side = side;
	}

	public void setSpeed(Float bicep, Float rotate, Float shoulder, Float omoplate) {
		this.bicep.setSpeed(bicep);
		this.rotate.setSpeed(rotate);
		this.shoulder.setSpeed(shoulder);
		this.omoplate.setSpeed(omoplate);
	}

	@Override
	public void startService() {
		super.startService();
		bicep.startService();
		rotate.startService();
		shoulder.startService();
		omoplate.startService();
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

			bicep.moveTo(bicep.getPos() + 2);
			rotate.moveTo(rotate.getPos() + 2);
			shoulder.moveTo(shoulder.getPos() + 2);
			omoplate.moveTo(omoplate.getPos() + 2);

			sleep(300);

		} catch (Exception e) {
			status.addError(e);
		}

		status.addInfo("test completed");
		return status;
	}
}
