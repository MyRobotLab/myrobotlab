package org.myrobotlab.pickToLight;

import java.util.HashMap;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

/*
 * 0x80 = The I/R sensor input (must write a 1 bit initially) 
 0x7C = Unused bits 
 0x02 = LED (Write 0 to turn ON the LED; write 1 to turn OFF) 
 0x01 = "Chip select" for 7-segment display controller 

 Since the 0x80 bit must be written with a 1 to enable the sensor input, it is recommended that 
 ALL writes to the I/O expander write that bit high.  
 As a result, there are only four possible values that should be written:

 0x80 - 128 = LED ON and display selected 
 0x81 - 129 = LED ON and display de-selected 
 0x82 - 130 = LED OFF and display selected 
 0x83 - 131 = LED OFF and display de-selected

 */

public class Module {

  public class BlinkThread extends Thread {
    public int blinkNumber = 5;
    public int blinkDelay = 100;
    public String value = "";
    public boolean leaveDisplayOn = true;
    public boolean leaveLEDOn = false;

    @Override
    public void run() {
      int count = 0;
      while (count < 5) {
        // selector &= ~MASK_DISPLAY; WRONG !
        if (value != null) {
          display(value);
        }
        ledOn();
        Service.sleep(blinkDelay);
        if (value != null) {
          display("");
        }
        ledOff();
        // selector |= MASK_LED; WRONG !!
        Service.sleep(blinkDelay);
        ++count;
      }

      if (leaveDisplayOn) {
        // selector &= ~MASK_DISPLAY; WONG
        display(value);
      }

      if (leaveLEDOn) {
        ledOn();
      }
    }
  }

  public class CycleThread extends Thread {
    public boolean isRunning = false;
    int delay = 300;
    String msg;

    public CycleThread(String msg, int delay) {
      this.msg = "    " + msg + "    ";
      this.delay = delay;
    }

    @Override
    public void run() {
      isRunning = true;
      try {
        while (isRunning) {
          // start with scroll on page
          for (int i = 0; i < msg.length() - 3; ++i) {
            display(msg.substring(i, i + 4));
            sleep(delay);
          }
        }
      } catch (InterruptedException e) {
        isRunning = false;
      }
    }
  }

  public final static Logger log = LoggerFactory.getLogger(Module.class);

  transient private com.pi4j.io.i2c.I2CBus i2cbus;

  transient private com.pi4j.io.i2c.I2CDevice device;
  Address2 address = new Address2();
  String type;
  String version; // hardware version

  String state;
  int selector = 0x83; // IR selected - LED OFF
  static public final int MASK_DISPLAY = 0x01;

  static public final int MASK_LED = 0x02;

  static public final int MASK_SENSOR = 0x80;
  // private String lastValue = "";

  static private boolean translationInitialized = false;

  transient static HashMap<String, Byte> translation = new HashMap<String, Byte>();

  transient CycleThread ct = null;

  public static void initTranslation() {

    translation.put("", (byte) 0);
    translation.put(" ", (byte) 0);

    translation.put(":", (byte) 3);
    // translation.put("of", (byte)0)

    translation.put("a", (byte) 119);
    translation.put("b", (byte) 124);
    translation.put("c", (byte) 57);
    translation.put("d", (byte) 94);
    translation.put("e", (byte) 121);
    translation.put("f", (byte) 113);
    translation.put("g", (byte) 111);
    translation.put("h", (byte) 118);
    translation.put("i", (byte) 48);
    translation.put("J", (byte) 30);
    translation.put("k", (byte) 118);
    translation.put("l", (byte) 56);
    translation.put("m", (byte) 21);
    translation.put("n", (byte) 84);
    translation.put("o", (byte) 63);
    translation.put("p", (byte) 115);
    translation.put("q", (byte) 103);
    translation.put("r", (byte) 80);
    translation.put("s", (byte) 109);
    translation.put("t", (byte) 120);
    translation.put("u", (byte) 62);
    translation.put("v", (byte) 98);
    translation.put("x", (byte) 118);
    translation.put("y", (byte) 110);
    translation.put("z", (byte) 91);

    translation.put("-", (byte) 64);
    // translation.put("dot", (byte)???);

    translation.put("0", (byte) 63);
    translation.put("1", (byte) 6);
    translation.put("2", (byte) 91);
    translation.put("3", (byte) 79);
    translation.put("4", (byte) 102);
    translation.put("5", (byte) 109);
    translation.put("6", (byte) 125);
    translation.put("7", (byte) 7);
    translation.put("8", (byte) 127);
    translation.put("9", (byte) 111);

    translationInitialized = true;
  }

  public static byte translate(char c) {
    byte b = 0;
    String s = String.valueOf(c).toLowerCase();
    if (translation.containsKey(s)) {
      b = translation.get(s);
    } else {
      log.info(String.format("character %s not translated", c));
    }
    return b;
  }

