package org.myrobotlab.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.RXTXPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * @author GroG
 * 
 *         A necessary class to wrap references to rxtxLib in something which
 *         can be dynamically loaded. Without this abstraction any platform
 *         which did was not supported for by rxtx would not be able to use the
 *         Serial service or ports.
 * 
 */
public class PortRXTX extends Port implements SerialControl, SerialPortEventListener {

  public final static Logger log = LoggerFactory.getLogger(PortRXTX.class);

  transient private gnu.io.RXTXPort port;

  transient private CommPortIdentifier commPortId;

  transient private InputStream in;
  transient private OutputStream out;

  public PortRXTX() {
    super();
  }

  public PortRXTX(String portName, int rate, int databits, int stopbits, int parity)
      throws IOException, PortInUseException, UnsupportedCommOperationException, NoSuchPortException {
    super(portName, rate, databits, stopbits, parity);
    commPortId = CommPortIdentifier.getPortIdentifier(portName);
  }

  public int available() throws IOException {
    return in.available();
  }

  public int getBaudRate() {
    return port.getBaudRate();
  }

  public String getCurrentOwner() {
    if (commPortId != null)
      return commPortId.getCurrentOwner();
    return null;
  }

  public int getDataBits() {
    return port.getDataBits();
  }

  public InputStream getInputStream() {
    return port.getInputStream();
  }

  @Override
  public String getName() {
    return commPortId.getName();
  }

  public OutputStream getOutputStream() {
    return port.getOutputStream();
  }

  public int getParity() {
    return port.getParity();
  }

  @Override
  public List<String> getPortNames() {

    ArrayList<String> ret = new ArrayList<String>();
    try {
      CommPortIdentifier portId;
      Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
      while (portList.hasMoreElements()) {
        portId = (CommPortIdentifier) portList.nextElement();
        String inPortName = portId.getName();
        log.info(inPortName);
        if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {

          ret.add(portId.getName());

        }
      }
    } catch (Exception e) {
      Logging.logError(e);
    }
    return ret;
  }

  public int getPortType() {
    return commPortId.getPortType();
  }

  public int getStopBits() {
    return port.getStopBits();
  }

  public boolean isCD() {
    return port.isCD();
  }

  public boolean isCTS() {
    return port.isCTS();
  }

  public boolean isCurrentlyOwned() {
    return commPortId.isCurrentlyOwned();
  }

  public boolean isDSR() {
    return port.isDSR();
  }

  public boolean isDTR() {
    return port.isDTR();
  }

  public boolean isRI() {
    return port.isRI();
  }

  public boolean isRTS() {
    return port.isRTS();
  }

  @Override
  public void open() throws IOException {
    try {
      if (port != null) {
        log.info(String.format("port %s already open", portName));
        return;
      }
      log.info(String.format("opening %s", portName));
      port = (RXTXPort) commPortId.open(portName, 1000);
      port.setSerialPortParams(rate, dataBits, stopBits, parity);
      in = port.getInputStream();
      out = port.getOutputStream();
      setParams(rate, dataBits, stopBits, parity);
      port.addEventListener(this);
      port.notifyOnDataAvailable(true);
      listening = true;
      isOpen = true;
      log.info(String.format("opened %s", portName));
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public void close() {
    port.removeEventListener();
    // port.notifyOnDataAvailable(false);
    listening = false;
    readingThread = null;// is dead anyway
    port.close();
    out = null;
    in = null;
    /*
     * seen strategy of closing rxtxLib ports in a different thread to keep from
     * infinite blocking .. but new Thread(){ public void run(){ log.info(
     * "closing streams begin");
     * 
     * try { port.close(); } catch(Exception e){ Logging.logError(e); }
     * 
     * try { out.flush(); out.close(); } catch(Exception e){
     * Logging.logError(e); }
     * 
     * try { in.close(); } catch(Exception e){ Logging.logError(e); }
     * 
     * 
     * log.info("closing streams end"); } }.start();
     */
    port = null;
  }

  @Override
  public int read() throws IOException {
    return in.read();
  }

  // WORTHLESS INPUTSTREAM FUNCTION !! -- because if the size of the buffer
  // is ever bigger than the read and no end of stream has occurred
  // it will block forever :P
  public int read(byte[] data) throws IOException {
    return in.read(data);
  }

  @Override
  public void setDTR(boolean state) {
    port.setDTR(state);
  }

  @Override
  public boolean setParams(int rate, int databits, int stopbits, int parity) throws IOException {
    log.debug(String.format("setSerialPortParams %d %d %d %d", rate, databits, stopbits, parity));
    try {
      port.setSerialPortParams(rate, databits, stopbits, parity);
      return true;
    } catch (UnsupportedCommOperationException e) {
      new IOException(e);
    }
    return false;
  }

  public void setRTS(boolean state) {
    port.setRTS(state);
  }

  @Override
  public void write(int data) throws IOException {
    out.write(data);
  }

  // FIXME - check to make sure these are the same as InputStream &
  // OutputStream
  public void write(int[] data) throws IOException {
    for (int i = 0; i < data.length; ++i) {
      out.write(data[i]);
    }
  }

  @Override
  public boolean isHardware() {
    return true;
  }

  @Override
  public void run() {
    // we don't use countDown - because rxtx manages its own threads(sortof :P)
    log.info("no port thread in rxtxlib");
    try {
      Thread.sleep(300);
    } catch (InterruptedException e) {
    }
    // allow the .listen() in Port
    // to proceed
    // opened.countDown();
  }

  /**
   * rxtxlib's "serial event handling" - would be more simple if they just
   * implemented InputStream correctly :P
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
          log.error(String.format("===stats - dequeued total %d - %d bytes in %d ms %d Kbps", stats.total, stats.interval, stats.ts - stats.lastTS,
              8 * stats.interval / (stats.ts - stats.lastTS)));
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
