package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.util.Map;
import org.slf4j.Logger;

public class EddieControlBoard extends Service {

	private static final long serialVersionUID = 1L;
	private transient Serial serial;
	private transient Keyboard keyboard;

	Map mapper = new Map(-1.0f, 1.0f, -127.0f, 127.0f);
	float leftMotorPower = 0.0f;
	float rightMotorPower = 0.0f;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("serial", "Serial", "serial");
		peers.put("keyboard", "Keyboard", "serial");
		return peers;
	}

	public final static Logger log = LoggerFactory.getLogger(EddieControlBoard.class);

	public EddieControlBoard(String n) {
		super(n);
	}

	public void startService() {
		super.startService();
		if (serial == null) {
			serial = (Serial) startPeer("serial");
		}
		if (keyboard == null) {
			keyboard = (Keyboard) startPeer("keyboard");
		}

		keyboard.addKeyListener(this);
	}
	
	public boolean connect(String port) throws IOException {
		boolean ret = serial.connect(port, 115200, 8, 1, 0);
		if (ret){
			stop();
		}
		return ret;
	}

	public void stop() throws IOException {
		go(0.0f, 0.0f);
	}

	// read commands begin ---
	public String getHwVersion() throws InterruptedException, IOException {
		return sendCommand("HWVER");
	}

	public String getVersion() throws InterruptedException, IOException {
		return sendCommand("VER");
	}
	
	public String getPingValues() throws InterruptedException, IOException {
		return sendCommand("PING");
	}

	public String getAnalogValues() throws InterruptedException, IOException {
		return sendCommand("ADC");
	}
	
	public String getGPIOInputs() throws InterruptedException, IOException {
		return sendCommand("INS");
	}

	public String getGPIOOutputs() throws InterruptedException, IOException {
		return sendCommand("OUTS");
	}
	
	public String getGPIOLowValues() throws InterruptedException, IOException {
		return sendCommand("LOWS");
	}

	public String getGPIOHighValues() throws InterruptedException, IOException {
		return sendCommand("HIGHS");
	}
	
	public String read() throws InterruptedException, IOException {
		return sendCommand("READ");
	}
	// read commands end ---


	public void onKey(String cmd) throws IOException {
		switch (cmd) {

		// left begin ---
		case "NumPad-7": {
			leftMotorPower += 0.01;
			if (leftMotorPower > 1.0) {
				leftMotorPower = 1.0f;
			}
			go(leftMotorPower, rightMotorPower);
			break;
		}
		case "NumPad-4": {
			leftMotorPower -= 0.01;
			if (leftMotorPower < -1.0) {
				leftMotorPower = -1.0f;
			}
			rightMotorPower += 0.01;
			if (rightMotorPower > 1.0) {
				rightMotorPower = 1.0f;
			}
			go(leftMotorPower, rightMotorPower);
			break;
		}
		case "NumPad-1": {
			leftMotorPower -= 0.01;
			if (leftMotorPower < -1.0) {
				leftMotorPower = -1.0f;
			}
			go(leftMotorPower, rightMotorPower);
			break;
		}
		// left end ---

		// right begin --
		case "NumPad-9": {
			rightMotorPower += 0.01;
			if (rightMotorPower > 1.0) {
				rightMotorPower = 1.0f;
			}
			go(leftMotorPower, rightMotorPower);
			break;
		}
		case "NumPad-6": {
			rightMotorPower -= 0.01;
			if (rightMotorPower < -1.0) {
				rightMotorPower = -1.0f;
			}
			leftMotorPower += 0.01;
			if (leftMotorPower > 1.0) {
				leftMotorPower = 1.0f;
			}
			go(leftMotorPower, rightMotorPower);
			break;
		}
		case "NumPad-3": {
			rightMotorPower -= 0.01;
			if (rightMotorPower < -1.0) {
				rightMotorPower = -1.0f;
			}
			go(leftMotorPower, rightMotorPower);
			break;
		}
		// right end --

		// center
		case "NumPad-8": {
			leftMotorPower += 0.01;
			if (leftMotorPower > 1.0) {
				leftMotorPower = 1.0f;
			}
			rightMotorPower += 0.01;
			if (rightMotorPower > 1.0) {
				rightMotorPower = 1.0f;
			}
			go(leftMotorPower, rightMotorPower);
			break;
		}

		case "NumPad-2": {
			leftMotorPower -= 0.01;
			if (leftMotorPower > 1.0) {
				leftMotorPower = 1.0f;
			}
			rightMotorPower -= 0.01;
			if (rightMotorPower > 1.0) {
				rightMotorPower = 1.0f;
			}
			go(rightMotorPower, rightMotorPower);
			break;
		}

		// stop all
		case "NumPad-5":
		case "Space": {
			leftMotorPower = 0.0f;
			rightMotorPower = 0.0f;
			go(rightMotorPower, rightMotorPower);
			break;
		}

		default: {
			warn("key command - [%s] - not defined", cmd);
			break;
		}

		}
	}

	/**
	 * sending a command when expecting a string response in the context of
	 * blocking for response
	 * 
	 * @param cmd
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public String sendCommand(String cmd) throws InterruptedException, IOException {
		log.info(String.format("sendCommand %s", cmd));
		String ret = null;

		serial.setBlocking(true);
		serial.write(String.format("%s\r", cmd));
		ret = serial.readString();
		serial.setBlocking(false);

		return ret;
	}

	public void go(float left, float right) throws IOException {
		log.info(String.format("go %f %f", left, right));
		int l = mapper.calc(left);
		if (l > 127) {
			l = 128 - l;
		}
		int r = mapper.calc(right);
		if (r > 127) {
			r = 128 - r;
		}
		String cmd = String.format("GO %s %s\r", Integer.toHexString(l & 0xFF), Integer.toHexString(r & 0xFF)).toUpperCase();
		info("%s", cmd);
		serial.write(cmd);
	}

	public void setMotorSpeed(float left, float right) {
		// The left and right speeds have units of positions per second and are
		// entered as
		// signed (two's complement) 16-bit hex values. The range of allowed
		// values is
		// from 8000 to 7FFF.
		int myleft = (int) (left * 1000);
		int myright = (int) (right * 1000);
		String l = Integer.toHexString(myleft);
		String r = Integer.toHexString(myright);
		// Long.parseLong("ffff8000", 16);
		// serial.write(String.format("%s\r",cmd));
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public Status test() {
		Status status = super.test();
		try {
			EddieControlBoard ecb = (EddieControlBoard) Runtime.start(getName(), "EddieControlBoard");
			Runtime.start("gui", "GUIService");
			ecb.serial.connectToVirtualUART();
			// ecb.connect(port)
			// ecb.go(1, 1);
		} catch (Exception e) {
			Logging.logException(e);
		}

		return status;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			EddieControlBoard ecb = (EddieControlBoard) Runtime.start("ecb", "EddieControlBoard");
			ecb.test();

			// 129 -> 81
			// 128 -> 80 (full reverse)
			// 127 -> 7F (full forward)
			// 255 -> (little reverse)

			// 81 FF 0 1 7F
			// 128 --- 255 0 1 --- 127
			/*
			 * float i = 0.94f;
			 * 
			 * Map mapper = new Map(-1.0f, 1.0f, -127.0f, 127.0f); int x =
			 * mapper.calc(i); if (x > 127) { x = 128 - x; }
			 * 
			 * log.info("{}", Integer.toHexString(x & 0xFF));
			 * 
			 * String hex = Integer.toHexString(256 & 0xFF);
			 * log.info(hex.toUpperCase()); hex = Integer.toHexString(255 &
			 * 0xFF); log.info(hex.toUpperCase()); hex = Integer.toHexString(230
			 * & 0xFF); // slow reverse log.info(hex.toUpperCase());
			 */

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
