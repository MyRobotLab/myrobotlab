package org.myrobotlab.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

public class VirtualSerialPort implements SerialDevice {
	public String name;
	public BlockingQueue<Byte> rx = new LinkedBlockingQueue<Byte>();
	public BlockingQueue<Byte> tx = new LinkedBlockingQueue<Byte>();
	private boolean isOpen = false;

	public final static Logger log = LoggerFactory.getLogger(VirtualSerialPort.class.getCanonicalName());

	Object srcport = new Object();

	// I could support a list of SerialDeviceEventListeners but RXTX does not ..
	// so modeling their poor design
	public SerialDeviceEventListener listener;
	// RXThread rxthread;
	private boolean notifyOnDataAvailable;

	private VirtualSerialPort nullModem = null;

	public static void makeNullModem(VirtualSerialPort vp0, VirtualSerialPort vp1) {
		// save for future references
		vp0.nullModem = vp1;
		vp1.nullModem = vp0;

		// share single pair of queues
		vp0.rx = vp1.tx;
		vp0.tx = vp1.rx;

	}

	/*
	 * class RXThread extends Thread {
	 * 
	 * int currentDataSize = 0;
	 * 
	 * public RXThread(){ super(String.format("%s_virtual_rx",name)); } public
	 * void run() { try { while (isOpen) { rx.peek(); //userRX.add(b); // TODO -
	 * generate only at currentDataSize intervals if (listener != null &&
	 * notifyOnDataAvailable) { SerialDeviceEvent sde = new
	 * SerialDeviceEvent(srcport, SerialDeviceEvent.DATA_AVAILABLE, false,
	 * true); listener.serialEvent(sde); } }
	 * 
	 * } catch (Exception e) { Logging.logException(e); } } }
	 */
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
		// rxthread = new RXThread();
		// rxthread.start();

	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public void close() {
		isOpen = false;
		// rxthread.interrupt();
		// rxthread = null;
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
		// TODO Auto-generated method stub
		notifyOnDataAvailable = enable;
	}

	@Override
	public void write(int data) throws IOException {
		write((byte) data);
	}


	@Override
	public void write(int[] data) throws IOException {
		for (int i = 0; i < data.length; ++i) {
			write((byte) data[i]);
		}
	}

	@Override
	public int read() throws IOException {
		try {
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
				data[i] = rx.take();
			}
			return read;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return 0;
	}
}
