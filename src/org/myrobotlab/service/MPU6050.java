package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceEvent;
import org.slf4j.Logger;

public class MPU6050 extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MPU6050.class);
	
	private transient Serial serial;
	private transient SerialDevice serialDevice;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("serial", "Serial", "Serial");
		return peers;
	}

	public MPU6050(String n) {
		super(n);
	}
	
	public boolean connect(String port){
		if (serial == null){
			serial = (Serial)Runtime.start("serial", "Serial");
		}
		
		return serial.connect(port);
	}

	@Override
	public String getDescription() {
		return "used as a general mpu6050";
	}
	
	public static final int MAGIC_NUMBER = 170;
	static final public int MAX_MSG_LEN = 64;
	//StringBuilder rxDebug = new StringBuilder ();

	// TODO - define as int[] because Java bytes suck !
	byte[] msg = new byte[64]; // TODO define outside
	int newByte;
	int byteCount = 0;
	int msgSize = 0;
	private int error_arduino_to_mrl_rx_cnt;
	private int error_mrl_to_arduino_rx_cnt;
	StringBuilder debugRX = new StringBuilder();
	
	public void serialEvent(SerialDeviceEvent event) {
		switch (event.getEventType()) {
		case SerialDeviceEvent.BI:
		case SerialDeviceEvent.OE:
		case SerialDeviceEvent.FE:
		case SerialDeviceEvent.PE:
		case SerialDeviceEvent.CD:
		case SerialDeviceEvent.CTS:
		case SerialDeviceEvent.DSR:
		case SerialDeviceEvent.RI:
		case SerialDeviceEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialDeviceEvent.DATA_AVAILABLE:

			// at this point we should have a complete message
			// the msg contains only daat // METHOD | P0 | P1 ... | PN

			// msg buffer
			// msg[0] METHOD
			// msg[1] P0
			// msg[2] P1
			// ...
			// msg[N] PN+1
			try {

				/*
				 * DON'T EVER MAKE THIS NON-MONOLITHIC ! - the overhead of going
				 * into another method is high and will end up loss of data in
				 * serial communications !
				 * 
				 * Optimize this as much as possible !
				 */
				while (serialDevice.isOpen() && (newByte = serialDevice.read()) > -1) {

					++byteCount;

					if (byteCount == 1) {
						if (newByte != MAGIC_NUMBER) {
							byteCount = 0;
							msgSize = 0;
							warn(String.format("Arduino->MRL error - bad magic number %d - %d rx errors", newByte, ++error_arduino_to_mrl_rx_cnt));
							//dump.setLength(0);
						}
						continue;
					} else if (byteCount == 2) {
						// get the size of message
						if (newByte > 64) {
							byteCount = 0;
							msgSize = 0;
							error(String.format("Arduino->MRL error %d rx sz errors", ++error_arduino_to_mrl_rx_cnt));
							continue;
						}
						msgSize = (byte) newByte;
						// dump.append(String.format("MSG|SZ %d", msgSize));
					} else if (byteCount > 2) {
						// remove header - fill msg data - (2) headbytes -1
						// (offset)
						// dump.append(String.format("|P%d %d", byteCount,
						// newByte));
						msg[byteCount - 3] = (byte) newByte;
					}

					// process valid message
					if (byteCount == 2 + msgSize) {
						// log.error("A {}", dump.toString());
						// dump.setLength(0);

						// MSG CONTENTS = FN | D0 | D1 | ...
						byte function = msg[0];
						//log.info(String.format("%d", msg[1]));
						switch (function) {

						/* FIXME
						case MRLCOMM_ERROR: {
							++error_mrl_to_arduino_rx_cnt;
							error("MRL->Arduino rx %d type %d", error_mrl_to_arduino_rx_cnt, msg[1]);
							break;
						}

						*/
						
						
						
						
						default: {
							error(formatMRLCommMsg("unknown serial event <- ", msg, msgSize));
							break;
						}
						
						
						} // end switch

						if (log.isDebugEnabled()) {
							log.debug(formatMRLCommMsg("serialEvent <- ", msg, msgSize));
						}

						// processed msg
						// reset msg buffer
						msgSize = 0;
						byteCount = 0;
					}
				} // while (serialDevice.isOpen() && (newByte =
					// serialDevice.read()) > -1

			} catch (Exception e) {
				++error_mrl_to_arduino_rx_cnt;
				error("msg structure violation %d", error_mrl_to_arduino_rx_cnt);
				// try again ?
				msgSize = 0;
				byteCount = 0;
				Logging.logException(e);
			}

		}

	}
	
	public String formatMRLCommMsg(String prefix, byte[] message, int size){
		debugRX.setLength(0);
		if (prefix != null){
			debugRX.append(prefix);
		}
		debugRX.append(String.format("MAGIC_NUMBER|SZ %d|FN %d", size, message[0]));
		for (int i = 1; i < size; ++i) {
			debugRX.append(String.format("|P%d %d", i, message[i]));
		}
		return debugRX.toString();
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();

		try {

			MPU6050 mpu6050 = (MPU6050)Runtime.start("mpu6050", "MPU6050");
			mpu6050.test();
			
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	@Override
	public String[] getCategories() {
		return new String[] {"microcontroller"};
	}
}
