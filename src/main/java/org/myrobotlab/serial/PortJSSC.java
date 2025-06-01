package org.myrobotlab.serial;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.string.StringUtil;
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
  // default string to use if not otherwise specified to support Instantiator.
  private static final String NULL_PORT = "NULL_PORT";
  transient SerialPort port = null;

  public PortJSSC() {
    super(NULL_PORT);
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
    try {
      List<String> ret = Arrays.asList(SerialPortList.getPortNames());
      log.info("Serial Ports {}", ret);
      return ret;
    } catch (Exception e) {
      log.error("getPortNames threw", e);
    }
    // null or empty?
    return new ArrayList<String>();
  }

  @Override
  public boolean isCTS() {
    try {
      return port.isCTS();
    } catch (SerialPortException e) { /* don't care */
    }
    return false;
  }

  @Override
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
      // add self as a event listener, and listen to MASK_RXCHAR
      port.addEventListener(this, SerialPort.MASK_RXCHAR);
      // jssc uses own listening thread to push events
      listening = true;
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
      log.error("Serial port close exception.", e);
    }
    port = null;
  }

  @Override
  public byte[] readBytes() {
    try {
      // read what's available
      return port.readBytes();
    } catch (SerialPortException e) {
      log.warn("Port: {} Read Bytes Exception", portName, e);
      return null;
    }
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

  @Override
  public void write(byte[] data) throws Exception {
    if (debug && debugTX) {
      String dataString = StringUtil.byteArrayToIntString(data);
      log.info("Sending Byte Array: >{}< to Port: {}", dataString, portName);
    }
    port.writeBytes(data);
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
    // in addEventListener
    if (event.isRXCHAR()) {
      try {
        int byteCount = event.getEventValue();
        if (byteCount == 0) {
          // no data available.
          return;
        }
        byte[] buffer = port.readBytes(byteCount);
        if (buffer == null) {
          // no data available.
          return;
        }
        // we have data, let's notify the listeners.
        // log.info("Reading Data from port {} - Data:>{}<", getName(), buffer);
        for (String key : listeners.keySet()) {
          // TODO: feels like this should be synchronized or maybe the buffer
          // should be immutable?
          listeners.get(key).onBytes(buffer);
        }
        // gather stats about this serial event (bytes read...)
        for (int i = 0; i < buffer.length; i++) {
          ++stats.total;
          if (stats.total % stats.interval == 0) {
            stats.ts = System.currentTimeMillis();
            long delta = stats.ts - stats.lastTS;
            // avoid the divide by zero
            if (delta != 0) {
              log.debug("===stats - dequeued total {} - {} bytes in {} ms {} Kbps", stats.total, stats.interval, delta, 8 * stats.interval / delta);
            }
            // TODO: should we be calling this still?
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