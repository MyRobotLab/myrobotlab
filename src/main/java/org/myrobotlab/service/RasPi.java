package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.BoardType;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.i2c.I2CFactory;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.service.abstracts.AbstractMicrocontroller;
import org.myrobotlab.service.config.RasPiConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.slf4j.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.Pin;
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
 * all control offered by the Pi4J project.
 * 
 * More Info : http://pi4j.com/
 * 
 */
public class RasPi extends AbstractMicrocontroller implements I2CController, GpioPinListenerDigital {

  @Override
  public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
    // display pin state on console
    log.info(" --> GPIO PIN STATE CHANGE: {} = {}", event.getPin(), event.getState());
  }

  public static class I2CDeviceMap {
    transient public I2CBus bus;
    transient public I2CDevice device;
    public int deviceHandle;
    public String serviceName;
  }

  /**
   * default bus current bus of raspi service
   */
  String bus = "1";

  public static final int INPUT = 0x0;

  public final static Logger log = LoggerFactory.getLogger(RasPi.class);

  public static final int OUTPUT = 0x1;

  private static final long serialVersionUID = 1L;

  transient GpioController gpio;

  protected Map<Integer, Set<String>> validAddresses = new HashMap<>();

  /**
   * for attached devices
   */
  HashMap<String, I2CDeviceMap> i2cDevices = new HashMap<String, I2CDeviceMap>();

  /**
   * "quick fix" - no subscriptions nor listeners are made with other services,
   * so I created this to show the references to i2c device services
   */
  protected Set<String> attachedServices = new HashSet<>();

  // FIXME - all wiringPi type of i2c access should be removed
  // Pi4j nicely abstracts it away - this interface should be used
  @Deprecated
  private boolean wiringPi = false; // Defined to be able to switch between

  protected String boardType = null;

  public RasPi(String n, String id) {
    super(n, id);

    Platform platform = Platform.getLocalInstance();
    log.info("platform is {}", platform);
    log.info("architecture is {}", platform.getArch());

    try {
      boardType = SystemInfo.getBoardType().toString();
      gpio = GpioFactory.getInstance();
      log.info("Executing on Raspberry PI");
      getPinList();
    } catch (Exception e) {
      error("raspi service requires arm %s is not arm - %s", getName(), e.getMessage());
    }
  }

  /*
   * @Override public void attach(String name) { ServiceInterface si =
   * Runtime.getService(name); if
   * (I2CControl.class.isAssignableFrom(si.getClass())) {
   * attachI2CControl((I2CControl) si); return; } }
   * 
   * @Override public void detach(String name) { ServiceInterface si =
   * Runtime.getService(name); if
   * (I2CControl.class.isAssignableFrom(si.getClass())) {
   * detachI2CControl((I2CControl) si); return; } }
   */

  @Override
  public void attach(Attachable service) throws Exception {
    if (I2CControl.class.isAssignableFrom(service.getClass())) {
      attachI2CControl((I2CControl) service);
      return;
    }
  }

  @Override
  public void detach(Attachable service) {
    if (I2CControl.class.isAssignableFrom(service.getClass())) {
      detachI2CControl((I2CControl) service);
      return;
    }
  }

  @Override
  public void attachI2CControl(I2CControl control) {

    attachedServices.add(control.getName());
    // This part adds the service to the mapping between
    // busAddress||DeviceAddress
    // and the service name to be able to send data back to the invoker
    String key = String.format("%d.%d", Integer.parseInt(control.getBus()), Integer.decode(control.getAddress()));

    if (i2cDevices.containsKey(key)) {
      log.error("Device {} {} {} already exists.", control.getBus(), control.getAddress(), control.getName());
    } else {
      createI2cDevice(Integer.parseInt(control.getBus()), Integer.decode(control.getAddress()), control.getName());
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
        broadcastState();
        log.info("Created device for {} key {}", serviceName, key);
      } catch (Exception e) {
        log.error("createI2cDevice failed", e);
        error(e.getMessage());
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
      i2cDevices.remove(key);
      control.detachI2CController(this);
    }
    attachedServices.remove(control.getName());
  }

  public void digitalWrite(int pin, int value) {
    log.info("digitalWrite {} {}", pin, value);
    // msg.digitalWrite(pin, value);
    PinDefinition pinDef = addressIndex.get(pin);
    GpioPinDigitalMultipurpose gpio = ((GpioPinDigitalMultipurpose) pinDef.getPinImpl());
    if (value == 0) {
      gpio.low();
    } else {
      gpio.high();
    }
    invoke("publishPinDefinition", pinDef);
  }

  @Override
  public void disablePin(int address) {
    PinDefinition pin = addressIndex.get(address);
    pin.setEnabled(false);
    ((GpioPinDigitalMultipurpose) pin.getPinImpl()).removeListener();
    PinDefinition pinDef = addressIndex.get(address);
    invoke("publishPinDefinition", pinDef);
  }

  @Override
  public void enablePin(int address) {
    enablePin(address, 0);
  }

  @Override
  public void enablePin(int address, int rate) {
    PinDefinition pinDef = addressIndex.get(address);
    GpioPinDigitalMultipurpose gpio = ((GpioPinDigitalMultipurpose) pinDef.getPinImpl());
    gpio.addListener(this);
    pinDef.setEnabled(true);
    invoke("publishPinDefinition", pinDef); // broadcast pin change
  }

  @Override /* services attached - not i2c devices */
  public Set<String> getAttached() {
    // return i2cDevices.keySet();
    return attachedServices;
  }

  @Override
  public List<PinDefinition> getPinList() {

    for (Pin pin : RaspiPin.allPins()) {

      // pin.getSupportedPinModes()
      PinDefinition pindef = new PinDefinition(getName(), pin.getAddress());
      pindef.setPinName(pin.getName());
      EnumSet<PinMode> modes = pin.getSupportedPinModes();
      // FIXME - the raspi definitions are "better" they have input & ouput
      // FIXME - reconcile rxtx
      // FIXME - get pull up resistance
      if (modes.contains(PinMode.DIGITAL_OUTPUT)) {
        pindef.setDigital(true);
      }
      if (modes.contains(PinMode.ANALOG_OUTPUT)) {
        pindef.setAnalog(true);
      }
      if (modes.contains(PinMode.PWM_OUTPUT)) {
        pindef.setAnalog(true);
      }

      addressIndex.put(pin.getAddress(), pindef);
      pinIndex.put(pin.getName(), pindef);

      // GpioPinDigitalInput provisionedPin = gpio.provisionDigitalInputPin(pin,
      // pull);
      // provisionedPin.setShutdownOptions(true); // unexport pin on program
      // shutdown
      // provisionedPins.add(provisionedPin); // add provisioned pin to
      // collection
    }

    return new ArrayList<PinDefinition>(addressIndex.values());
  }

  /**
   * Check if wiringPi library is used. Returns true when wiringPi library is
   * used
   * 
   * @return true if library used
   * 
   */
  @Deprecated
  public boolean getWiringPi() {
    return wiringPi;
  }

  @Override // FIXME - I2CControl has bus why is it supplied here as a parameter
            // or why
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
        ++bytesRead;
      }
    } else {
      try {
        bytesRead = devicedata.device.read(buffer, 0, buffer.length);
      } catch (IOException e) {
        error("i2c could not read");
        log.error("i2cRead threw", e);
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

    PinDefinition pinDef = addressIndex.get(pin);
    if (mode == INPUT) {
      pinDef.setPinImpl(gpio.provisionDigitalMultipurposePin(RaspiPin.getPinByAddress(pin), PinMode.DIGITAL_INPUT));
    } else {
      pinDef.setPinImpl(gpio.provisionDigitalMultipurposePin(RaspiPin.getPinByAddress(pin), PinMode.DIGITAL_OUTPUT));
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
   *          wiring status for the pi
   * 
   */
  @Deprecated
  public void setWiringPi(boolean status) {
    this.wiringPi = status;
  }

  @Override
  public void startService() {
    super.startService();
    try {
      log.info("Initiating i2c");
      I2CFactory.getInstance(Integer.parseInt(bus));
      log.info("i2c initiated on bus {}", bus);
      // scan(); takes too long
    } catch (IOException e) {
      log.error("i2c initiation failed", e);
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

    PinDefinition pinDef = addressIndex.get(address);
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

    Runtime.start("servo01", "Servo");
    Runtime.start("ada16", "Adafruit16CServoDriver");
    Runtime.start(String.format("rasPi%d", i), "RasPi");
    WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
    webgui.autoStartBrowser(false);
    webgui.startService();

  }

  @Override
  public void reset() {
    // TODO Auto-generated method stub
    // reset pins/i2c devices/gpio pins
  }

  @Override
  public BoardInfo getBoardInfo() {
    RaspiPin.allPins();
    // FIXME - this needs more work .. BoardInfo needs to be an interface where
    // RasPiInfo is derived
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

  public void scan() {
    scan(null);
  }

  public void scan(Integer busNumber) {

    if (busNumber == null) {
      busNumber = Integer.parseInt(bus);
    }

    try {

      I2CBus bus = I2CFactory.getInstance(busNumber);

      validAddresses = new HashMap<>();

      if (!validAddresses.containsKey(busNumber)) {
        validAddresses.put(busNumber, new HashSet<>());
      }

      Set<String> addresses = validAddresses.get(busNumber);

      for (int i = 1; i < 128; i++) {
        try {
          I2CDevice device = bus.getDevice(i);
          device.write((byte) 0);
          addresses.add(Integer.toHexString(i));
        } catch (Exception ignore) {
        }
      }

      log.info("scan found: ---");
      for (String a : addresses) {
        log.info("address: " + a);
      }
      log.info("----------");
    } catch (Exception e) {
      error("cannot access i2c bus %d", busNumber);
      log.error("scan threw", e);
    }

    broadcastState();
  }

  @Override
  public ServiceConfig getConfig() {
    RasPiConfig config = new RasPiConfig();
    return config;
  }

  public ServiceConfig apply(ServiceConfig c) {
    RasPiConfig config = (RasPiConfig) c;
    return c;
  }

}
