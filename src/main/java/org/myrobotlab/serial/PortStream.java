package org.myrobotlab.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
  public byte[] readBytes() {
    try {
      int size = in.available();
      if (size < 1) {
        // no data to read.. just return null
        return null;
      }
      // ok, we have data! so, let's read
      byte[] data = new byte[size];
      int numRead = in.read(data);
      if (numRead != size) {
        // Uh oh.. let's just log this warning, we shouldn't see it.
        log.warn("Port Stream Read possible error.  numRead {} does not equal size {}", numRead, size);
        // return just the buffer of what we think was actually read.
        return Arrays.copyOfRange(data, 0, numRead);
      }
      // Assume that all data was read properly and return the full buffer
      return data;

    } catch (IOException e) {
      log.warn("Interrupted PortStream in readBytes.  Perhaps port was closed?", e);
    }
    return null;
  }

  public void setInputStream(InputStream in) {
    this.in = in;
  }

  public void setOutputStream(OutputStream out) {
    this.out = out;
  }

  @Override
  public boolean setParams(int rate, int databits, int stopbits, int parity) {
    log.debug("setSerialPortParams {} {} {} {}", rate, databits, stopbits, parity);
    return true;
  }

  @Override
  public void write(int data) throws IOException {
    out.write(data);
    // WOW - PipedOutputStream auto flushes about 1 time every second :P
    // we force flushing here !
    out.flush();
  }

  @Override
  public void write(byte[] data) throws IOException {
    out.write(data);
    // TODO: should we flush here?
    out.flush();
  }

  @Override
  public boolean isHardware() {
    return false;
  }

}
