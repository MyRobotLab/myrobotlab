package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class InMoovHand extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(InMoovHand.class);

	/**
	 * peer services
	 */
	transient public Servo thumb;
	transient public Servo index;
	transient public Servo majeure;
	transient public Servo ringFinger;
	transient public Servo pinky;
	transient public Servo wrist;
	transient public Arduino arduino;
	private String side;

	// static in Java are not overloaded but overwritten - there is no
	// polymorphism for statics
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		peers.put("thumb", "Servo", "Thumb servo");
		peers.put("index", "Servo", "Index servo");
		peers.put("majeure", "Servo", "Majeure servo");
		peers.put("ringFinger", "Servo", "RingFinger servo");
		peers.put("pinky", "Servo", "Pinky servo");
		peers.put("wrist", "Servo", "Wrist servo");
		peers.put("arduino", "Arduino", "Arduino controller for this arm");
		// peers.put("keyboard", "Keyboard", "Keyboard control");
		// peers.put("xmpp", "XMPP", "XMPP control");
		return peers;
	}

	public InMoovHand(String n) {
		super(n);
		thumb = (Servo) createPeer("thumb");
		index = (Servo) createPeer("index");
		majeure = (Servo) createPeer("majeure");
		ringFinger = (Servo) createPeer("ringFinger");
		pinky = (Servo) createPeer("pinky");
		wrist = (Servo) createPeer("wrist");
		arduino = (Arduino) createPeer("arduino");

		thumb.setRest(2);
		index.setRest(2);
		majeure.setRest(2);
		ringFinger.setRest(2);
		pinky.setRest(2);
		wrist.setRest(90);

		// connection details
		thumb.setPin(2);
		index.setPin(3);
		majeure.setPin(4);
		ringFinger.setPin(5);
		pinky.setPin(6);
		wrist.setPin(7);

		thumb.setController(arduino);
		index.setController(arduino);
		majeure.setController(arduino);
		ringFinger.setController(arduino);
		pinky.setController(arduino);
		wrist.setController(arduino);
	}

	// FIXME make
	// .isValidToStart() !!! < check all user data !!!

	@Override
	public void startService() {
		super.startService();
		thumb.startService();
		index.startService();
		majeure.startService();
		ringFinger.startService();
		pinky.startService();
		wrist.startService();
		arduino.startService();
	}

	// FIXME FIXME - this method must be called
	// user data needed
	/**
	 * connect - user data needed
	 * 
	 * @param port
	 * @return
	 */
	public boolean connect(String port) {
		startService();

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
		setSpeed(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f);
		rest();
		sleep(2000);
		setSpeed(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
		broadcastState();
		return true;
	}

	/**
	 * attach all the servos - this must be re-entrant and accomplish the
	 * re-attachment when servos are detached
	 * 
	 * @return
	 */
	public boolean attach() {
		sleep(InMoov.attachPauseMs);
		thumb.attach();
		sleep(InMoov.attachPauseMs);
		index.attach();
		sleep(InMoov.attachPauseMs);
		majeure.attach();
		sleep(InMoov.attachPauseMs);
		ringFinger.attach();
		sleep(InMoov.attachPauseMs);
		pinky.attach();
		sleep(InMoov.attachPauseMs);
		wrist.attach();
		return true;
	}

	@Override
	public String getDescription() {
		return "hand service for inmoov";
	}

	// TODO - waving thread fun
	public void moveTo(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky) {
		moveTo(thumb, index, majeure, ringFinger, pinky, null);
	}

	public void moveTo(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("%s.moveTo %d %d %d %d %d %d", getName(), thumb, index, majeure, ringFinger, pinky, wrist));
		}
		this.thumb.moveTo(thumb);
		this.index.moveTo(index);
		this.majeure.moveTo(majeure);
		this.ringFinger.moveTo(ringFinger);
		this.pinky.moveTo(pinky);
		if (wrist != null)
			this.wrist.moveTo(wrist);
	}

	public void rest() {
		// initial positions
		setSpeed(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);

		thumb.rest();
		index.rest();
		majeure.rest();
		ringFinger.rest();
		pinky.rest();
		wrist.rest();
	}

	public void broadcastState() {
		// notify the gui
		thumb.broadcastState();
		index.broadcastState();
		majeure.broadcastState();
		ringFinger.broadcastState();
		pinky.broadcastState();
		wrist.broadcastState();
	}

	public void detach() {
		thumb.detach();
		sleep(InMoov.attachPauseMs);
		index.detach();
		sleep(InMoov.attachPauseMs);
		majeure.detach();
		sleep(InMoov.attachPauseMs);
		ringFinger.detach();
		sleep(InMoov.attachPauseMs);
		pinky.detach();
		sleep(InMoov.attachPauseMs);
		wrist.detach();
	}

	public void release() {
		detach();
		thumb.releaseService();
		index.releaseService();
		majeure.releaseService();
		ringFinger.releaseService();
		pinky.releaseService();
		wrist.releaseService();
	}

	public void setSpeed(Float thumb, Float index, Float majeure, Float ringFinger, Float pinky, Float wrist) {
		this.thumb.setSpeed(thumb);
		this.index.setSpeed(index);
		this.majeure.setSpeed(majeure);
		this.ringFinger.setSpeed(ringFinger);
		this.pinky.setSpeed(pinky);
		this.wrist.setSpeed(wrist);
	}

	public void victory() {
		moveTo(150, 0, 0, 180, 180, 90);
	}

	public void devilHorns() {
		moveTo(150, 0, 180, 180, 0, 90);
	}

	public void hangTen() {
		moveTo(0, 180, 180, 180, 0, 90);
	}

	public void bird() {
		moveTo(150, 180, 0, 180, 180, 90);
	}

	public void thumbsUp() {
		moveTo(0, 180, 180, 180, 180, 90);
	}

	public void ok() {
		moveTo(150, 180, 0, 0, 0, 90);
	}

	public void one() {
		moveTo(150, 0, 180, 180, 180, 90);
	}

	public void two() {
		victory();
	}

	public void three() {
		moveTo(150, 0, 0, 0, 180, 90);
	}

	public void four() {
		moveTo(150, 0, 0, 0, 0, 90);
	}

	public void five() {
		open();
	}

	public void count() {
		one();
		sleep(1);
		two();
		sleep(1);
		three();
		sleep(1);
		four();
		sleep(1);
		five();
	}

	public String getScript(String inMoovServiceName) {
		return String.format("%s.moveHand(\"%s\",%d,%d,%d,%d,%d,%d)\n", inMoovServiceName, side, thumb.getPositionInt(), index.getPositionInt(), majeure.getPositionInt(),
				ringFinger.getPositionInt(), pinky.getPositionInt(), wrist.getPositionInt());
	}

	public void setPins(int thumb, int index, int majeure, int ringFinger, int pinky, int wrist) {
		log.info(String.format("setPins %d %d %d %d %d %d", thumb, index, majeure, ringFinger, pinky, wrist));
		this.thumb.setPin(thumb);
		this.index.setPin(index);
		this.majeure.setPin(majeure);
		this.ringFinger.setPin(ringFinger);
		this.pinky.setPin(pinky);
		this.wrist.setPin(wrist);
	}

	// ----- initialization end --------
	// ----- movements begin -----------

	public void close() {
		moveTo(130, 180, 180, 180, 180);
	}

	public void open() {
		rest();
	}

	public void openPinch() {
		moveTo(0, 0, 180, 180, 180);
	}

	public void closePinch() {
		moveTo(130, 140, 180, 180, 180);
	}

	public void test() {
		try {
			if (arduino == null) {
				Status.throwError("arduino is null");
			}

			if (!arduino.isConnected()) {
				Status.throwError("arduino not connected");
			}

			thumb.moveTo(thumb.getPosition() + 2);
			index.moveTo(index.getPosition() + 2);
			majeure.moveTo(majeure.getPosition() + 2);
			ringFinger.moveTo(ringFinger.getPosition() + 2);
			pinky.moveTo(pinky.getPosition() + 2);
			wrist.moveTo(wrist.getPosition() + 2);

		} catch (Exception e) {
			error(e);
		}

		info("test completed");
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		InMoovHand rightHand = new InMoovHand("r01");
		Runtime.createAndStart("gui", "GUIService");
		rightHand.connect("COM12");
		rightHand.startService();
		Runtime.createAndStart("webgui", "WebGUI");
		// rightHand.connect("COM12"); TEST RECOVERY !!!

		rightHand.close();
		rightHand.open();
		rightHand.openPinch();
		rightHand.closePinch();
		rightHand.rest();

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 */
	}

	public void setSide(String side) {
		this.side = side;
	}

	public String getSide() {
		return side;
	}

	public long getLastActivityTime() {
		long minLastActivity = 0;
		
		minLastActivity = (minLastActivity < thumb.getLastActivityTime())?thumb.getLastActivityTime():minLastActivity;
		minLastActivity = (minLastActivity < index.getLastActivityTime())?index.getLastActivityTime():minLastActivity;
		minLastActivity = (minLastActivity < majeure.getLastActivityTime())?majeure.getLastActivityTime():minLastActivity;
		minLastActivity = (minLastActivity < ringFinger.getLastActivityTime())?ringFinger.getLastActivityTime():minLastActivity;
		minLastActivity = (minLastActivity < pinky.getLastActivityTime())?pinky.getLastActivityTime():minLastActivity;
		minLastActivity = (minLastActivity < wrist.getLastActivityTime())?wrist.getLastActivityTime():minLastActivity;
		
		return minLastActivity;

	}

	public boolean isAttached() {
		boolean attached = false;
		attached |= thumb.isAttached();
		attached |= index.isAttached();
		attached |= majeure.isAttached();
		attached |= ringFinger.isAttached();
		attached |= pinky.isAttached();
		attached |= wrist.isAttached();
		return attached;
	}
	
	public void setRest(int thumb, int index, int majeure, int ringFinger, int pinky){
		setRest(thumb, index, majeure, ringFinger, pinky, null);
	}

	public void setRest(int thumb, int index, int majeure, int ringFinger, int pinky, Integer wrist) {
		log.info(String.format("setRest %d %d %d %d %d %d", thumb, index, majeure, ringFinger, pinky, wrist));
		this.thumb.setRest(thumb);
		this.index.setRest(index);
		this.majeure.setRest(majeure);
		this.ringFinger.setRest(ringFinger);
		this.pinky.setRest(pinky);
		if (wrist != null){
			this.wrist.setRest(wrist);
		}
	}
	
	public void map(int minX, int maxX, int minY, int maxY){
		thumb.map(minX, maxX, minY, maxY);
		index.map(minX, maxX, minY, maxY);
		majeure.map(minX, maxX, minY, maxY);
		ringFinger.map(minX, maxX, minY, maxY);
		pinky.map(minX, maxX, minY, maxY);
	}

}
