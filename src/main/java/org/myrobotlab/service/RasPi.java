package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.BoardType;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.i2c.I2CFactory;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractMicrocontroller;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.slf4j.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.system.SystemInfo;
import com.pi4j.wiringpi.I2C;
import com.pi4j.wiringpi.SoftPwm;

/**
 * 
 * RasPi - This is the MyRobotLab Service for the Raspberry Pi. It should allow
 * all control offered by the great Pi4J project.
 * 
 * More Info : http://pi4j.com/
 * 
 */
// TODO Ensure that only one instance of RasPi can execute on each RaspBerry PI
// TODO pi4j implementation NOWORKY : I/O errors, dont know why, not know this
// enough :)
public class RasPi extends AbstractMicrocontroller implements I2CController {

  public static class GpioPinListener implements GpioPinListenerDigital {
    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
      // display pin state on console
      log.info(" --> GPIO PIN STATE CHANGE: {} = {}", event.getPin(), event.getState());
    }

  }

  public static class I2CDeviceMap {
    public I2CBus bus;
    public I2CDevice device;
    public int deviceHandle;
    public String serviceName;
  }

  // i2c bus
  transient public static I2CBus i2c;

  public static final int INPUT = 0x0;

  public final static Logger log = LoggerFactory.getLogger(RasPi.class);

  public static final int OUTPUT = 0x1;

  private static final long serialVersionUID = 1L;

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(RasPi.class.getCanonicalName());
    meta.addDescription("Raspberry Pi service used for accessing specific RasPi hardware like th GPIO pins and i2c");
    meta.addCategory("i2c", "control");
    meta.setSponsor("Mats");
    meta.addDependency("com.pi4j", "pi4j-core", "1.2");
    meta.addDependency("com.pi4j", "pi4j-native", "1.2", "pom");
    return meta;
  }

  transient GpioController gpio;

  transient GpioPinDigitalOutput gpio01;

  transient GpioPinDigitalOutput gpio03;

  transient HashMap<String, I2CDeviceMap> i2cDevices = new HashMap<String, I2CDeviceMap>();

  private boolean wiringPi = true; // Defined to be able to switch between

  public RasPi(String n, String id) {
    super(n, id);

    Platform platform = Platform.getLocalInstance();
    log.info("platform is {}", platform);
    log.info("architecture is {}", platform.getArch());

    if ("arm".equals(platform.getArch()) || "armv7.hfp".equals(platform.getArch())) {
      gpio = GpioFactory.getInstance();
      log.info("Executing on Raspberry PI");
    } else {
      // we should be running on a Raspberry Pi
      log.error("architecture is not arm");
    }
    getPinList();
  }

  @Override
  public void attachI2CControl(I2CControl control) {

    // This part adds the service to the mapping between
    // busAddress||DeviceAddress
    // and the service name to be able to send data back to the invoker

    String key = String.format("%d.%d", Integer.parseInt(control.getDeviceBus()), Integer.decode(control.getDeviceAddress()));

    if (i2cDevices.containsKey(key)) {
      log.error("Device {} {} {} already exists.", control.getDeviceBus(), control.getDeviceAddress(), control.getName());
    } else {
      createI2cDevice(Integer.parseInt(control.getDeviceBus()), Integer.decode(control.getDeviceAddress()), control.getName());
      control.attachI2CController(this);
    }
  }

  void createI2cDevice(int bus, int address, String serviceName) {

    String key = String.format("%d.%d", bus, address);
    I2CDeviceMap devicedata = new I2CDeviceMap();
    if (!i2cDevices.containsKey(key)) {
      try {
        if (wiringPi) {
          int deviceHandle = I2C.wiringPiI2CSetup(address);
          devicedata.serviceName = serviceName;
          devicedata.bus = null;
          devicedata.device = null;
          devicedata.deviceHandle = deviceHandle;
        } else {
          I2CBus i2cBus = I2CFactory.getInstance(bus);
          I2CDevice device = i2cBus.getDevice(address);
          devicedata.serviceName = serviceName;
          devicedata.bus = i2cBus;
          devicedata.device = device;
          devicedata.deviceHandle = -1;
        }
        i2cDevices.put(key, devicedata);
        log.info("Created device for {} key {}", serviceName, key);
      } catch (NumberFormatException | IOException e) {
        Logging.logError(e);
      }
    }
  }

  @Override
  public void detachI2CControl(I2CControl control) {
    // This method should delete the i2c device entry from the list of
    // I2CDevices
    // The order of the detach is important because the higher level service may
    // want to execute something that
    // needs this service to still be availabe
    String key = String.format("%d.%d", Integer.parseInt(control.getDeviceBus()), Integer.decode(control.getDeviceAddress()));
    if (i2cDevices.containsKey(key)) {
      control.detachI2CController(this);
      i2cDevices.remove(key);
    }

  }

  public void digitalWrite(int pin, int value) {
    log.info("digitalWrite {} {}", pin, value);
    // msg.digitalWrite(pin, value);
    PinDefinition pinDef = pinIndex.get(pin);
    if (value == 0) {
      pinDef.getGpioPin().low();
    } else {
      pinDef.getGpioPin().high();
    }
    invoke("publishPinDefinition", pinDef);
  }

  @Override
  public void disablePin(int address) {
    PinDefinition pin = pinIndex.get(address);
    pin.setEnabled(false);
    pin.getGpioPin().removeListener();
    PinDefinition pinDef = pinIndex.get(address);
    invoke("publishPinDefinition", pinDef);
  }

  @Override
  public void enablePin(int address) {
    enablePin(address, 0);
  }

  @Override
  public void enablePin(int address, int rate) {
    PinDefinition pin = pinIndex.get(address);
    pin.getGpioPin().addListener(new GpioPinListener());
    pin.setEnabled(true);
    invoke("publishPinDefinition", pin); // broadcast pin change
  }

  @Override
  public Set<String> getAttached() {
    return i2cDevices.keySet();
  }

  @Override
  public List<PinDefinition> getPinList() {
    // FIXME - RasPi version have different pin maps
    // FIXME - self identify boardtype
    // FIXME - boardType -generates-> pinList
    // If the pinIndex is populated already, return it's values
    if (pinIndex != null) {
      return new ArrayList<PinDefinition>(pinIndex.values());
    }

    pinMap.clear();
    pinIndex.clear();
    List<PinDefinition> pinList = new ArrayList<PinDefinition>();

    try {
      // if (SystemInfo.getBoardType() == SystemInfo.BoardType.RaspberryPi_3B) {
      if (true) {
        for (int i = 0; i < 32; ++i) {
          PinDefinition pindef = new PinDefinition(getName(), i);
          String pinName = null;
          if (i == 16) {
            pindef.setRx(true);
          }
          if (i == 15) {
            pindef.setTx(true);
          }
          if (i <= 16 || i >= 21) {
            pinName = String.format("GPIO%d", i);
            pindef.setDigital(true);
          } else {
            pinName = String.format("Unused%d", i);
            pindef.setDigital(false);
          }
          pindef.setPinName(pinName);
          pindef.setAddress(i);
          pinIndex.put(i, pindef);
          pinMap.put(pinName, pindef);
          pinList.add(pindef);
        }
      } else {
        log.error("Unknown boardtype %{}", SystemInfo.getBoardType());
      }
    } catch (Exception e) {
      log.error("getPinList threw", e);
    }

    return pinList;
  }

  /**
   * Check if wiringPi library is used. Returns true when wiringPi library is
   * used
   * 
   * @return
   */
  public boolean getWiringPi() {
    return wiringPi;
  }

  @Override
  public int i2cRead(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {
    int bytesRead = 0;
    String key = String.format("%d.%d", busAddress, deviceAddress);
    I2CDeviceMap devicedata = i2cDevices.get(key);
    if (devicedata == null) {
      createI2cDevice(busAddress, deviceAddress, control.getName());
      devicedata = i2cDevices.get(key);
    }

    if (wiringPi) {
      for (int i = 0; i < size; i++) {
        buffer[i] = (byte) (I2C.wiringPiI2CRead(devicedata.deviceHandle) & 0xFF);
        log.debug("Read value {}", buffer[i]);
      }
    } else {
      try {
        bytesRead = devicedata.device.read(buffer, 0, buffer.length);
      } catch (IOException e) {
        Logging.logError(e);
      }
    }
    return bytesRead;
  }

  @Override
  public void i2cWrite(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {

    String key = String.format("%d.%d", busAddress, deviceAddress);
    I2CDeviceMap devicedata = i2cDevices.get(key);
    if (devicedata == null) {
      createI2cDevice(busAddress, deviceAddress, control.getName());
      devicedata = i2cDevices.get(key);
    }

    if (wiringPi) {
      int reg = buffer[0] & 0xFF;
      for (int i = 1; i < size; i++) {
        int value = buffer[i] & 0xFF;
        log.debug(String.format("Writing to register {} value {}", reg, value));
        I2C.wiringPiI2CWriteReg8(devicedata.deviceHandle, reg, value);
        reg++;
      }
    } else {
      try {
        devicedata.device.write(buffer, 0, size);
      } catch (IOException e) {
        Logging.logError(e);
      }
    }
  }

  @Override
  public int i2cWriteRead(I2CControl control, int busAddress, int deviceAddress, byte[] writeBuffer, int writeSize, byte[] readBuffer, int readSize) {

    if (writeSize != 1) {
      log.error("writeSize other than 1 is not yet supported in i2cWriteRead");
    }
    String key = String.format("%d.%d", busAddress, deviceAddress);
    I2CDeviceMap devicedata = i2cDevices.get(key);
    if (devicedata == null) {
      createI2cDevice(busAddress, deviceAddress, control.getName());
      devicedata = i2cDevices.get(key);
    }

    if (wiringPi) {
      for (int i = 0; i < readSize; i++) {
        readBuffer[i] = (byte) (I2C.wiringPiI2CReadReg8(devicedata.deviceHandle, (writeBuffer[0] + i) & 0xFF));
        log.debug("Read register {} value {}", (writeBuffer[0] + i) & 0xFF, readBuffer[i]);
      }
    } else {
      try {
        devicedata.device.read(writeBuffer, 0, writeBuffer.length, readBuffer, 0, readBuffer.length);
      } catch (IOException e) {
        Logging.logError(e);
      }
    }
    return readBuffer.length;
  }

  public void pinMode(int pin, int mode) {

    PinDefinition pinDef = pinIndex.get(pin);
    if (mode == INPUT) {
      pinDef.setGpioPin(gpio.provisionDigitalMultipurposePin(RaspiPin.getPinByAddress(pin), PinMode.DIGITAL_INPUT));
    } else {
      pinDef.setGpioPin(gpio.provisionDigitalMultipurposePin(RaspiPin.getPinByAddress(pin), PinMode.DIGITAL_OUTPUT));
    }
    invoke("publishPinDefinition", pinDef);
  }

  @Override
  public void pinMode(int address, String mode) {

    if (mode != null && mode.equalsIgnoreCase("INPUT")) {
      pinMode(address, INPUT);
    } else {
      pinMode(address, OUTPUT);
    }
  }

  @Override
  public PinData publishPin(PinData pinData) {
    // TODO Make sure this method is invoked when a pin value interrupt is
    // received
    // caching last value
    PinDefinition pinDef = getPin(pinData.pin);
    pinDef.setValue(pinData.value);
    return pinData;
  }
  
  // FIXME - return array
  // FIXME - return array
  public Integer[] scanI2CDevices(int busAddress) {
    log.info("scanning through I2C devices");
    ArrayList<Integer> list = new ArrayList<Integer>();
    try {
      /*
       * From its name we can easily deduce that it provides a communication
       * link between ICs (integrated circuits). I2C is multimaster and can
       * support a maximum of 112 devices on the bus. The specification declares
       * that 128 devices can be connected to the I2C bus, but it also defines
       * 16 reserved addresses.
       */
      I2CBus bus = I2CFactory.getInstance(busAddress);

      for (int i = 0; i < 128; ++i) {
        I2CDevice device = bus.getDevice(i);
        if (device != null) {
          try {
            device.read();
            list.add(i);
            /*
             * sb.append(i); sb.append(" ");
             */
            log.info("found device on address {}", i);
          } catch (Exception e) {
            log.warn("bad read on address {}", i);
          }

        }
      }
    } catch (Exception e) {
      Logging.logError(e);
    }

    Integer[] ret = list.toArray(new Integer[list.size()]);
    return ret;
  }

  /**
   * Forces usage of wiringPi library (
   * http://wiringpi.com/reference/i2c-library/ )
   * 
   * @param status
   */
  public void setWiringPi(boolean status) {
    this.wiringPi = status;
  }

  @Override
  public void startService() {
    super.startService();
    try {
      log.info("Initiating i2c");
      i2c = I2CFactory.getInstance(I2CBus.BUS_1);
      log.info("i2c initiated");
    } catch (IOException e) {
      // TODO Auto-generated catch block
      log.error("i2c initiation failed");
      Logging.logError(e);
    }
  }

  public void testGPIOOutput() {
    GpioPinDigitalMultipurpose pin = gpio.provisionDigitalMultipurposePin(RaspiPin.GPIO_02, PinMode.DIGITAL_INPUT, PinPullResistance.PULL_DOWN);
    log.info("Pin: {}", pin);
  }

  public void testPWM() {
    try {

      // initialize wiringPi library
      com.pi4j.wiringpi.Gpio.wiringPiSetup();

      // create soft-pwm pins (min=0 ; max=100)
      SoftPwm.softPwmCreate(1, 0, 100);

      // continuous loop
      while (true) {
        // fade LED to fully ON
        for (int i = 0; i <= 100; i++) {
          SoftPwm.softPwmWrite(1, i);
          Thread.sleep(100);
        }

        // fade LED to fully OFF
        for (int i = 100; i >= 0; i--) {
          SoftPwm.softPwmWrite(1, i);
          Thread.sleep(100);
        }
      }
    } catch (Exception e) {

    }
  }

  @Override
  public void write(int address, int value) {

    PinDefinition pinDef = pinIndex.get(address);
    pinMode(address, Arduino.OUTPUT);
    digitalWrite(address, value);
    // cache value
    pinDef.setValue(value);
  }

  public static void main(String[] args) {
    LoggingFactory.init("info");

    /*
     * RasPi.displayString(1, 70, "1");
     * 
     * RasPi.displayString(1, 70, "abcd");
     * 
     * RasPi.displayString(1, 70, "1234");
     * 
     * 
     * //RasPi raspi = new RasPi("raspi");
     */

    // raspi.writeDisplay(busAddress, deviceAddress, data)

    int i = 0;

    Runtime.start(String.format("rasPi%d", i), "RasPi");
    Runtime.createAndStart(String.format("rasGUI%d", i), "SwingGui");
    Runtime.createAndStart(String.format("rasPython%d", i), "Python");
    // Runtime.createAndStart(String.format("rasClock%d",i), "Clock");
   
  }

  @Override
  public void reset() {
    // TODO Auto-generated method stub
    // reset pins/i2c devices/gpio pins
  }

  @Override
  public BoardInfo getBoardInfo() {
    // FIXME - this needs more work .. BoardInfo needs to be an interface where RasPiInfo is derived
    return null;
  }

  @Override
  public List<BoardType> getBoardTypes() {
    // TODO Auto-generated method stub
    // FIXME - this need work
    return null;
  }

  // - add more pin mappings if desired ...
  @Override
  public Integer getAddress(String pin) {
    return Integer.parseInt(pin);
  }

}
