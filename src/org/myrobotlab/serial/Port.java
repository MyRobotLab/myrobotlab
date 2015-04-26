package org.myrobotlab.serial;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.myrobotlab.framework.QueueStats;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.slf4j.Logger;

/**
 * FIXME !!! - implement subclass abstract/interface - HardwarePort &
 * SoftwarePort
 * 
 * @author Grog
 *
 */

public abstract class Port implements Runnable, PortSource {

	public final static Logger log = LoggerFactory.getLogger(Port.class);

	String portName;
	String threadName;

	// needs to be owned by Serial
	HashMap<String, SerialDataListener> listeners = null;

	//transient CountDownLatch opened = null;
	//transient CountDownLatch closed = null;

	static int pIndex = 0;

	// thread related
	transient Thread readingThread = null;
	boolean listening = false;

	QueueStats stats = new QueueStats();

	// hardware serial port details
	// default convention over configuration
	int rate = 57600;
	int databits = 8;
	int stopbits = 1;
	int parity = 0;

	int txErrors;
	int rxErrors;

	boolean isOpen = false;

	// necessary - to be able to invoke
	// "nameless" port implementation to query "hardware" ports
	// overloading a "Port" and a PortQuery - :P
	public Port() { }

	public Port(String portName) {
		this.stats.name = portName;
		this.portName = portName;
		stats.interval = 1000;
	}

	public Port(String portName, int rate, int databits, int stopbits, int parity) throws IOException {
		this(portName);
		this.rate = rate;
		this.databits = databits;
		this.stopbits = stopbits;
		this.parity = parity;
	}

	public void close() {

//		closed = new CountDownLatch(1);
		listening = false;
		if (readingThread != null) {
			readingThread.interrupt();
		}
		readingThread = null;
/*		
		try {
			closed.await();
		} catch (Exception e) {
			Logging.logError(e);
		}
*/		

		// TODO - suppose to remove listeners ???
		log.info(String.format("closed port %s", portName));

	}

	public String getName() {
		return portName;
	}

	abstract public boolean isHardware();

	public boolean isListening() {
		return listening;
	}

	public boolean isOpen() {
		return isOpen;
	}

	public void listen(HashMap<String, SerialDataListener> listeners) {
//		opened = new CountDownLatch(1);
		this.listeners = listeners;
		if (readingThread == null) {
			++pIndex;
			threadName = String.format("%s.portListener %s", portName, pIndex);
			readingThread = new Thread(this, threadName);
			readingThread.start();
		} else {
			log.info(String.format("%s already listening", portName));
		}
/*		
		try {
			// we want to wait until our
			// reader has started and is
			// blocking on a read before
			// we proceed
			opened.await();
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
*/		
		try{
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
	}

	public void open() throws IOException {
		log.info(String.format("opening port %s", portName));
		isOpen = true;
	}

	abstract public int read() throws IOException, InterruptedException;

	/**
	 * reads from Ports input stream and puts it on the Serials main RX line -
	 * to be published and buffered
	 */
	@Override
	public void run() {

		log.info(String.format("listening on port %s", portName));
		listening = true;
		Integer newByte = -1;
		try {
//			opened.countDown();
			// TODO - if (Queue) while take()
			// normal streams are processed here - rxtx is abnormal
			while (listening && ((newByte = read()) > -1)) { // "real" java byte 255 / -1 will kill this
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
			}
			log.info(String.format("%s no longer listening - last byte %d ", portName, newByte));
		} catch (InterruptedException x) {
			log.info(String.format("InterruptedException %s stopping ", portName));
		} catch (InterruptedIOException c) {
			log.info(String.format("InterruptedIOException %s stopping ", portName));
		} catch (Exception e) {
			Logging.logError(e);
		} finally {
			// allow the thread calling close
			// to proceed
/*			if (closed != null){
				closed.countDown();
			}
*/			
			log.info(String.format("stopped listening on %s", portName));
		}
	}

	/**
	 * "real" serial function stubbed out in the abstract class in case the
	 * serial implementation does not actually implement this method e.g.
	 * (bluetooth, iostream, tcp/ip)
	 * 
	 * @param state
	 */
	public void setDTR(boolean state) {
	}

	/**
	 * The way rxtxLib currently works - is it will give a -1 on a read when it
	 * has no data to give although in the specification this means end of
	 * stream - for rxtxLib this is not necessarily the end of stream. The
	 * implementation there - the thread is in rxtx - and will execute
	 * serialEvent when serial data has arrived. This might have been a design
	 * decision. The thread which calls this is in the rxtxlib - so we have it
	 * call the run() method of a non-active thread class.
	 * 
	 * needs to be buried in rxtxlib implementation
	 * 
	 */

	abstract public void write(int b) throws IOException;

}
