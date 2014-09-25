package org.myrobotlab.serial;

import java.io.IOException;
import java.util.TooManyListenersException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

public class VirtualSerialPort implements SerialDevice {
	public String name;
	public BlockingQueue<Integer> rx = new LinkedBlockingQueue<Integer>();
	public BlockingQueue<Integer> tx = new LinkedBlockingQueue<Integer>();
	
	private boolean isOpen = false;

	public final static Logger log = LoggerFactory.getLogger(VirtualSerialPort.class.getCanonicalName());

	Object srcport = new Object();

	// I could support a list of SerialDeviceEventListeners but RXTX does not ..
	// so modeling their poor design :P
	public SerialDeviceEventListener listener;
	RXThread rxthread;
	private boolean notifyOnDataAvailable;
	
	// poison pill "real" bytes are from 0 to 255 ;)
	static public final Integer SHUTDOWN = -1;

	private VirtualSerialPort nullModem = null;

	// underlying serial event management thread
	class RXThread extends Thread {

		int currentDataSize = 0;
		long pollingPause = 100;

		public RXThread() {
			super(String.format("%s_virtual_rx", name));
		}

		public void run() {
			try {
				while (isOpen) {
					Integer b = rx.peek(); // userRX.add(b); // TODO -generate only at
								// currentDataSize intervals
					if (listener != null && notifyOnDataAvailable && b != null) {
						SerialDeviceEvent sde = new SerialDeviceEvent(srcport, SerialDeviceEvent.DATA_AVAILABLE, false, true);
						listener.serialEvent(sde);
					}
					
					Thread.sleep(pollingPause);
				}

			} catch (Exception e) {
				if (e instanceof InterruptedException) {
					log.info("shutting down rx thread on port");
				} else {
					Logging.logException(e);
				}
			}
		}
	}

	public VirtualSerialPort(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int available() {
		return rx.size();
	}

	// GAWD this is lame ! why throw on an open .. someone is exception happy ;P
	@Override
	public void open() throws SerialDeviceException {
		if (isOpen) {
			log.warn(String.format("%s already open", name));
			throw new SerialDeviceException();
		}

		isOpen = true;

		rxthread = new RXThread();
		rxthread.start();
	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public void close() {
		isOpen = false;
		if (rxthread != null){
			rxthread.interrupt();
		}
		rxthread = null;
	}

	@Override
	public void setParams(int b, int d, int s, int p) throws SerialDeviceException {
		log.warn("setParams does nothing on virtual serial ports - cuz they is wicked fast !");
	}

	@Override
	public void setDTR(boolean state) {
		log.warn("virtual devices don't have DTS");
	}

	@Override
	public void setRTS(boolean state) {
		log.warn("virtual devices don't have RTS");
	}

	// TODO - make this silly like RXTX - so that it throws TooManyListners :P
	@Override
	public void addEventListener(SerialDeviceEventListener lsnr) throws TooManyListenersException {
		if (nullModem != null) {
			// if is a null modem
			// then tx on one modem will be the thread
			// to generate the rx event on the other
			nullModem.listener = lsnr;
		} else {
			listener = lsnr;
		}
	}

	@Override
	public void notifyOnDataAvailable(boolean enable) {
		notifyOnDataAvailable = enable;
	}

	@Override
	public void write(int data) throws IOException {
		tx.add(data);
	}

	@Override
	public void write(int[] data) throws IOException {
		for (int i = 0; i < data.length; ++i) {
			tx.add(data[i]);
		}
	}

	@Override
	public int read() throws IOException {
		try {
			log.info("rx.take() size {}", rx.size());
			//rx.poll(100, unit);
			return rx.take();
		} catch (InterruptedException e) {
			return -1;
		}

	}

	@Override
	public int read(byte[] data) throws IOException {

		try {
			int read = Math.min(rx.size(), data.length);
			for (int i = 0; i < rx.size() && i < data.length; ++i) {
				data[i] = (byte)(rx.take() & 0xff);
			}
			return read;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return -1;
	}
	
	public void release(){
		rx.add(SHUTDOWN);
	}
}
