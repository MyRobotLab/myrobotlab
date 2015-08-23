package org.myrobotlab.serial;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         A necessary class to wrap references to rxtxLib in something which can be dynamically loaded. Without this abstraction any platform which did was not supported for by
 *         rxtx would not be able to use the Serial service or ports.
 * 
 */
public class PortJSSC extends Port implements PortSource, SerialPortEventListener, Serializable {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(PortJSSC.class);

	// private gnu.io.RXTXPort port;

	// private CommPortIdentifier commPortId;

	transient SerialPort port = null;

	public PortJSSC() {
		super();
	}

	public PortJSSC(String portName, int rate, int dataBits, int stopBits, int parity) throws IOException {
		super(portName, rate, dataBits, stopBits, parity);
		// commPortId = CommPortIdentifier.getPortIdentifier(portName);
	}

	/*
	 * public int available() throws IOException { port. return in.available(); }
	 */
	
	public boolean isOpen(){
		if (port != null){
			return port.isOpened();
		}
		
		return false;
	}

	public int getBaudRate() {
		return rate;
	}

	public int getDataBits() {
		return dataBits;
	}

	@Override
	public String getName() {
		return portName;
	}

	public int getParity() {
		return parity;
	}

	@Override
	public List<String> getPortNames() {

		ArrayList<String> ret = new ArrayList<String>();
		try {
			String[] portNames = SerialPortList.getPortNames();
			for (int i = 0; i < portNames.length; i++) {
				ret.add(portNames[i]);
				System.out.println(portNames[i]);
			}
		} catch (Exception e) {
			Logging.logError(e);
		}
		return ret;
	}

	public int getStopBits() {
		return stopBits;
	}

	public boolean isCTS() throws SerialPortException {
		return port.isCTS();
	}

	public boolean isDSR() throws SerialPortException {
		return port.isDSR();
	}

	@Override
	public void open() throws IOException {
		try {
			port = new SerialPort(portName);
			port.openPort();
			port.setParams(rate, dataBits, stopBits, parity);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public void close() {
		try {
			
			listening = false;
			readingThread = null;// is dead anyway
			port.closePort();
			// port.notifyOnDataAvailable(false);
			
			port.removeEventListener();
		} catch (Exception e) {
			Logging.logError(e);
		}
		port = null;
	}

	// / FIXME KLUDGY !!!!!

	@Override
	public int read() throws Exception {
		return port.readIntArray(1)[0];
		/*
		if (port == null) {
			return -1;
		}
		int[] ret = port.readIntArray(1);
		if (ret != null) {
			return ret[0];
		} else {
			return -1;
		}
		*/
	}

	@Override
	public void setDTR(boolean state) {
		try {
			port.setDTR(state);
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public void setParams(int rate, int dataBits, int stopBits, int parity) throws Exception {

		log.debug(String.format("setSerialPortParams %d %d %d %d", rate, dataBits, stopBits, parity));
		port.setParams(rate, dataBits, stopBits, parity);
	}

	public void setRTS(boolean state) {
		try {
			port.setRTS(state);
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	@Override
	public void write(int data) throws Exception {
		port.writeInt(data);
	}

	// FIXME - check to make sure these are the same as InputStream &
	// OutputStream
	public void write(int[] data) throws Exception {
		for (int i = 0; i < data.length; ++i) {
			write(data[i]);
		}
	}

	@Override
	public boolean isHardware() {
		return true;
	}

	/*
	 * @Override public void run() { // we don't use countDown - because rxtx manages its own threads(sortof :P) log.info("no port thread in rxtxlib"); try { Thread.sleep(300); }
	 * catch (InterruptedException e) { } // allow the .listen() in Port // to proceed // opened.countDown(); }
	 */

	/**
	 * rxtxlib's "serial event handling" - would be more simple if they just implemented InputStream correctly :P
	 */
	@Override
	public void serialEvent(SerialPortEvent event) {
		log.info(String.format("rxtx event on port %s", portName));

		Integer newByte = -1;

		try {
			while (listening && ((newByte = read()) > -1)) {
				// listener.onByte(newByte); // <-- FIXME ?? onMsg() < ???
				for (String key : listeners.keySet()) {
					listeners.get(key).onByte(newByte);
				}
				++stats.total;
				if (stats.total % stats.interval == 0) {
					stats.ts = System.currentTimeMillis();
					log.error(String.format("===stats - dequeued total %d - %d bytes in %d ms %d Kbps", stats.total, stats.interval, stats.ts - stats.lastTS, 8 * stats.interval
							/ (stats.ts - stats.lastTS)));
					// publishQueueStats(stats);
					stats.lastTS = stats.ts;
				}
				// log.info(String.format("%d",newByte));
				// rxtx leave whenever it has no new data to delver with a -1
				// which is not what an Java InputStream is supposed to do..
			}

			log.info(String.format("%d", newByte));
		} catch (Exception e) {
			++rxErrors;
			Logging.logError(e);
		}

	}

}