  public Module(int bus, int i2cAddress) {
    try {

      address.setI2CBus(bus);
      address.setI2CAddress(i2cAddress);

      Platform platform = Platform.getLocalInstance();

      if (platform.isArm()) {
        // create I2C communications bus instance
        i2cbus = I2CFactory.getInstance(bus);

        // create I2C device instance
        device = i2cbus.getDevice(i2cAddress);
      }

      if (!translationInitialized) {
        initTranslation();
      }

    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  public void blinkOff(String msg, int blinkNumber, int blinkDelay) {
    log.info(String.format("blinkOff %s", msg));
    BlinkThread b = new BlinkThread();
    b.blinkNumber = blinkNumber;
    b.blinkDelay = blinkDelay;
    b.value = msg;
    b.leaveDisplayOn = false;
    b.start();
  }

  public void blinkOn(String msg, int blinkNumber, int blinkDelay) {
    BlinkThread b = new BlinkThread();
    b.blinkNumber = blinkNumber;
    b.blinkDelay = blinkDelay;
    b.value = msg;
    b.start();
  }

  public void clear() {
    cycleStop();
    selector |= MASK_LED;
    display("");
  }

  public void cycle(String msg) {
    if (ct != null) {
      cycleStop();
    }
    ct = new CycleThread(msg, 300);
    ct.start();
  }

  public void cycle(String msg, int delay) {
    if (ct != null) {
      cycleStop();
    }
    ct = new CycleThread(msg, delay);
    ct.start();
  }

  public void cycleStop() {
    if (ct != null) {
      ct.isRunning = false;
      ct.interrupt();
      ct = null;
      display("    ");
    }
  }

  public String display(String str) {
    // lastValue = str;

    // d1 d2 : d3 d4
    byte[] display = new byte[] { 0, 0x17, 0, 0, 0, 0 };

    if (str == null || str == "") {
      writeDisplay(display);
      return str;
    }

    if (str.length() < 4) {
      str = String.format("%4s", str);
    }

    display[5] = translate(str.charAt(0));
    display[4] = translate(str.charAt(1));
    display[3] = translate(str.charAt(2));
    display[2] = translate(str.charAt(3));

    writeDisplay(display);

    return str;
  }

  public int getI2CAddress() {
    return address.getI2CAddress();
  }

  public int getI2CBus() {
    return address.getI2CBus();
  }

  public int ledOff() {
    try {
      selector |= MASK_LED;
      log.info("ledOff {}", Integer.toHexString(selector));
      device.write((byte) selector);
    } catch (Exception e) {
      log.error(String.format("ledOff device %d error in writing", address.controller));
      Logging.logError(e);
    }

    return selector;
  }

  // TODO - should only have to wrap the highest level transaction (WebGui
  // Thread) -
  // such that smaller transaction handling is not necessary
  public int ledOn() {
    try {
      selector &= ~MASK_LED;
      log.info("ledOn {}", Integer.toHexString(selector));
      device.write((byte) selector);
    } catch (Exception e) {
      log.error(String.format("ledOn device %d error in writing", address.controller));
      Logging.logError(e);
    }
    return selector;
  }

  public void logByteArray(byte[] data) {
    logByteArray("", data);
  }

  public void logByteArray(String prefix, byte[] data) {

    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < data.length; ++i) {
      sb.append(data[i]);
      if (i != data.length) {
        sb.append(",");
      }
    }

    log.info(String.format("%s %s", prefix, sb.toString()));
  }

  public int readSensor() {
    try {
      // sudo i2cset -y 1 0x10 0x83
      // sudo i2cget -y 1 0x10

      // lame should bitwise this
      // I have a feeling LED and display affect
      // YOU CANT HAVE LED ON WHEN POLLING SENSOR ??
      // device.write((byte) 0x83);
      // need to do bitwise mask !!!
      return device.read();

    } catch (Exception e) {
      Logging.logError(e);
    }
    return -1;
  }

  public byte[] writeDisplay(byte[] data) {
    if (device == null) {
      log.error("device is null");
      return data;
    }

    try {

      if (log.isDebugEnabled()) {
        logByteArray("writeDisplay", data);
      }

      // select display
      device.write((byte) (selector &= ~MASK_DISPLAY)); // FIXME NOT
      // CORRECT !

      I2CDevice display = i2cbus.getDevice(0x38);
      display.write(data, 0, data.length);

      // de-select display
      device.write((byte) (selector |= MASK_DISPLAY));// FIXME NOT CORRECT
      // ! for LED

    } catch (Exception e) {
      log.error(String.format("writeDisplay device %d error in writing", address.controller));
      Logging.logError(e);
    }

    return data;
  }

}
