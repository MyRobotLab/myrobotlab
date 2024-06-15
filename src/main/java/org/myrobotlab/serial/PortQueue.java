package org.myrobotlab.serial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

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
public class PortQueue extends Port {

  public final static Logger log = LoggerFactory.getLogger(PortQueue.class);

  private BlockingQueue<Integer> in;
  private BlockingQueue<Integer> out;

  public PortQueue(String portName) {
    super(portName);
  }

  public PortQueue(String portName, BlockingQueue<Integer> in, BlockingQueue<Integer> out) {
    super(portName);
    this.in = in;
    this.out = out;
  }

  public int available() throws IOException {
    return in.size();
  }

  @Override
  public List<String> getPortNames() {
    // no "new" ports to contribute in
    // the "pure" Java (non-JNI/JNA) world...
    return new ArrayList<String>();
  }

  /**
   * Returns a byte array containing what's available on the in queue. Null if
   * no data is available. This reads bytes from the output queue. That data
   * written to the queue by MrlCommIno can be read here and taken off the
   * queue.
   */
  @Override
  public byte[] readBytes() {
    try {
      // here we should take as many bytes as there are to return.
      int size = in.size();
      if (size == 0) {
        // no data to process.
        return null;
      }
      byte[] data = new byte[size];
      for (int i = 0; i < size; i++) {
        data[i] = in.take().byteValue();
      }
      return data;
    } catch (InterruptedException e) {
      // log.debug("Interrupted PortQueue in readBytes.");
      return null;
    }
  }

  @Override
  public boolean setParams(int rate, int databits, int stopbits, int parity) {
    log.debug("setSerialPortParams {} {} {} {}", rate, databits, stopbits, parity);
    return true;
  }

  @Override
  public void write(int data) throws IOException {
    out.add(data);
  }

  @Override
  public void write(byte[] data) throws IOException {
    // convert this to match the output queue of integer by upcasting the byte
    // array
    for (int i = 0; i < data.length; i++) {
      out.add(data[i] & 0xFF);
    }
  }

  @Override
  public boolean isHardware() {
    return false;
  }

}
