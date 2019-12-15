package org.myrobotlab.serial;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 * @author GroG
 * 
 *         A necessary class to wrap references to rxtxLib in something which
 *         can be dynamically loaded. Without this abstraction any platform
 *         which did was not supported for by rxtx would not be able to use the
 *         Serial service or ports.
 * 
 */
public class PortJSSC extends Port implements SerialControl, SerialPortEventListener, Serializable {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(PortJSSC.class);

  transient SerialPort port = null;

  public PortJSSC() {
    super();
  }

  public PortJSSC(String portName, int rate, int dataBits, int stopBits, int parity) throws IOException {
    super(portName, rate, dataBits, stopBits, parity);
  }

  @Override
  public boolean isOpen() {
    if (port != null) {
      return port.isOpened();
    }
    return false;
  }

  // FIXME - better way to handle this across all port types
  @Override
  public List<String> getPortNames() {

    ArrayList<String> ret = new ArrayList<String>();
    try {
      String[] portNames = SerialPortList.getPortNames();
      for (int i = 0; i < portNames.length; i++) {
        ret.add(portNames[i]);
        log.info(portNames[i]);
      }
    } catch (Exception e) {
      log.error("getPortNames threw", e);
    }
    return ret;
  }

  public boolean isCTS() {
    try {
      return port.isCTS();
    } catch (SerialPortException e) { /* don't care */
    }
    return false;
  }

  public boolean isDSR() {
    try {
      return port.isDSR();
    } catch (SerialPortException e) {/* don't care */
    }
    return false;
  }

  @Override
  public void open() throws IOException {
    try {
      port = new SerialPort(portName);
      port.openPort();
      port.setParams(rate, dataBits, stopBits, parity);
      // TODO - add self as a event listener, and listen to MASK_RXCHAR
      // it would probably be a good idea to register for "all" then filter on
      // the SerialEvent - then
      // it would be easier to get notified on other events besides just serial
      // reads .. eg. dtr etc..
      port.addEventListener(this, SerialPort.MASK_RXCHAR);
    } catch (Exception e) {
      throw new IOException(String.format("could not open port %s  rate %d dataBits %d stopBits %d parity %d", portName, rate, dataBits, stopBits, parity), e);
    }
  }

  @Override
  public void close() {
    try {

      listening = false;

      port.closePort();
      // FIXME - JSSC issue (IMHO)
      // if a listener doesn't exist it throws ? meh :P
      // port.removeEventListener();
      // port.notifyOnDataAvailable(false);
    } catch (Exception e) {
      log.error("close threw", e);
    }
    port = null;
  }

  /**
   * FIXME - the "int read()" should provide a timeout to be supplied ! This
   * needs refactoring in the interface
   */
  @Override
  public int read() throws Exception {
    return read(1, 20000)[0];
  }

  /**
   * FIXME - this more powerful read should be propegated up to the interface
   * 
   * @param numbytes
   * @param timeout
   * @return
   * @throws Exception
   */
  public int[] read(int numbytes, int timeout) throws Exception {
    return port.readIntArray(numbytes, timeout);
  }

  @Override
  public void setDTR(boolean state) {
    try {
      port.setDTR(state);
    } catch (Exception e) {
      log.error("setDTR threw", e);
    }
  }

  @Override
  public boolean setParams(int rate, int dataBits, int stopBits, int parity) throws Exception {
    super.setParams(rate, dataBits, stopBits, parity);
    try {
      if (port == null || !port.isOpened()) {
        log.error("port not opened or is null");
        return false;
      }

      return port.setParams(rate, dataBits, stopBits, parity);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public void setRTS(boolean state) {
    try {
      port.setRTS(state);
    } catch (Exception e) {
      log.error("setRTS threw", e);
    }
  }

  @Override
  public void run() {
    log.info("PortJSSC started - will be using jssc library thread to push serial events");
  }

  @Override
  public void write(int data) throws Exception {
    port.writeInt(data);
  }

  /**
   * Java made a mistake having InputStream and OutputStream abstract
   * classes vs interfaces - perhaps a PortInputStream and PortOutputStream can
   * be created in the future....
   */
  public void write(int[] data) throws Exception {
    // use the writeIntArray method to batch this operation.
    if (debug && debugTX) {
      StringBuilder b = new StringBuilder();
      for (int i = 0; i < data.length; i++) {
        b.append("" + Integer.toString(data[i]) + "");
        if (i != data.length - 1)
          b.append(",");
      }
      log.debug("Sending Int Array: {}", b);
    }

    port.writeIntArray(data);
  }

  @Override
  public boolean isHardware() {
    return true;
  }

  /**
   * This is call back from jssc library - pushing serial data
   * 
   */
  @Override
  public void serialEvent(SerialPortEvent event) {
    // FYI - if you want more events processed here - you need to register them
    // in setParams
    if (event.isRXCHAR()) {// If data is available

      log.debug("Serial Receive Event fired.");
      byte[] buffer = null;
      try {
        buffer = this.port.readBytes(event.getEventValue());
        for (int i = 0; i < buffer.length; i++) {

          for (String key : listeners.keySet()) {
            listeners.get(key).onByte((int) (buffer[i] & 0xFF));
          }
          ++stats.total;
          if (stats.total % stats.interval == 0) {
            stats.ts = System.currentTimeMillis();
            log.info("===stats - dequeued total {} - {} bytes in {} ms {} Kbps", stats.total, stats.interval, stats.ts - stats.lastTS,
                8 * stats.interval / (stats.ts - stats.lastTS));
            // publishQueueStats(stats);
            stats.lastTS = stats.ts;
          }
        }
      } catch (Exception e) {
        log.error("serialEvent readBytes threw", e);
      }
    }
  }
}
