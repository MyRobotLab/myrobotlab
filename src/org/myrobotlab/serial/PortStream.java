package org.myrobotlab.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         A necessary class to wrap references to rxtxLib in something which
 *         can be dynamically loaded. Without this abstraction any platform
 *         which did was not supported for by rxtx would not be able to use the
 *         Serial service or ports.
 * 
 * 
 */
public class PortStream extends Port {

  public final static Logger log = LoggerFactory.getLogger(PortStream.class);

  private InputStream in;
  private OutputStream out;

  public PortStream(String portName) throws IOException {
    super(portName);
  }

  public PortStream(String portName, InputStream in, OutputStream out) throws IOException {
    super(portName);
    this.in = in;
    this.out = out;
  }

  public int available() throws IOException {
    return in.available();
  }

  public InputStream getInputStream() {
    return in;
  }

  public OutputStream getOutputStream() {
    return out;
  }

  @Override
  public List<String> getPortNames() {
    // no "new" ports to contribute in
    // the "pure" Java (non-JNI/JNA) world...
    return new ArrayList<String>();
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

  public void setInputStream(InputStream in) {
    this.in = in;
  }

  public void setOutputStream(OutputStream out) {
    this.out = out;
  }

  public boolean setParams(int rate, int databits, int stopbits, int parity) {
    log.debug(String.format("setSerialPortParams %d %d %d %d", rate, databits, stopbits, parity));
    return true;
  }

  @Override
  public void write(int data) throws IOException {
    out.write(data);
    // WOW - PipedOutputStream auto flushes about 1 time every second :P
    // we force flushing here !
    out.flush();
  }
  
  public void write(int[] data) throws IOException {
    // TODO: is there a more effecient way to do this?
    for (int i = 0; i < data.length; i++) {
      out.write(data[i]);
    }
  }

  @Override
  public boolean isHardware() {
    return false;
  }

}
