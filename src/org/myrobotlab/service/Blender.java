package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class Blender extends Service {

	/**
	 * Control line - JSON over TCP/IP This is the single control communication
	 * line over which virtual objects are created - and linked with Serial
	 * connections
	 * 
	 * Typically, a virtual object is created and if it has a serial line (like
	 * an Arduino) a new TCP/IP connection is created which sends and receives
	 * the binary serial data
	 * 
	 * @author GroG
	 *
	 */
	public class ControlHandler extends Thread {
		Socket socket;
		DataInputStream dis;
		boolean listening = false;

		public ControlHandler(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			BufferedReader in = null;
			try {
				listening = true;
				// in = socket.getInputStream();
				dis = new DataInputStream(socket.getInputStream());
				in = new BufferedReader(new InputStreamReader(dis));
				while (listening) {
					// handle inbound control messages

					// JSONObject json = new JSONObject(in.readLine());
					String json = in.readLine();
					log.info(String.format("%s", json));
					Message msg = Encoder.fromJson(json, Message.class);
					log.info(String.format("msg %s", msg));
					invoke(msg);

				}
			} catch (Exception e) {
				Logging.logError(e);
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (Exception e) {
				}
			}
		}
	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Blender.class);

	public static final String SUCCESS = "SUCCESS";
	Socket control = null;
	transient ControlHandler controlHandler = null;
	String host = "localhost";
	Integer controlPort = 8989;

	Integer serialPort = 9191;
	String blenderVersion;

	/*
	 * transient HashMap<String, VirtualPort> virtualPorts = new HashMap<String,
	 * VirtualPort>();
	 * 
	 * public class VirtualPort { public Serial serial; public
	 * VirtualNullModemCable cable; }
	 */

	// Socket serial = null; NO

	String expectedBlenderVersion = "0.9";

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			Blender blender = (Blender) Runtime.start("blender", "Blender");
			blender.test();

			// Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public Blender(String n) {
		super(n);
	}

	public synchronized void attach(Arduino service) {
		// let Blender know we are going
		// to virtualize an Arduino
		sendMsg("attach", service.getName(), service.getSimpleName());
	}

	public boolean connect() {
		try {
			if (control != null && control.isConnected()) {
				info("already connected");
				return true;
			}

			info("connecting to Blender.py %s %d", host, controlPort);
			control = new Socket(host, controlPort);
			controlHandler = new ControlHandler(control);
			controlHandler.start();

			info("connected - goodtimes");
			return true;
		} catch (Exception e) {
			error(e);
		}
		return false;
	}

	public boolean connect(String host, Integer port) {
		this.host = host;
		this.controlPort = port;
		return connect();
	}

	public boolean disconnect() {
		try {
			if (control != null) {
				control.close();
			}
			if (controlHandler != null) {
				controlHandler.listening = false;
				controlHandler = null;
			}
			// TODO - run through all serial connections
			return true;
		} catch (Exception e) {
			error(e);
		}
		return false;
	}

	@Override
	public String[] getCategories() {
		return new String[] { "display", "simulator" };
	}

	@Override
	public String getDescription() {
		return "used as a general blender";
	}

	// -------- publish api end --------

	// -------- Blender.py --> callback api begin --------
	public void getVersion() {
		sendMsg("getVersion");
	}

	boolean isConnected() {
		return (control != null) && control.isConnected();
	}

	// call back from blender
	public String onAttach(String name) {
		try {
			info("onAttach - Blender is ready to attach serial device %s", name);
			// FIXME - more general case determined by "Type"
			ServiceInterface si = Runtime.getService(name);
			if ("org.myrobotlab.service.Arduino".equals(si.getType())) {
				// FIXME - make more general - "any" Serial device !!!
				Arduino arduino = (Arduino) Runtime.getService(name);
				if (arduino != null) {

					// get handle to serial service of the Arduino
					Serial serial = arduino.getSerial();

					// connecting over tcp ip
					serial.connectTCP(host, serialPort);

					// int vpn = virtualPorts.size();

					/*
					 * VIRTUAL PORT IS NOT NEEDED !!! - JUST A SERIAL OVER
					 * TCP/IP YAY !!!! :) VirtualPort vp = new VirtualPort();
					 * vp.serial = (Serial)
					 * Runtime.start(String.format("%s.UART.%d"
					 * ,arduino.getName(), vpn), "Serial"); vp.cable =
					 * Serial.createNullModemCable(String.format("MRL.%d", vpn),
					 * String.format("BLND.%d", vpn));
					 * virtualPorts.put(arduino.getName(), vp);
					 * vp.serial.connect(String.format("BLND.%d", vpn));
					 */
					// vp.serial.addRelay(host, serialPort);
					// arduino.connect(String.format("MRL.%d", vpn));
					// add the tcp relay pipes

				} else {
					error("onAttach %s not found", name);
				}
			}

		} catch (Exception e) {
			Logging.logError(e);
		}
		return name;
	}

	public String onBlenderVersion(String version) {
		blenderVersion = version;
		if (blenderVersion.equals(expectedBlenderVersion)) {
			info("Blender.py version is %s goodtimes", version);
		} else {
			error("Blender.py version is %s goodtimes", version);
		}

		return version;
	}

	public String onGetVersion(String version) {
		info("blender returned %s", version);
		return version;
	}

	// -------- Blender.py --> callback api end --------

	public void publishConnect() {
	}

	// -------- publish api begin --------
	public void publishDisconnect() {
	}

	public void sendMsg(String method, Object... data) {
		if (isConnected()) {
			try {
				Message msg = createMessage("Blender.py", method, data);
				OutputStream out = control.getOutputStream();
				String json = Encoder.toJson(msg);
				info("sending p%s", json);
				out.write(json.getBytes());
			} catch (Exception e) {
				error(e);
			}
		} else {
			error("not connected");
		}
	}

	@Override
	public Status test() {
		Status status = super.test();
		try {

			Runtime.start("gui", "GUIService");
			Blender blender = (Blender) Runtime.start(getName(), "Blender");
			if (!blender.connect()) {
				throw new Exception("could not connect");
			}

			blender.getVersion();

			Arduino arduino01 = (Arduino) Runtime.start("arduino01", "Arduino");

			blender.attach(arduino01);
			sleep(3000);
			// left.biceps0
			// i01.head.neck
			Servo neck = (Servo) Runtime.start("jaw2", "Servo");

			// Servo rothead = (Servo) Runtime.start("i01.head.rothead",
			// "Servo");

			neck.attach(arduino01, 7);
			// rothead.attach(arduino01, 9);

			// rothead.moveTo(90);
			neck.moveTo(90);
			sleep(100);
			// rothead.moveTo(120);
			neck.moveTo(120);
			sleep(100);
			// rothead.moveTo(0);
			neck.moveTo(0);
			sleep(100);
			// rothead.moveTo(90);
			neck.moveTo(90);
			sleep(100);
			// rothead.moveTo(120);
			neck.moveTo(120);
			sleep(100);
			// rothead.moveTo(0);
			neck.moveTo(0);
			sleep(100);

			// servo01.sweep();
			// servo01.stop();
			neck.detach();

			blender.getVersion();
			// blender.toJson();
			// blender.toJson();

		} catch (Exception e) {
			status.addError(e);
		}

		return status;
	}

	public void toJson() {
		sendMsg("toJson");
	}

}
