package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.arduino.BoardType;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.i2c.I2CFactory;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractMicrocontroller;
import org.myrobotlab.service.config.RasPiConfig;
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
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.system.SystemInfo;
//import com.pi4j.wiringpi.I2C;
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

  public static class I2CDeviceMap {
    transient public I2CBus bus;
    transient public I2CDevice device;
    public int deviceHandle;
    public String serviceName;

    public String toString() {
      return String.format("bus: %d deviceHandle: %d service: %s", bus.getBusNumber(), deviceHandle, serviceName);
    }
  }

  public final static Map<String, String> bcmToWiring = new HashMap<>();

  public static final int INPUT = 0x0;

  public final static Logger log = LoggerFactory.getLogger(RasPi.class);

  public static final int OUTPUT = 0x1;

  private static final long serialVersionUID = 1L;

  public final static Map<String, String> wiringToBcm = new HashMap<>();

  static {

    bcmToWiring.put("GPIO 0", "GPIO 27");
    bcmToWiring.put("GPIO 1", "GPIO 31");
    bcmToWiring.put("GPIO 2", "GPIO 8");
    bcmToWiring.put("GPIO 3", "GPIO 9");
    bcmToWiring.put("GPIO 4", "GPIO 7");
    bcmToWiring.put("GPIO 5", "GPIO 21");
    bcmToWiring.put("GPIO 6", "GPIO 22");
    bcmToWiring.put("GPIO 7", "GPIO 11");
    bcmToWiring.put("GPIO 8", "GPIO 10");
    bcmToWiring.put("GPIO 9", "GPIO 13");
    bcmToWiring.put("GPIO 10", "GPIO 12");
    bcmToWiring.put("GPIO 11", "GPIO 14");
    bcmToWiring.put("GPIO 12", "GPIO 27");
    bcmToWiring.put("GPIO 13", "GPIO 26");
    bcmToWiring.put("GPIO 14", "GPIO 15");
    bcmToWiring.put("GPIO 15", "GPIO 16");
    bcmToWiring.put("GPIO 16", "GPIO 25");
    bcmToWiring.put("GPIO 17", "GPIO 0");
    bcmToWiring.put("GPIO 18", "GPIO 1");
    bcmToWiring.put("GPIO 19", "GPIO 23");
    bcmToWiring.put("GPIO 20", "GPIO 28");
    bcmToWiring.put("GPIO 21", "GPIO 29");
    bcmToWiring.put("GPIO 22", "GPIO 3");
    bcmToWiring.put("GPIO 23", "GPIO 4");
    bcmToWiring.put("GPIO 24", "GPIO 5");
    bcmToWiring.put("GPIO 25", "GPIO 6");
    bcmToWiring.put("GPIO 26", "GPIO 24");
    bcmToWiring.put("GPIO 27", "GPIO 2");

    for (String pin : bcmToWiring.keySet()) {
      String wiring = bcmToWiring.get(pin);
      wiringToBcm.put(wiring, pin);
    }
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

  protected String boardType = null;

  /**
   * default bus current bus of raspi service
   */
  protected String bus = "1";

  protected transient GpioController gpio;

  /**
   * for attached devices
   */
  protected Map<String, I2CDeviceMap> i2cDevices = new HashMap<String, I2CDeviceMap>();

  protected Map<Integer, Set<String>> validI2CAddresses = new HashMap<>();

  protected String wrongPlatformError = null;

  public RasPi(String n, String id) {
    super(n, id);
  }

  @Override
  public void attach(Attachable service) throws Exception {
    if (I2CControl.class.isAssignableFrom(service.getClass())) {
      attachI2CControl((I2CControl) service);
      return;
    }
  }

  @Override
  public void attachI2CControl(I2CControl control) {

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
        I2CBus i2cBus = I2CFactory.getInstance(bus);
        I2CDevice device = i2cBus.getDevice(address);
        devicedata.serviceName = serviceName;
        devicedata.bus = i2cBus;
        devicedata.device = device;
        devicedata.deviceHandle = -1;
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
  public void detach(Attachable service) {
    if (I2CControl.class.isAssignableFrom(service.getClass())) {
      detachI2CControl((I2CControl) service);
      return;
    }
  }

  @Override
  public void detachI2CControl(I2CControl control) {
    // This method should delete the i2c device entry from the list of
    // I2CDevices
    // The order of the detach is important because the higher level service may
    // want to execute something that
    // needs this service to still be availabe
    String key = String.format("%d.%d", Integer.parseInt(control.getBus()), Integer.decode(control.getAddress()));
    if (i2cDevices.containsKey(key)) {
      i2cDevices.remove(key);
      control.detachI2CController(this);
    }
  }

  @Override
  @Deprecated /* use disablePin(String) */
  public void disablePin(int address) {
    error("disablePin(int) not supported use disablePin(String)");
  }

  @Override
  public void disablePin(String pin) {
    if (!pinIndex.containsKey(pin)) {
      error("Pin %s not found", pin);
      return;
    }
    PinDefinition pinDef = pinIndex.get(pin);
    pinDef.setEnabled(false);
    getGPIO(pin).removeListener(this);
    invoke("publishPinDefinition", pinDef);
  }

  @Override
  @Deprecated /* use enablePin(String pin) */
  public void enablePin(int address) {
    error("enablePin(int address) not supoprted use enablePin(String pin)");
  }

  @Override
  @Deprecated /* use enablePin(String, int) */
  public void enablePin(int address, int rate) {
    error("use enablePin(String, int)");
  }

  @Override
  public void enablePin(String pin) {
    if (!pinIndex.containsKey(pin)) {
      error("Pin %s not found", pin);
      return;
    }
    RasPiConfig c = (RasPiConfig) config;
    enablePin(pin, c.pollRateHz);
  }

  @Override
  public void enablePin(String pin, int rate) {
    if (!pinIndex.containsKey(pin)) {
      error("Pin %s not found", pin);
      return;
    }

    PinDefinition pinDef = pinIndex.get(pin);
    pinMode(pin, "INPUT");
    getGPIO(pin).addListener(this);
    pinDef.setEnabled(true);
    invoke("publishPinDefinition", pinDef); // broadcast pin change
  }

  // - add more pin mappings if desired ...
  @Override
  public Integer getAddress(String pin) {
    return Integer.parseInt(pin);
  }

  @Override /* services attached - not i2c devices */
  public Set<String> getAttached() {
    Set<String> ret = new TreeSet<>();
    for (I2CDeviceMap i2c : i2cDevices.values()) {
      ret.add(CodecUtils.getFullName(i2c.serviceName));
    }
    return ret;
  }

  @Override
  public BoardInfo getBoardInfo() {

    BoardInfo boardInfo = new BoardInfo();

    try {

      // Get the board revision
      String revision = SystemInfo.getRevision();
      log.info("Board Revision: " + revision);

      // Get the board type
      boardInfo.boardTypeName = SystemInfo.getModelName();
      log.info("Board Model: " + boardInfo.boardTypeName);

      // Get the board's memory information
      log.info("Memory Info: " + SystemInfo.getMemoryTotal());

      // Get the board's operating system info
      log.info("OS Name: " + SystemInfo.getOsName());

    } catch (Exception e) {
      error(e);
    }

    return boardInfo;
  }

  @Override
  public List<BoardType> getBoardTypes() {
    // TODO Auto-generated method stub
    // FIXME - this need work
    return null;
  }

  /**
   * Gets the multipurpose implementation of a pin, if it doesn't currently
   * exists, it will provision it.
   * 
   * @param pin
   * @return
   */
  private GpioPinDigitalMultipurpose getGPIO(String pin) {
    log.info("getGPIO {}", pin);
    if (!pinIndex.containsKey(pin)) {
      error("Pin %s not found", pin);
      return null;
    }

    PinDefinition pindef = getPin(pin);
    if (pindef == null) {
      error("No pin definition exists for %s", pin);
      return null;
    }

    GpioPinDigitalMultipurpose gpioPin = (GpioPinDigitalMultipurpose) pindef.getPinImpl();
    if (gpioPin == null) {
      log.info("provisioning gpio {}", pin);
      gpioPin = gpio.provisionDigitalMultipurposePin(RaspiPin.getPinByName(bcmToWiring.get(pin)), PinMode.DIGITAL_OUTPUT);
      pindef.setPinImpl(gpioPin);
    }

    return gpioPin;
  }

  @Override
  public List<PinDefinition> getPinList() {
    List<PinDefinition> pinList = new ArrayList<>();

    if (!pinIndex.isEmpty()) {
      pinList.addAll(pinIndex.values());
      return pinList;
    }

    for (Pin wiringPin : RaspiPin.allPins()) {

      // RaspiPin.allPins() RETURNS WIRING NUMBERS !!!!

      // if (wiringPin.getName().equals("GPIO 2") ||
      // wiringPin.getName().equals("GPIO 3") ||
      // wiringPin.getName().equals("GPIO 8") ||
      // wiringPin.getName().equals("GPIO 9")) {
      // log.info("filtering out pin {} from gpio provisioning", wiringPin);
      // continue;
      // }

      String wPinName = wiringPin.getName();

      if (!wiringToBcm.containsKey(wPinName)) {
        log.info("skipping wiring pin {} - no gpio definition", wPinName);
        continue;
      }

      String bcmPinName = wiringToBcm.get(wPinName);

      PinDefinition pindef = new PinDefinition();
      // set to output for starting
      pindef.setMode("OUTPUT");
      pindef.setPinName(bcmPinName);
      EnumSet<PinMode> modes = wiringPin.getSupportedPinModes();

      pindef.setDigital(modes.contains(PinMode.DIGITAL_OUTPUT));
      pindef.setAnalog(modes.contains(PinMode.ANALOG_OUTPUT));
      pindef.setPwm(modes.contains(PinMode.PWM_OUTPUT));

      // FIXME - remove this, do not support address only pin
      String lastPart = bcmPinName.trim().split(" ")[1];
      pindef.setAddress(Integer.parseInt(lastPart));

      pinIndex.put(bcmPinName, pindef);
      pinList.add(pindef);
    }

    return pinList;
  }

  @Override
  public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
    // display pin state on console
    log.info(" --> GPIO PIN STATE CHANGE: {} = {}", event.getPin(), event.getState());
    PinDefinition pindef = pinIndex.get(wiringToBcm.get(event.getPin().getName()));
    if (pindef == null) {
      log.error("pindef is null for pin {}", event.getPin().getName());
    } else {
      pindef.setValue(event.getState().getValue());
      invoke("publishPinDefinition", pindef);
    }

  }

  @Override // FIXME - I2CControl has bus why is it supplied here as a
            // parameter, t
            // ANSWER: here are two busses on the Raspi. Normally we only use 1,
            // bus 0 is used by the SD card
  public int i2cRead(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {
    int bytesRead = 0;
    String key = String.format("%d.%d", busAddress, deviceAddress);
    I2CDeviceMap devicedata = i2cDevices.get(key);
    if (devicedata == null) {
      createI2cDevice(busAddress, deviceAddress, control.getName());
      devicedata = i2cDevices.get(key);
    }

    try {
      bytesRead = devicedata.device.read(buffer, 0, buffer.length);
    } catch (IOException e) {
      error("i2c could not read");
      log.error("i2cRead threw", e);
    }
    return bytesRead;
  }

  @Override
  public void i2cWrite(I2CControl control, int busAddress, int deviceAddress, byte[] buffer, int size) {
    String key = String.format("%d.%d", busAddress, deviceAddress);

    if (buffer == null || buffer.length == 0) {
      log.warn("buffer 0 not writing to i2c bus");
      return;
    }

    I2CDeviceMap devicedata = i2cDevices.get(key);
    if (devicedata == null) {
      createI2cDevice(busAddress, deviceAddress, control.getName());
      devicedata = i2cDevices.get(key);
    }

    try {
      devicedata.device.write(buffer, 0, size);
    } catch (IOException e) {
      log.error("i2cWrite threw input {} {} {} {}", busAddress, deviceAddress, buffer, size, e);
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

    try {
      devicedata.device.read(writeBuffer, 0, writeBuffer.length, readBuffer, 0, readBuffer.length);
    } catch (IOException e) {
      error(e);
    }
    return readBuffer.length;
  }

  // shouldn't the pin mode be a string?
  // shouldn't the pin be a string like "GPIO17"
  /**
   * Sets the pin mode to Input or Output
   * 
   * @param pin
   * @param mode
   *          INPUT = 0x0. Output = 0x1.
   */
  public void pinMode(String pin, String mode) {
    log.info("pinMode {}, mode {}", pin, mode);

    if (mode == null) {
      error("Pin mode cannot be null");
      return;
    }

    mode = mode.trim().toUpperCase();

    if (!pinIndex.containsKey(pin)) {
      error("Pin %s not found", pin);
      return;
    }

    PinDefinition pinDef = pinIndex.get(pin);
    // this will provision the pin if it is not already provisioned
    GpioPinDigitalMultipurpose gpio = getGPIO(pin);
    if (mode.equals("INPUT")) {
      pinDef.setMode("INPUT");
      gpio.setMode(PinMode.DIGITAL_INPUT);
    } else if (mode.equals("OUTPUT")) {
      pinDef.setMode("OUTPUT");
      gpio.setMode(PinMode.DIGITAL_OUTPUT);
    } else {
      error("mode %s is not valid", mode);
    }
    log.info("pinDef {}", pinDef);
    invoke("publishPinDefinition", pinDef);
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

  public void read() {
    log.debug("read task invoked");
    List<PinData> pinArray = new ArrayList<>();
    // load pin array
    for (String pin : pinIndex.keySet()) {
      PinDefinition pindef = pinIndex.get(pin);
      if (pindef.isEnabled()) {
        log.info("pin {} enabled {}", pin, pindef.isEnabled());
        int value = read(pin);
        pindef.setValue(value);
        PinData pd = new PinData(pin, value);
        log.info("pin data {}", pd);
        pinArray.add(pd);
      }
    }

    if (pinArray.size() > 0) {
      PinData[] array = pinArray.toArray(new PinData[0]);
      invoke("publishPinArray", new Object[] { array });
    }
  }

  @Override
  public int read(String pin) {

    if (!pinIndex.containsKey(pin)) {
      error("Pin %s not found", pin);
      return -1;
    }
    PinDefinition pindef = pinIndex.get(pin);
    GpioPinDigitalMultipurpose gpioPin = getGPIO(pin);
    if (!gpioPin.isMode(PinMode.DIGITAL_INPUT)) {
      pinMode(pin, "INPUT");
    }
    if (gpioPin.isLow()) {
      pindef.setValue(0);
      return 0;
    } else {
      pindef.setValue(1);
      return 1;
    }
  }

  @Override
  public void reset() {
    // TODO Auto-generated method stub
    // reset pins/i2c devices/gpio pins
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

      if (!validI2CAddresses.containsKey(busNumber)) {
        validI2CAddresses.put(busNumber, new HashSet<>());
      }

      Set<String> addresses = validI2CAddresses.get(busNumber);

      for (int i = 1; i < 128; i++) {
        String hex = Integer.toHexString(i);
        try {
          I2CDevice device = bus.getDevice(i);
          device.read();
          if (!addresses.contains(hex)) {
            addresses.add(hex);
            info("found new i2c device %s", hex);
          }
        } catch (Exception ignore) {
          if (addresses.contains(hex)) {
            info("removing i2c device %s", hex);
            addresses.remove(hex);
          }
        }
      }

      log.debug("scanning bus {} found: ---", busNumber);
      for (String a : addresses) {
        log.debug("address: " + a);
      }
      log.debug("----------");

    } catch (Exception e) {
      error("cannot access i2c bus %d", busNumber);
      log.error("scan threw", e);
    }

    broadcastState();
  }

  // FIXME - return array
  /**
   * Starts a scan of the I2C specified and returns a list of addresses that
   * responded. This command send a wread and write request with do data to each
   * address. Deviced on the address should respond with an ACK flag as part of
   * the protocol. Each responding device address is added to a list that is
   * returned when the scan is complete.
   * 
   * @param busAddress
   * @return List of devices.
   */
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
      error(e);
    }

    Integer[] ret = list.toArray(new Integer[list.size()]);
    return ret;
  }

  @Override
  public void startService() {
    super.startService();
    try {

      Platform platform = Platform.getLocalInstance();
      log.info("platform is {}", platform);
      log.info("architecture is {}", platform.getArch());

      boardType = SystemInfo.getBoardType().toString();
      gpio = GpioFactory.getInstance();
      log.info("Executing on Raspberry PI");
      getPinList();
      // FIXME - uncomment this
      // log.info("Initiating i2c");
      // I2CFactory.getInstance(Integer.parseInt(bus));
      // log.info("i2c initiated on bus {}", bus);
      // addTask(1000, "scan");
      //
      // log.info("read task initialized");
      // addTask(1000, "read");

      // TODO - config which starts all pins in input or output mode

    } catch (IOException e) {
      log.error("i2c initiation failed", e);
    } catch (Exception e) {
      // an error in the constructor won't get broadcast - so we need Runtime to
      // do it
      Runtime.getInstance().error("raspi service requires arm %s is not arm - %s", getName(), e.getMessage());
      log.error("RasPi init failed", e);
      wrongPlatformError = "The RasPi service requires raspberry pi hardware";
      broadcastState();
    }

  }

  // FIXME - remove
  public void test() {
    // Create GPIO controller instance
    GpioController gpio = GpioFactory.getInstance();

    GpioPinDigitalMultipurpose pin = gpio.provisionDigitalMultipurposePin(RaspiPin.GPIO_00, PinMode.DIGITAL_OUTPUT);

    // Set the pin mode to output
    pin.setMode(PinMode.DIGITAL_OUTPUT);

    // Write a value of 1 (HIGH) to GPIO pin 0
    pin.high();

    // Delay for 2 seconds
    sleep(2000);

    // Write a value of 0 (LOW) to GPIO pin 0
    pin.low();

    // Delay for 2 seconds
    sleep(2000);

    // Set the pin mode to input
    pin.setMode(PinMode.DIGITAL_INPUT);

    // Add a listener to monitor pin state changes
    pin.addListener((GpioPinListenerDigital) (GpioPinDigitalStateChangeEvent event) -> {
      log.info("Pin state changed to: " + event.getState());
    });

    // Shutdown GPIO controller and release resources
    // gpio.shutdown();
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
      error(e);
    }
  }

  @Override
  public void write(String pin, int value) {
    log.info("write {} {}", pin, value);
    if (!pinIndex.containsKey(pin)) {
      error("Pin %s not found", pin);
      return;
    }

    PinDefinition pinDef = pinIndex.get(pin);
    pinMode(pin, "OUTPUT");

    GpioPinDigitalMultipurpose gpio = getGPIO(pin);
    gpio.setState(value == 0 ? PinState.LOW : PinState.HIGH);
    pinDef.setValue(value);

    invoke("publishPinDefinition", pinDef);
  }

  @Override
  public void pinMode(int address, String mode) {
    pinMode(String.format("%d", address), mode);
  }

  @Override
  public void write(int address, int value) {
    write(String.format("%d", address), value);
  }

}
