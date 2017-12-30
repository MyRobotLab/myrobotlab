package org.myrobotlab.codec.serial;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.interfaces.LoggingSink;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public abstract class Codec {

  public final static Logger log = LoggerFactory.getLogger(Codec.class);

  // TODO - use ByteBuffer for codecs - only concern is the level of Java
  // supported
  // including the level of Android OS - Android did not have a ByteBuffer
  // until ??? version
  // TODO - possibly model after the apache codec / encoder / decoder design
  /*
   * Object encode(Object source) ;
   * 
   * Object decode(Object source) ;
   */

  BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
  // Integer timeout = null;
  Integer timeout = 1000;
  int maxQueue = 1024;

  LoggingSink sink = null;;

  static Properties keyToType = new Properties();;

  static {

    keyToType.put("asc", "Ascii");
    keyToType.put("ascii", "Ascii");
    keyToType.put("hex", "Hex");
    keyToType.put("dec", "Decimal");
    keyToType.put("decimal", "Decimal");
    keyToType.put("ard", "ArduinoMsg");
    keyToType.put("arduino", "ArduinoMsg");
  }

  static public Codec getDecoder(String key, LoggingSink sink)
      throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException {
    if (key == null) {
      return null;
    }
    String fullTypeName = String.format("org.myrobotlab.codec.serial.%sCodec", keyToType.getProperty(key.toLowerCase(), "decimal"));

    Codec formatter = null;
    Class<?> clazz = Class.forName(fullTypeName);
    if (sink == null) {
      formatter = (Codec) clazz.newInstance();
    } else {
      Constructor<?> c = clazz.getConstructor(new Class[] { LoggingSink.class });
      formatter = (Codec) c.newInstance(sink);
    }
    return formatter;
  }

  public Codec() {
  }

  // FIXME - register - each codec dynamically registers

  public Codec(LoggingSink sink) {
    this.sink = sink;
  }

  public void clear() {
    queue.clear();
  }

  public String decode() {
    try {
      if (timeout != null) {
        return queue.poll(timeout, TimeUnit.MILLISECONDS);
      }
      return queue.take();
    } catch (Exception e) {
      // don't care
    }
    return null;
  }

  final public String decode(int newByte) {
    String decoded = decodeImpl(newByte);
    if (decoded != null && maxQueue > queue.size()) {
      queue.add(decoded);
    }
    return decoded;
  }

  abstract public String decode(int[] msgs);

  abstract public String decodeImpl(int newByte);

  abstract public int[] encode(String source);

  public void error(String format, Object... args) {
    if (sink != null) {
      sink.error(format, args);
    } else {
      log.error(String.format(format, args));
    }
  }

  abstract public String getCodecExt();

  abstract public String getKey();

  public int getMaxQueue() {
    return maxQueue;
  }

  public BlockingQueue<String> getQueue() {
    return queue;
  }

  public Integer getTimeout() {
    return timeout;
  }

  public void setMaxQueue(int maxQueue) {
    this.maxQueue = maxQueue;
  }

  public void setQueue(BlockingQueue<String> queue) {
    this.queue = queue;
  }

  public Integer setTimeout(Integer timeoutms) {
    this.timeout = timeoutms;
    return timeoutms;
  }
}
