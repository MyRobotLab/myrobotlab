package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinArrayListener;
import org.myrobotlab.service.interfaces.PinDefinition;
import org.myrobotlab.service.interfaces.PinListener;
import org.slf4j.Logger;

/**
 * AdaFruit Ina219 Shield Controller Service
 * 
 * @author Mats
 * 
 *         https://learn.adafruit.com/adafruit-4-channel-adc-breakouts
 *         /programming The code here is to a large extent based on the Adafruit
 *         C++ libraries here: https://github.com/adafruit/Adafruit_ADS1X15
 * 
 *         Next follows the license agreement from Adafruit that this service is
 *         based on. It has been converted fron C++ to Java.
 * 
 *         Software License Agreement (BSD License)
 * 
 *         Copyright (c) 2012, Adafruit Industries All rights reserved.
 * 
 *         Redistribution and use in source and binary forms, with or without
 *         modification, are permitted provided that the following conditions
 *         are met: 1. Redistributions of source code must retain the above
 *         copyright notice, this list of conditions and the following
 *         disclaimer. 2. Redistributions in binary form must reproduce the
 *         above copyright notice, this list of conditions and the following
 *         disclaimer in the documentation and/or other materials provided with
 *         the distribution. 3. Neither the name of the copyright holders nor
 *         the names of its contributors may be used to endorse or promote
 *         products derived from this software without specific prior written
 *         permission.
 * 
 *         THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ''AS IS'' AND ANY
 *         EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *         IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *         PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE
 *         LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *         CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *         SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *         BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *         WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *         OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 *         EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class Ads1115 extends Service implements I2CControl, PinArrayControl {
  /**
   * Publisher - Publishes pin data at a regular interval
   * 
   */
  public class Publisher extends Thread {

    public Publisher(String name) {
      super(String.format("%s.publisher", name));
    }

    @Override
    public void run() {

      log.info(String.format("New publisher instance started at a sample frequency of %s Hz", sampleFreq));
      long sleepTime = 1000 / (long) sampleFreq;
      isPublishing = true;
      try {
        while (isPublishing) {
          Thread.sleep(sleepTime);
          publishPinData();
        }

      } catch (Exception e) {
        if (e instanceof InterruptedException) {
          log.info("Shutting down Publisher");
        } else {
          isPublishing = false;
          log.error("publisher threw", e);
        }
      }
    }

    void publishPinData() {

      PinData[] pinArray = new PinData[pinDataCnt];

      for (int i = 0; i < pinArray.length; ++i) {
        PinData pinData = new PinData(i, read(i));
        pinArray[i] = pinData;
        int address = pinData.address;

        // handle individual pins
        if (pinListeners.containsKey(address)) {
          List<PinListener> list = pinListeners.get(address);
          for (int j = 0; j < list.size(); ++j) {
            PinListener pinListner = list.get(j);
            if (pinListner.isLocal()) {
              pinListner.onPin(pinData);
            } else {
              invoke("publishPin", pinData);
            }
          }
        }
      }

      // publish array
      invoke("publishPinArray", new Object[] { pinArray });
    }
  }

  // Publisher
  boolean isPublishing = false;
  transient Publisher publisher = null;
  int pinDataCnt = 4;
  //

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Ads1115.class);
  public transient I2CController controller;

  /*
   * public static final byte INA219_SHUNTVOLTAGE = 0x01; public static final
   * byte INA219_BUSVOLTAGE = 0x02;
   */
  public List<String> deviceAddressList = Arrays.asList("0x48", "0x49", "0x4A", "0x4B");

  public String deviceAddress = "0x48";

  public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7");
  public String deviceBus = "1";

  public int adc0 = 0;
  public int adc1 = 0;
  public int adc2 = 0;
  public int adc3 = 0;

  public double voltage0 = 0;
  public double voltage1 = 0;
  public double voltage2 = 0;
  public double voltage3 = 0;

  private int m_conversionDelay;
  private int m_bitShift;
  private int m_gain = 0;
  private double m_fs = 0;

  public List<String> controllers;
  public String controllerName;
  public boolean isAttached = false;

  /*
   * =========================================================================
   * CONVERSION DELAY (in mS)
   * -----------------------------------------------------------------------
   */
  static byte ADS1015_CONVERSIONDELAY = 1;
  static byte ADS1115_CONVERSIONDELAY = 8;
  /*
   * =========================================================================
   */

  /*
   * =========================================================================
   * POINTER REGISTER
   * -----------------------------------------------------------------------
   */
  static byte ADS1015_REG_POINTER_MASK = 0x03;
  static byte ADS1015_REG_POINTER_CONVERT = 0x00;
  static byte ADS1015_REG_POINTER_CONFIG = 0x01;
  static byte ADS1015_REG_POINTER_LOWTHRESH = 0x02;
  static byte ADS1015_REG_POINTER_HITHRESH = 0x03;
  /*
   * =========================================================================
   */

  /*
   * =========================================================================
   * CONFIG REGISTER
   * -----------------------------------------------------------------------
   */
  static int ADS1015_REG_CONFIG_OS_MASK = 0x8000;
  static int ADS1015_REG_CONFIG_OS_SINGLE = 0x8000; // Write:
                                                    // Set
                                                    // to
                                                    // start
                                                    // a
  // single-conversion
  static int ADS1015_REG_CONFIG_OS_BUSY = 0x0000; // Read:
                                                  // Bit
                                                  // =
                                                  // 0
                                                  // when
  // conversion is in progress
  static int ADS1015_REG_CONFIG_OS_NOTBUSY = 0x8000; // Read:
                                                     // Bit
                                                     // =
                                                     // 1
                                                     // when
  // device is not
  // performing a
  // conversion

  static int ADS1015_REG_CONFIG_MUX_MASK = 0x7000;
  static int ADS1015_REG_CONFIG_MUX_DIFF_0_1 = 0x0000; // Differential
                                                       // P
                                                       // =
  // AIN0,
  // N = AIN1
  // (default;
  static int ADS1015_REG_CONFIG_MUX_DIFF_0_3 = 0x1000; // Differential
                                                       // P
                                                       // =
  // AIN0,
  // N = AIN3
  static int ADS1015_REG_CONFIG_MUX_DIFF_1_3 = 0x2000; // Differential
                                                       // P
                                                       // =
  // AIN1,
  // N = AIN3
  static int ADS1015_REG_CONFIG_MUX_DIFF_2_3 = 0x3000; // Differential
                                                       // P
                                                       // =
  // AIN2,
  // N = AIN3
  static int ADS1015_REG_CONFIG_MUX_SINGLE_0 = 0x4000; // Single-ended
                                                       // AIN0
  static int ADS1015_REG_CONFIG_MUX_SINGLE_1 = 0x5000; // Single-ended
                                                       // AIN1
  static int ADS1015_REG_CONFIG_MUX_SINGLE_2 = 0x6000; // Single-ended
                                                       // AIN2
  static int ADS1015_REG_CONFIG_MUX_SINGLE_3 = 0x7000; // Single-ended
                                                       // AIN3

  static int ADS1015_REG_CONFIG_PGA_MASK = 0x0E00;
  static final int ADS1015_REG_CONFIG_PGA_6_144V = 0x0000; // +/-6.144V
                                                           // range
  // = Gain
  // 2/3
  static final int ADS1015_REG_CONFIG_PGA_4_096V = 0x0200; // +/-4.096V
                                                           // range
  // = Gain 1
  static final int ADS1015_REG_CONFIG_PGA_2_048V = 0x0400; // +/-2.048V
                                                           // range
  // = Gain 2
  // (default)
  static final int ADS1015_REG_CONFIG_PGA_1_024V = 0x0600; // +/-1.024V
                                                           // range
  // = Gain 4
  static final int ADS1015_REG_CONFIG_PGA_0_512V = 0x0800; // +/-0.512V
                                                           // range
  // = Gain 8
  static final int ADS1015_REG_CONFIG_PGA_0_256V = 0x0A00; // +/-0.256V
                                                           // range
  // = Gain
  // 16

  static int ADS1015_REG_CONFIG_MODE_MASK = 0x0100;
  static int ADS1015_REG_CONFIG_MODE_CONTIN = 0x0000; // Continuous
                                                      // conversion
  // mode
  static int ADS1015_REG_CONFIG_MODE_SINGLE = 0x0100; // Power-down
  // single-shot
  // mode (default)

  static int ADS1015_REG_CONFIG_DR_MASK = 0x00E0;
  static int ADS1015_REG_CONFIG_DR_128SPS = 0x0000; // 128
                                                    // samples
                                                    // per
                                                    // second
  static int ADS1015_REG_CONFIG_DR_250SPS = 0x0020; // 250
                                                    // samples
                                                    // per
                                                    // second
  static int ADS1015_REG_CONFIG_DR_490SPS = 0x0040; // 490
                                                    // samples
                                                    // per
                                                    // second
  static int ADS1015_REG_CONFIG_DR_920SPS = 0x0060; // 920
                                                    // samples
                                                    // per
                                                    // second
  static int ADS1015_REG_CONFIG_DR_1600SPS = 0x0080; // 1600
                                                     // samples
                                                     // per
  // second
  // (default)
  static int ADS1015_REG_CONFIG_DR_2400SPS = 0x00A0; // 2400
                                                     // samples
                                                     // per
  // second
  static int ADS1015_REG_CONFIG_DR_3300SPS = 0x00C0; // 3300
                                                     // samples
                                                     // per
  // second

  static int ADS1015_REG_CONFIG_CMODE_MASK = 0x0010;
  static int ADS1015_REG_CONFIG_CMODE_TRAD = 0x0000; // Traditional
                                                     // comparator
  // with hysteresis
  // (default)
  static int ADS1015_REG_CONFIG_CMODE_WINDOW = 0x0010; // Window
                                                       // comparator

  static int ADS1015_REG_CONFIG_CPOL_MASK = 0x0008;
  static int ADS1015_REG_CONFIG_CPOL_ACTVLOW = 0x0000; // ALERT/RDY
                                                       // pin
                                                       // is
                                                       // low
  // when active
  // (default)
  static int ADS1015_REG_CONFIG_CPOL_ACTVHI = 0x0008; // ALERT/RDY
                                                      // pin
                                                      // is
                                                      // high
  // when active

  static int ADS1015_REG_CONFIG_CLAT_MASK = 0x0004; // Determines
                                                    // if
                                                    // ALERT/RDY
  // pin latches once
  // asserted
  static int ADS1015_REG_CONFIG_CLAT_NONLAT = 0x0000; // Non-latching
  // comparator
  // (default)
  static int ADS1015_REG_CONFIG_CLAT_LATCH = 0x0004; // Latching
                                                     // comparator

  static int ADS1015_REG_CONFIG_CQUE_MASK = 0x0003;
  static int ADS1015_REG_CONFIG_CQUE_1CONV = 0x0000; // Assert
                                                     // ALERT/RDY
                                                     // after
  // one conversions
  static int ADS1015_REG_CONFIG_CQUE_2CONV = 0x0001; // Assert
                                                     // ALERT/RDY
                                                     // after
  // two conversions
  static int ADS1015_REG_CONFIG_CQUE_4CONV = 0x0002; // Assert
                                                     // ALERT/RDY
                                                     // after
  // four conversions
  static int ADS1015_REG_CONFIG_CQUE_NONE = 0x0003; // Disable
                                                    // the
                                                    // comparator
  // and put ALERT/RDY in
  // high
  // state (default)

  static int GAIN_TWOTHIRDS = ADS1015_REG_CONFIG_PGA_6_144V;
  static int GAIN_ONE = ADS1015_REG_CONFIG_PGA_4_096V;
  static int GAIN_TWO = ADS1015_REG_CONFIG_PGA_2_048V;
  static int GAIN_FOUR = ADS1015_REG_CONFIG_PGA_1_024V;
  static int GAIN_EIGHT = ADS1015_REG_CONFIG_PGA_0_512V;
  static int GAIN_SIXTEEN = ADS1015_REG_CONFIG_PGA_0_256V;

  /*
   * =========================================================================
   */

  /**
   * pin named map of all the pins on the board
   */
  Map<String, PinDefinition> pinMap = null;
  /**
   * the definitive sequence of pins - "true address"
   */
  Map<Integer, PinDefinition> pinIndex = null;

  transient Map<String, PinArrayListener> pinArrayListeners = new HashMap<String, PinArrayListener>();

  /**
   * map of pin listeners
   */
  transient Map<Integer, List<PinListener>> pinListeners = new HashMap<Integer, List<PinListener>>();

  /**
   * the map of pins which the pin listeners are listening too - if the set is
   * null they are listening to "any" published pin
   */
  Map<String, Set<Integer>> pinSets = new HashMap<String, Set<Integer>>();

  double sampleFreq = 1; // Set
                         // default // hZ.

  public static void main(String[] args) {
    LoggingFactory.getInstance().configure();
    LoggingFactory.getInstance().setLevel(Level.INFO);

    try {
      Ads1115 ads1115 = (Ads1115) Runtime.start("Ads1115", "Ads1115");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Ads1115(String n) {
    super(n);
    createPinList();
    refreshControllers();
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
    init_ADS1115();
  }

  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();

  }

  public List<String> refreshControllers() {
    controllers = Runtime.getServiceNamesFromInterface(I2CController.class);
    return controllers;
  }

  public I2CController getController() {
    return controller;
  }

  public String getControllerName() {

    String controlerName = null;

    if (controller != null) {
      controlerName = controller.getName();
    }

    return controlerName;
  }

  @Override
  public void setDeviceBus(String deviceBus) {
    if (isAttached) {
      log.error(String.format("Already attached to %s, use detach(%s) first", this.controllerName));
      return;
    }
    this.deviceBus = deviceBus;
    broadcastState();
  }

  @Override
  public void setDeviceAddress(String deviceAddress) {
    if (isAttached) {
      log.error(String.format("Already attached to %s, use detach(%s) first", this.controllerName));
      return;
    }
    this.deviceAddress = deviceAddress;
    broadcastState();
  }

  /**
   * This method reads all four A/D pins
   */
  public void refresh() {

    adc0 = readADC_SingleEnded(0);
    adc1 = readADC_SingleEnded(1);
    adc2 = readADC_SingleEnded(2);
    adc3 = readADC_SingleEnded(3);
    convertToVoltage();
    broadcastState();
  }

  void convertToVoltage() {
    int max = 256 * 128;
    voltage0 = adc0 * m_fs / max;
    voltage1 = adc1 * m_fs / max;
    voltage2 = adc2 * m_fs / max;
    voltage3 = adc3 * m_fs / max;

  }

  /**
   * This method initiates the ADS1015
   */
  public void init_ADS1015() {
    m_conversionDelay = ADS1015_CONVERSIONDELAY;
    m_bitShift = 4;
    m_gain = GAIN_TWOTHIRDS; /* +/- 6.144V range (limited to VDD +0.3V max!) */
    this.m_fs = 6.144;
  }

  /**
   * This method initiates the ADS1115
   */
  public void init_ADS1115() {
    m_conversionDelay = ADS1115_CONVERSIONDELAY;
    m_bitShift = 0;
    m_gain = GAIN_TWOTHIRDS; /* +/- 6.144V range (limited to VDD +0.3V max!) */
    m_fs = 6.144;
  }

  /*
   * ************************************************************************
   *
   * Sets the gain and input voltage range
   *
   * /
   **************************************************************************/
  public void setGain(int gain) {
    m_gain = gain;
    switch (gain) {
      case ADS1015_REG_CONFIG_PGA_6_144V:
        m_fs = 6.144;
        break;
      case ADS1015_REG_CONFIG_PGA_4_096V:
        m_fs = 4.096;
        break;
      case ADS1015_REG_CONFIG_PGA_2_048V:
        m_fs = 2.096;
        break;
      case ADS1015_REG_CONFIG_PGA_1_024V:
        m_fs = 1.024;
        break;
      case ADS1015_REG_CONFIG_PGA_0_512V:
        m_fs = 0.512;
        break;
      case ADS1015_REG_CONFIG_PGA_0_256V:
        m_fs = 0.256;
        break;
    }
  }

  /*
   * ************************************************************************
   * 
   * Gets a gain and input voltage range
   * 
   * /
   **************************************************************************/
  public int getGain() {
    return m_gain;
  }

  /*
   * This method reads and returns the Voltage in milliVolts
   */
  public int readADC_SingleEnded(int channel) {
    if (channel > 3) {
      return 0;
    }
    // Start with default values
    int config = ADS1015_REG_CONFIG_CQUE_NONE | // Disable the comparator
    // (default val)
        ADS1015_REG_CONFIG_CLAT_NONLAT | // Non-latching (default val)
        ADS1015_REG_CONFIG_CPOL_ACTVLOW | // Alert/Rdy active low
        // (default val)
        ADS1015_REG_CONFIG_CMODE_TRAD | // Traditional comparator
        // (default val)
        ADS1015_REG_CONFIG_DR_1600SPS | // 1600 samples per second
        // (default)
        ADS1015_REG_CONFIG_MODE_SINGLE; // Single-shot mode (default)

    // Set PGA/voltage range
    config |= m_gain;

    switch (channel) {
      case 0:
        config |= ADS1015_REG_CONFIG_MUX_SINGLE_0;
        break;
      case 1:
        config |= ADS1015_REG_CONFIG_MUX_SINGLE_1;
        break;
      case 2:
        config |= ADS1015_REG_CONFIG_MUX_SINGLE_2;
        break;
      case 3:
        config |= ADS1015_REG_CONFIG_MUX_SINGLE_3;
        break;
    }
    // Set 'start single-conversion' bit
    config |= ADS1015_REG_CONFIG_OS_SINGLE;

    // Write config register to the ADC
    writeRegister(ADS1015_REG_POINTER_CONFIG, config);

    // Wait for the conversion to complete
    sleep(m_conversionDelay);

    // Read the conversion results
    // Shift 12-bit results right 4 bits for the ADS1015
    return readRegister(ADS1015_REG_POINTER_CONVERT) >> m_bitShift;
  }

  /*
   * /* !
   * 
   * @brief Reads the conversion results, measuring the voltage difference
   * between the P (AIN0) and N (AIN1) input. Generates a signed value since the
   * difference can be either positive or negative.
   */
  /* */
  public int readADC_Differential_0_1() {
    // Start with default values
    int config = ADS1015_REG_CONFIG_CQUE_NONE | // Disable the comparator
    // (default val)
        ADS1015_REG_CONFIG_CLAT_NONLAT | // Non-latching (default val)
        ADS1015_REG_CONFIG_CPOL_ACTVLOW | // Alert/Rdy active low
        // (default val)
        ADS1015_REG_CONFIG_CMODE_TRAD | // Traditional comparator
        // (default val)
        ADS1015_REG_CONFIG_DR_1600SPS | // 1600 samples per second
        // (default)
        ADS1015_REG_CONFIG_MODE_SINGLE; // Single-shot mode (default)

    // Set PGA/voltage range
    config |= m_gain;

    // Set channels
    config |= ADS1015_REG_CONFIG_MUX_DIFF_0_1; // AIN0 = P, AIN1 = N

    // Set 'start single-conversion' bit
    config |= ADS1015_REG_CONFIG_OS_SINGLE;

    // Write config register to the ADC
    writeRegister(ADS1015_REG_POINTER_CONFIG, config);

    // Wait for the conversion to complete
    sleep(m_conversionDelay);

    // Read the conversion results
    int res = readRegister(ADS1015_REG_POINTER_CONVERT);
    if (m_bitShift == 0) {
      return res;
    } else {
      // Shift 12-bit results right 4 bits for the ADS1015,
      // making sure we keep the sign bit intact
      return res / 16;
    }
  }

  /*
   * !
   * 
   * @brief Reads the conversion results, measuring the voltage difference
   * between the P (AIN2) and N (AIN3) input. Generates a signed value since the
   * difference can be either positive or negative.
   */
  public int readADC_Differential_2_3() {
    // Start with default values
    int config = ADS1015_REG_CONFIG_CQUE_NONE | // Disable the comparator
    // (default val)
        ADS1015_REG_CONFIG_CLAT_NONLAT | // Non-latching (default val)
        ADS1015_REG_CONFIG_CPOL_ACTVLOW | // Alert/Rdy active low
        // (default val)
        ADS1015_REG_CONFIG_CMODE_TRAD | // Traditional comparator
        // (default val)
        ADS1015_REG_CONFIG_DR_1600SPS | // 1600 samples per second
        // (default)
        ADS1015_REG_CONFIG_MODE_SINGLE; // Single-shot mode (default)

    // Set PGA/voltage range
    config |= m_gain;

    // Set channels
    config |= ADS1015_REG_CONFIG_MUX_DIFF_2_3; // AIN2 = P, AIN3 = N

    // Set 'start single-conversion' bit
    config |= ADS1015_REG_CONFIG_OS_SINGLE;

    // Write config register to the ADC
    writeRegister(ADS1015_REG_POINTER_CONFIG, config);

    // Wait for the conversion to complete
    sleep(m_conversionDelay);

    // Read the conversion results
    int res = readRegister(ADS1015_REG_POINTER_CONVERT) >> m_bitShift;
    if (m_bitShift == 0) {
      return res;
    } else {
      // Shift 12-bit results right 4 bits for the ADS1015,
      // making sure we keep the sign bit intact
      return res / 16;
    }
  }

  /*
   * !
   * 
   * @brief Sets up the comparator to operate in basic mode, causing the
   * ALERT/RDY pin to assert (go from high to low) when the ADC value exceeds
   * the specified threshold. This will also set the ADC in continuous
   * conversion mode.
   */
  public void startComparator_SingleEnded(int channel, int threshold) {
    // Start with default values
    int config = ADS1015_REG_CONFIG_CQUE_1CONV | // Comparator enabled and
    // asserts on 1 match
        ADS1015_REG_CONFIG_CLAT_LATCH | // Latching mode
        ADS1015_REG_CONFIG_CPOL_ACTVLOW | // Alert/Rdy active low
        // (default val)
        ADS1015_REG_CONFIG_CMODE_TRAD | // Traditional comparator
        // (default val)
        ADS1015_REG_CONFIG_DR_1600SPS | // 1600 samples per second
        // (default)
        ADS1015_REG_CONFIG_MODE_CONTIN | // Continuous conversion mode
        ADS1015_REG_CONFIG_MODE_CONTIN; // Continuous conversion mode

    // Set PGA/voltage range
    config |= m_gain;

    // Set single-ended input channel
    switch (channel) {
      case (0):
        config |= ADS1015_REG_CONFIG_MUX_SINGLE_0;
        break;
      case (1):
        config |= ADS1015_REG_CONFIG_MUX_SINGLE_1;
        break;
      case (2):
        config |= ADS1015_REG_CONFIG_MUX_SINGLE_2;
        break;
      case (3):
        config |= ADS1015_REG_CONFIG_MUX_SINGLE_3;
        break;
    }

    // Set the high threshold register
    // Shift 12-bit results left 4 bits for the ADS1015
    writeRegister(ADS1015_REG_POINTER_HITHRESH, threshold << m_bitShift);

    // Write config register to the ADC
    writeRegister(ADS1015_REG_POINTER_CONFIG, config);
  }

  /*
   * !
   * 
   * @brief In order to clear the comparator, we need to read the conversion
   * results. This function reads the last conversion results without changing
   * the config value.
   */
  public int getLastConversionResults() {
    // Wait for the conversion to complete
    sleep(ADS1115_CONVERSIONDELAY);

    // Read the conversion results
    int res = readRegister(ADS1015_REG_POINTER_CONVERT) >> m_bitShift;
    if (m_bitShift == 0) {
      return res;
    } else {
      // Shift 12-bit results right 4 bits for the ADS1015,
      // making sure we keep the sign bit intact
      return res / 16;
    }
  }

  void i2cWrite(int reg) {
    byte[] writebuffer = { (byte) reg };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length);
  }

  void writeRegister(int reg, int value) {
    byte[] writebuffer = { (byte) reg, (byte) (value >> 8), (byte) (value & 0xff) };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), writebuffer, writebuffer.length);
  }

  int readRegister(int reg) {
    i2cWrite(ADS1015_REG_POINTER_CONVERT);
    byte[] readbuffer = new byte[2];
    controller.i2cRead(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), readbuffer, readbuffer.length);
    return ((int) readbuffer[0]) << 8 | (int) (readbuffer[1] & 0xff);
  }

  /**
   * GOOD DESIGN - this method is the same pretty much for all Services could be
   * a Java 8 default implementation to the interface
   */
  @Override
  public boolean isAttached(String name) {
    return (controller != null && controller.getName().equals(name));
  }

  @Override
  public List<PinDefinition> getPinList() {
    List<PinDefinition> list = new ArrayList<PinDefinition>(pinIndex.values());
    return list;
  }

  @Override
  public int read(Integer address) {
    pinIndex.get(address).setValue(readADC_SingleEnded(address));
    return pinIndex.get(address).getValue();
  }

  @Override
  public int read(String pinName) {
    return read(pinNameToAddress(pinName));
  }

  @Override
  public void pinMode(Integer address, String mode) {
    if (mode != null && mode.equalsIgnoreCase("INPUT")) {
    } else {
      log.error("Ads1115 only supports INPUT mode");
    }

  }

  @Override
  public void write(Integer address, Integer value) {
    log.error("Ads1115 only supports read, not write");

  }

  @Override
  public PinData publishPin(PinData pinData) {
    // caching last value
    pinIndex.get(pinData.address).setValue(pinData.value);
    return pinData;
  }

  /**
   * publish all read pin data in one array at once
   */
  @Override
  public PinData[] publishPinArray(PinData[] pinData) {
    return pinData;
  }

  public void attach(String listener, int pinAddress) {
    attach((PinListener) Runtime.getService(listener), pinAddress);
  }

  @Override
  public void attach(PinListener listener, Integer pinAddress) {
    String name = listener.getName();

    if (listener.isLocal()) {
      List<PinListener> list = null;
      if (pinListeners.containsKey(pinAddress)) {
        list = pinListeners.get(pinAddress);
      } else {
        list = new ArrayList<PinListener>();
      }
      list.add(listener);
      pinListeners.put(pinAddress, list);

    } else {
      // setup for pub sub
      // FIXME - there is an architectual problem here
      // locally it works - but remotely - outbox would need to know
      // specifics of
      // the data its sending
      addListener("publishPin", name, "onPin");
    }

  }

  @Override
  public void attach(PinArrayListener listener) {
    pinArrayListeners.put(listener.getName(), listener);

  }

  @Override
  public void enablePin(Integer address) {
    if (controller == null) {
      error("must be connected to enable pins");
      return;
    }

    log.info(String.format("enablePin %s", address));
    PinDefinition pin = pinIndex.get(address);
    pin.setEnabled(true);
    invoke("publishPinDefinition", pin);

    if (!isPublishing) {
      log.info(String.format("Starting a new publisher instance"));
      publisher = new Publisher(getName());
      publisher.start();
    }
  }

  @Override
  public void disablePin(Integer address) {
    if (controller == null) {
      log.error("Must be connected to disable pins");
      return;
    }
    PinDefinition pin = pinIndex.get(address);
    pin.setEnabled(false);
    invoke("publishPinDefinition", pin);
  }

  @Override
  public void disablePins() {
    for (int i = 0; i < pinDataCnt; i++) {
      disablePin(i);
    }
    if (isPublishing) {
      isPublishing = false;
    }
  }

  public Map<String, PinDefinition> createPinList() {
    pinMap = new HashMap<String, PinDefinition>();
    pinIndex = new HashMap<Integer, PinDefinition>();

    for (int i = 0; i < pinDataCnt; ++i) {
      PinDefinition pindef = new PinDefinition(getName(), i, String.format("A%d", i));  
      pindef.setRx(false);
      pindef.setTx(false);
      pindef.setAnalog(true);
      pindef.setPwm(false);
      pindef.setAddress(i);
      pindef.setMode("INPUT");
      pinMap.put(pindef.getPinName(), pindef);
      pinIndex.put(i, pindef);
    }

    return pinMap;
  }

  public Integer pinNameToAddress(String pinName) {
    if (!pinMap.containsKey(pinName)) {
      error("no pin %s exists", pinName);
      return null;
    }
    return pinMap.get(pinName).getAddress();
  }

  /*
   * Set the sample rate in Hz, I.e the number of polls per second
   * 
   * @return the rate that was set.
   */
  public double setSampleRate(double rate) {
    if (rate < 0) {
      log.error(String.format("setSampleRate. Rate must be > 0. Ignored %s, returning to %s", rate, this.sampleFreq));
      return this.sampleFreq;
    }
    this.sampleFreq = rate;
    return rate;
  }

  /*
   * method to communicate changes in pinmode or state changes
   * 
   * @return the pin definition passed in. (used by invoke.)
   */
  public PinDefinition publishPinDefinition(PinDefinition pinDef) {
    return pinDef;
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Ads1115.class.getCanonicalName());
    meta.addDescription("a higher-precision analog to digital converter 16-bit");
    meta.setLicenseApache();
    meta.addCategory("shield", "sensor", "i2c");
    meta.setSponsor("Mats");
    return meta;
  }

  @Override
  // TODO Implement individula sample rates per pin
  public void enablePin(Integer address, Integer rate) {
    setSampleRate(rate);
    enablePin(address);
  }

  // This section contains all the new attach logic
  @Override
  public void attach(String service) throws Exception {
    attach((Attachable) Runtime.getService(service));
  }

  @Override
  public void attach(Attachable service) throws Exception {

    if (I2CController.class.isAssignableFrom(service.getClass())) {
      attachI2CController((I2CController) service);
      return;
    }
  }

  public void attach(String controllerName, String deviceBus, String deviceAddress) {
    attach((I2CController) Runtime.getService(controllerName), deviceBus, deviceAddress);
  }

  public void attach(I2CController controller, String deviceBus, String deviceAddress) {

    if (isAttached && this.controller != controller) {
      log.error(String.format("Already attached to %s, use detach(%s) first", this.controllerName));
    }

    controllerName = controller.getName();
    log.info(String.format("%s attach %s", getName(), controllerName));

    this.deviceBus = deviceBus;
    this.deviceAddress = deviceAddress;

    attachI2CController(controller);
    isAttached = true;
    broadcastState();
  }

  public void attachI2CController(I2CController controller) {

    if (isAttached(controller))
      return;

    if (this.controllerName != controller.getName()) {
      log.error(String.format("Trying to attached to %s, but already attached to (%s)", controller.getName(), this.controllerName));
      return;
    }

    this.controller = controller;
    isAttached = true;
    controller.attachI2CControl(this);
    log.info(String.format("Attached %s device on bus: %s address %s", controllerName, deviceBus, deviceAddress));
    broadcastState();
  }

  // This section contains all the new detach logic
  // TODO: This default code could be in Attachable
  @Override
  public void detach(String service) {
    detach((Attachable) Runtime.getService(service));
  }

  @Override
  public void detach(Attachable service) {

    if (I2CController.class.isAssignableFrom(service.getClass())) {
      detachI2CController((I2CController) service);
      return;
    }
  }

  @Override
  public void detachI2CController(I2CController controller) {

    if (!isAttached(controller))
      return;

    controller.detachI2CControl(this);
    isAttached = false;
    broadcastState();
  }

  // This section contains all the methods used to query / show all attached
  // methods
  /**
   * Returns all the currently attached services
   */
  @Override
  public Set<String> getAttached() {
    HashSet<String> ret = new HashSet<String>();
    if (controller != null && isAttached) {
      ret.add(controller.getName());
    }
    return ret;
  }

  @Override
  public String getDeviceBus() {
    return this.deviceBus;
  }

  @Override
  public String getDeviceAddress() {
    return this.deviceAddress;
  }

  @Override
  public boolean isAttached(Attachable instance) {
    if (controller != null && controller.getName().equals(instance.getName())) {
      return isAttached;
    }
    ;
    return false;
  }
  
  public PinDefinition getPin(String pinName) {
    if (pinMap.containsKey(pinName)){
      return pinMap.get(pinName);
    }
    return null;
  }
  
  public PinDefinition getPin(Integer address) {
    if (pinIndex.containsKey(address)){
      return pinIndex.get(address);
    }
    return null;
  }
}
