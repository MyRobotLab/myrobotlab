package org.myrobotlab.serial;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;

import org.myrobotlab.framework.QueueStats;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.slf4j.Logger;

/**
 * 
 * @author Grog
 *
 */

public abstract class Port implements Runnable, SerialControl {

  public final static Logger log = LoggerFactory.getLogger(Port.class);

  String portName;
  String threadName;

  // needs to be owned by Serial
  transient HashMap<String, SerialDataListener> listeners = null;

  // transient CountDownLatch opened = null;
  // transient CountDownLatch closed = null;

  final transient Object lock = new Object();

  static int pIndex = 0;

  // thread related
  transient Thread readingThread = null;
  boolean listening = false;

  QueueStats stats = new QueueStats();

  // hardware serial port details
  // default convention over configuration
  // int rate = 57600;
  int rate = 115200;
  int dataBits = 8;
  int stopBits = 1;
  int parity = 0;

  int txErrors;
  int rxErrors;

  boolean isOpen = false;

  // necessary - to be able to invoke
  // "nameless" port implementation to query "hardware" ports
  // overloading a "Port" and a PortQuery - :P
  public Port() {
  }

  public Port(String portName) {
    this.stats.name = portName;
    this.portName = portName;
    stats.interval = 1000;
  }

  public Port(String portName, int rate, int dataBits, int stopBits, int parity) throws IOException {
    this(portName);
    this.rate = rate;
    this.dataBits = dataBits;
    this.stopBits = stopBits;
    this.parity = parity;
  }

  public void close() {

    // closed = new CountDownLatch(1);
    listening = false;
    if (readingThread != null) {
      readingThread.interrupt();
    }
    readingThread = null;
    /*
     * try { closed.await(); } catch (Exception e) { Logging.logError(e); }
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
    // opened = new CountDownLatch(1);
    // try {
    if (this.listeners != null) {
      log.info("here");
    }
    this.listeners = listeners;
    if (readingThread == null) {
      ++pIndex;
      threadName = String.format("%s.portListener %s", portName, pIndex);
      readingThread = new Thread(this, threadName);
      readingThread.start();
      /*
       * - this might be a good thing .. wait until the reading thread starts -
       * but i don't remember if JSSC works this way synchronized (lock) {
       * lock.wait(); }
       */
    } else {
      log.info(String.format("%s already listening", portName));
    }
    // Thread.sleep(100); - added connect retry logic in Arduino
    // taking out arbitrary sleeps
    // } catch (InterruptedException e) {
    // }
  }

  public void open() throws IOException {
    log.info(String.format("opening port %s", portName));
    isOpen = true;
  }

  abstract public int read() throws Exception;

  /**
   * reads from Ports input stream and puts it on the Serials main RX line - to
   * be published and buffered
   */
  @Override
  public void run() {

    /*
     * - this might be a good thing .. wait until the reading thread starts -
     * but i don't remember if JSSC works this way synchronized(lock){
     * lock.notifyAll(); }
     */

    log.info(String.format("listening on port %s", portName));
    listening = true;
    Integer newByte = -1;
    try {
      // opened.countDown();
      // TODO - if (Queue) while take()
      // normal streams are processed here - rxtx is abnormal
      while (listening && ((newByte = read()) > -1)) { // "real" java byte
        // 255 / -1 will
        // kill this
    	// log.info(String.format("%d",newByte));
        for (String key : listeners.keySet()) {
          listeners.get(key).onByte(newByte);
          // log.info(String.format("%d",newByte));
        }
        ++stats.total;
        if (stats.total % stats.interval == 0) {

          stats.ts = System.currentTimeMillis();
          stats.delta = stats.ts - stats.lastTS;
          stats.lineSpeed = (8 * stats.interval) / stats.delta;
          for (String key : listeners.keySet()) {
            listeners.get(key).updateStats(stats);
          }
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
      /*
       * if (closed != null){ closed.countDown(); }
       */
      log.info(String.format("stopped listening on %s", portName));
    }
  }

  /**
   * "real" serial function stubbed out in the abstract class in case the serial
   * implementation does not actually implement this method e.g. (bluetooth,
   * iostream, tcp/ip)
   * @param state s
   */
  public void setDTR(boolean state) {
  }

  /**
   * The way rxtxLib currently works - is it will give a -1 on a read when it
   * has no data to give although in the specification this means end of stream
   * - for rxtxLib this is not necessarily the end of stream. The implementation
   * there - the thread is in rxtx - and will execute serialEvent when serial
   * data has arrived. This might have been a design decision. The thread which
   * calls this is in the rxtxlib - so we have it call the run() method of a
   * non-active thread class.
   * 
   * needs to be buried in rxtxlib implementation
   * @param b the byte
   * @throws Exception TODO
   * 
   */
  abstract public void write(int b) throws Exception;
  
  abstract public void write(int[] data) throws Exception;

  public boolean setParams(int rate, int dataBits, int stopBits, int parity) throws Exception {
    // TODO Auto-generated method stub
    return false;
  }

}
