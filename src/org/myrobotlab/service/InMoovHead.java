package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class InMoovHead extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(InMoovHead.class);

	transient public Servo jaw;
	transient public Servo eyeX;
	transient public Servo eyeY;
	transient public Servo rothead;
	transient public Servo neck;
	transient public Arduino arduino;

	// static in Java are not overloaded but overwritten - there is no
	// polymorphism for statics
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		peers.put("jaw", "Servo", "Jaw servo");
		peers.put("eyeX", "Servo", "Eyes pan servo");
		peers.put("eyeY", "Servo", "Eyes tilt servo");
		peers.put("rothead", "Servo", "Head pan servo");
		peers.put("neck", "Servo", "Head tilt servo");
		peers.put("arduino", "Arduino", "Arduino controller for this arm");

		return peers;
	}

	public InMoovHead(String n) {
		super(n);
		jaw = (Servo) createPeer("jaw");
		eyeX = (Servo) createPeer("eyeX");
		eyeY = (Servo) createPeer("eyeY");
		rothead = (Servo) createPeer("rothead");
		neck = (Servo) createPeer("neck");
		arduino = (Arduino) createPeer("arduino");

		// connection details
		neck.setPin(12);
		rothead.setPin(13);
		jaw.setPin(26);
		eyeX.setPin(22);
		eyeY.setPin(24);

		neck.setController(arduino);
		rothead.setController(arduino);
		jaw.setController(arduino);
		eyeX.setController(arduino);
		eyeY.setController(arduino);

		neck.setMinMax(20, 160);
		rothead.setMinMax(30, 150);
		// reset by mouth control
		jaw.setMinMax(10, 25);
		eyeX.setMinMax(60, 100);
		eyeY.setMinMax(50, 100);

		neck.setRest(90);
		rothead.setRest(90);
		jaw.setRest(10);
		eyeX.setRest(80);
		eyeY.setRest(90);

	}

	/**
	 * attach all the servos - this must be re-entrant and accomplish the
	 * re-attachment when servos are detached
	 * 
	 * @return
	 */
	public boolean attach() {
		sleep(InMoov.attachPauseMs);
		eyeX.attach();
		sleep(InMoov.attachPauseMs);
		eyeY.attach();
		sleep(InMoov.attachPauseMs);
		jaw.attach();
		sleep(InMoov.attachPauseMs);
		rothead.attach();
		sleep(InMoov.attachPauseMs);
		neck.attach();

		return true;
	}

	// FIXME - should be broadcastServoState
	@Override
	public void broadcastState() {
		// notify the gui
		rothead.broadcastState();
		neck.broadcastState();
		eyeX.broadcastState();
		eyeY.broadcastState();
		jaw.broadcastState();
	}

	// FIXME - make interface for Arduino / Servos !!!
	public boolean connect(String port) throws Exception {
		arduino.connect(port);
		
		attach();
		setSpeed(0.5f, 0.5f, 0.5f, 0.5f, 0.5f);
		rest();
		sleep(1000);
		setSpeed(1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
		broadcastState();
		return true;
	}

	public void detach() {
		sleep(InMoov.attachPauseMs);
		rothead.detach();
		sleep(InMoov.attachPauseMs);
		neck.detach();
		sleep(InMoov.attachPauseMs);
		eyeX.detach();
		sleep(InMoov.attachPauseMs);
		eyeY.detach();
		sleep(InMoov.attachPauseMs);
		jaw.detach();
	}

	@Override
	public String[] getCategories() {
		return new String[] { "robot" };
	}

	@Override
	public String getDescription() {
		return "InMoov Head Service";
	}

	public long getLastActivityTime() {

		long lastActivityTime = Math.max(rothead.getLastActivityTime(), neck.getLastActivityTime());
		lastActivityTime = Math.max(lastActivityTime, eyeX.getLastActivityTime());
		lastActivityTime = Math.max(lastActivityTime, eyeY.getLastActivityTime());
		lastActivityTime = Math.max(lastActivityTime, jaw.getLastActivityTime());
		return lastActivityTime;
	}

	public String getScript(String inMoovServiceName) {
		return String.format("%s.moveHead(%d,%d,%d,%d,%d)\n", inMoovServiceName, neck.getPos(), rothead.getPos(), eyeX.getPos(), eyeY.getPos(), jaw.getPos());
	}

	public boolean isAttached() {
		boolean attached = false;

		attached |= rothead.isAttached();
		attached |= neck.isAttached();
		attached |= eyeX.isAttached();
		attached |= eyeY.isAttached();
		attached |= jaw.isAttached();

		return attached;
	}

	public boolean isValid() {
		rothead.moveTo(rothead.getRest() + 2);
		neck.moveTo(neck.getRest() + 2);
		eyeX.moveTo(eyeX.getRest() + 2);
		eyeY.moveTo(eyeY.getRest() + 2);
		jaw.moveTo(jaw.getRest() + 2);
		return true;
	}

	public void moveTo(Integer neck, Integer rothead) {
		moveTo(neck, rothead, null, null, null);
	}

	public void moveTo(Integer neck, Integer rothead, Integer eyeX, Integer eyeY) {
		moveTo(neck, rothead, eyeX, eyeY, null);
	}

	public void moveTo(Integer neck, Integer rothead, Integer eyeX, Integer eyeY, Integer jaw) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("head.moveTo %d %d %d %d %d", neck, rothead, eyeX, eyeY, jaw));
		}
		this.rothead.moveTo(rothead);
		this.neck.moveTo(neck);
		if (eyeX != null)
			this.eyeX.moveTo(eyeX);
		if (eyeY != null)
			this.eyeY.moveTo(eyeY);
		if (jaw != null)
			this.jaw.moveTo(jaw);
	}

	public void release() {
		detach();
		rothead.releaseService();
		neck.releaseService();
		eyeX.releaseService();
		eyeY.releaseService();
		jaw.releaseService();
	}

	public void rest() {
		// initial positions
		setSpeed(1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
		rothead.rest();
		neck.rest();
		eyeX.rest();
		eyeY.rest();
		jaw.rest();
	}

	@Override
	public boolean save() {
		super.save();
		rothead.save();
		neck.save();
		eyeX.save();
		eyeY.save();
		jaw.save();
		return true;
	}

	public void setLimits(int headXMin, int headXMax, int headYMin, int headYMax, int eyeXMin, int eyeXMax, int eyeYMin, int eyeYMax, int jawMin, int jawMax) {
		rothead.setMinMax(headXMin, headXMax);
		neck.setMinMax(headYMin, headYMax);
		eyeX.setMinMax(eyeXMin, eyeXMax);
		eyeY.setMinMax(eyeYMin, eyeYMax);
		jaw.setMinMax(jawMin, jawMax);
	}

	// ----- initialization end --------
	// ----- movements begin -----------

	public void setpins(int headXPin, int headYPin, int eyeXPin, int eyeYPin, int jawPin) {
		log.info(String.format("setPins %d %d %d %d %d %d", headXPin, headYPin, eyeXPin, eyeYPin, jawPin));
		rothead.setPin(headXPin);
		neck.setPin(headYPin);
		eyeX.setPin(eyeXPin);
		eyeY.setPin(eyeYPin);
		jaw.setPin(jawPin);
	}

	public void setSpeed(Float headXSpeed, Float headYSpeed, Float eyeXSpeed, Float eyeYSpeed, Float jawSpeed) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("%s setSpeed %.2f %.2f %.2f %.2f %.2f", getName(), headXSpeed, headYSpeed, eyeXSpeed, eyeYSpeed, jawSpeed));
		}

		rothead.setSpeed(headXSpeed);
		neck.setSpeed(headYSpeed);
		eyeX.setSpeed(eyeXSpeed);
		eyeY.setSpeed(eyeYSpeed);
		jaw.setSpeed(jawSpeed);

	}

	@Override
	public void startService() {
		super.startService();
		jaw.startService();
		eyeX.startService();
		eyeY.startService();
		rothead.startService();
		neck.startService();
		arduino.startService();
	}

	/*
	 * public boolean load(){ super.load(); rothead.load(); neck.load();
	 * eyeX.load(); eyeY.load(); jaw.load(); return true; }
	 */

	@Override
	public Status test() {
		Status status = Status.info("starting %s %s test", getName(), getType());
		try {

			if (arduino == null) {
				error("arduino is null");
			}

			if (!arduino.isConnected()) {
				error("arduino not connected");
			}

			rothead.moveTo(rothead.getPos() + 2);
			neck.moveTo(neck.getPos() + 2);
			eyeX.moveTo(eyeX.getPos() + 2);
			eyeY.moveTo(eyeY.getPos() + 2);
			jaw.moveTo(jaw.getPos() + 2);

		} catch (Exception e) {
			status.addError(e);
		}

		status.addInfo("test completed");
		return status;
	}

}
