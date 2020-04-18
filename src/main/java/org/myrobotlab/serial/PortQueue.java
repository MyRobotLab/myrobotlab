package org.myrobotlab.serial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.NotImplementedException;
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
   * Returns a byte array containing what's available on the in queue.
   * Null if no data is available.
   * This reads bytes from the output queue.  That data written to the queue
   * by MrlCommIno can be read here and taken off the queue.
   */
  public byte[] readBytes() {
    // log.info("Read Bytes on Port Queue called.");
    try {
      // here we should take as many bytes as there are to return.
      int size = in.size();
      //int size = in.size();
      if (size == 0) {
        // no data to process. return null.
        return null;
      }
      byte[] data = new byte[size];
      for (int i = 0 ; i < size; i++) {
        data[i] = in.take().byteValue();
      }
      // log.info("Read value from the input stream. size:{} bytes: {}", size, data);
      return data;
    } catch (InterruptedException e) {
      // we don't care, just return if we were interrupted.
      // log.debug("Interrupted PortQueue in readBytes.");
      return null;
    }
  }
  
  public boolean setParams(int rate, int databits, int stopbits, int parity) {
    log.debug("setSerialPortParams {} {} {} {}", rate, databits, stopbits, parity);
    return true;
  }

  @Override
  public void write(int data) throws IOException {
    // log.info("Writing int to the output queue. {} size {}" , data, out.size());
    out.add(data);
    // WOW - PipedOutputStream auto flushes about 1 time every second :P
    // we force flushing here !
  }

  public void write(byte[] data) throws IOException {
    // TODO: is there a more effecient way to do this?
    for (int i = 0; i < data.length; i++) {
      // write(data[i]);
      out.add(data[i] & 0xFF);
    }
    //log.info("Writing Byte Array {} size:{}", data, out.size());
    
  }
  
  public void write(int[] data) throws IOException {
    // TODO: is there a more effecient way to do this?
    for (int i = 0; i < data.length; i++) {
      // TODO: is there a type casting problem here? or does it promote properly 0xFF?
      write(data[i]);
    }
  }

  @Override
  public boolean isHardware() {
    return false;
  }

}
