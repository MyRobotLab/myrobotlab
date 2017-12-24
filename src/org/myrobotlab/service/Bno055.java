package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.I2CControl;
import org.myrobotlab.service.interfaces.I2CController;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.PinListener;
import org.slf4j.Logger;

/**
 * 
 * BNO055 - BNO055 sensor contains a MEMS accelerometer, magnetometer and gyroscope in a single chip 
 * with a ARM based processor to digest all the sensor data. Data can be get and use in quaternions, 
 * Euler angles or vertors 
 *
 *https://www.adafruit.com/product/2472
 * 
 */
/**
 * ============================================ I2Cdev device library code is
 * placed under the MIT license Copyright (c) 2012 Jeff Rowberg Permission is
 * hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions: The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of
 * the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE. ===============================================
 */

public class Bno055 extends Service implements I2CControl, PinListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Bno055.class);

  StringBuilder debugRX = new StringBuilder();

  transient I2CController controller;

  public List<String> deviceAddressList = Arrays.asList("0x28", "0x29");
  public String deviceAddress = "0x28";

  public List<String> deviceBusList = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8");
  public String deviceBus = "1";

  public List<String> controllers;
  public String controllerName;
  public boolean isAttached = false;

  public static final int BNO055_ID = 0xA0;

  public enum register {
    //page 0 value
    CHIP_ID           ((byte)0x00, (byte)0, "Chip ID"),
    ACC_ID            ((byte)0x01, (byte)0, "Accelerometer ID"),
    MAG_ID            ((byte)0x02, (byte)0, "Magnetometer ID"),
    GYR_ID            ((byte)0x03, (byte)0, "Gyroscope ID"),
    SW_REV_ID_LSB     ((byte)0x04, (byte)0, "SW Revision ID"),
    SW_REV_ID_MSB     ((byte)0x05, (byte)0, ""),
    BL_REV_ID         ((byte)0x06, (byte)0, "Bootloader Version"),
    PAGE_ID           ((byte)0x07, (byte)0, "Page ID"),
    ACC_DATA_X_LSB    ((byte)0x08, (byte)0, ""),
    ACC_DATA_X_MSB    ((byte)0x09, (byte)0, ""),
    ACC_DATA_Y_LSB    ((byte)0x0A, (byte)0, ""),
    ACC_DATA_Y_MSB    ((byte)0x0B, (byte)0, ""),
    ACC_DATA_Z_LSB    ((byte)0x0C, (byte)0, ""),
    ACC_DATA_Z_MSB    ((byte)0x0D, (byte)0, ""),
    MAG_DATA_X_LSB    ((byte)0x0E, (byte)0, ""),
    MAG_DATA_X_MSB    ((byte)0x0F, (byte)0, ""),
    MAG_DATA_Y_LSB    ((byte)0x10, (byte)0, ""),
    MAG_DATA_Y_MSB    ((byte)0x11, (byte)0, ""),
    MAG_DATA_Z_LSB    ((byte)0x12, (byte)0, ""),
    MAG_DATA_Z_MSB    ((byte)0x13, (byte)0, ""),
    GYR_DATA_X_LSB    ((byte)0x14, (byte)0, ""),
    GYR_DATA_X_MSB    ((byte)0x15, (byte)0, ""),
    GYR_DATA_Y_LSB    ((byte)0x16, (byte)0, ""),
    GYR_DATA_Y_MSB    ((byte)0x17, (byte)0, ""),
    GYR_DATA_Z_LSB    ((byte)0x18, (byte)0, ""),
    GYR_DATA_Z_MSB    ((byte)0x19, (byte)0, ""),
    EUL_HEADING_LSB   ((byte)0x1A, (byte)0, "Euler Heading LSB"),
    EUL_HEADING_MSB   ((byte)0x1B, (byte)0, "Euler Heading MSB"),
    EUL_ROLL_LSB      ((byte)0x1C, (byte)0, "Euler Roll LSB"),
    EUL_ROLL_MSB      ((byte)0x1D, (byte)0, "Euler Roll MSB"),
    EUL_PITCH_LSB     ((byte)0x1E, (byte)0, "Euler Pitch LSB"),
    EUL_PITCH_MSB     ((byte)0x1F, (byte)0, "Euler Pitch MSB"),
    QUA_DATA_W_LSB    ((byte)0x20, (byte)0, ""),
    QUA_DATA_W_MSB    ((byte)0x21, (byte)0, ""),
    QUA_DATA_X_LSB    ((byte)0x22, (byte)0, ""),
    QUA_DATA_X_MSB    ((byte)0x23, (byte)0, ""),
    QUA_DATA_Y_LSB    ((byte)0x24, (byte)0, ""),
    QUA_DATA_Y_MSB    ((byte)0x25, (byte)0, ""),
    QUA_DATA_Z_LSB    ((byte)0x26, (byte)0, ""),
    QUA_DATA_Z_MSB    ((byte)0x27, (byte)0, ""),
    LIA_DATA_X_LSB    ((byte)0x28, (byte)0, ""),
    LIA_DATA_X_MSB    ((byte)0x29, (byte)0, ""),
    LIA_DATA_Y_LSB    ((byte)0x2A, (byte)0, ""),
    LIA_DATA_Y_MSB    ((byte)0x2B, (byte)0, ""),
    LIA_DATA_Z_LSB    ((byte)0x2C, (byte)0, ""),
    LIA_DATA_Z_MSB    ((byte)0x2D, (byte)0, ""),
    GRV_DATA_X_LSB    ((byte)0x2E, (byte)0, ""),
    GRV_DATA_X_MSB    ((byte)0x2F, (byte)0, ""),
    GRV_DATA_Y_LSB    ((byte)0x30, (byte)0, ""),
    GRV_DATA_Y_MSB    ((byte)0x31, (byte)0, ""),
    GRV_DATA_Z_LSB    ((byte)0x32, (byte)0, ""),
    GRV_DATA_Z_MSB    ((byte)0x33, (byte)0, ""),
    TEMP              ((byte)0x34, (byte)0, "Temperature"),
    CALIB_STAT        ((byte)0x35, (byte)0, "Calibration Status"),
    ST_RESULT         ((byte)0x36, (byte)0, ""),
    INT_STA           ((byte)0x37, (byte)0, ""),
    SYS_CLK_STATUS    ((byte)0x38, (byte)0, ""),
    SYS_STATUS        ((byte)0x39, (byte)0, ""),
    SYS_ERR           ((byte)0x3A, (byte)0, ""),
    UNIT_SEL          ((byte)0x3B, (byte)0, "Unit Selection"),
    OPR_MODE          ((byte)0x3D, (byte)0, "Operation Mode"),
    PWR_MODE          ((byte)0x3E, (byte)0, "Power Mode"),
    SYS_TRIGGGER      ((byte)0x3F, (byte)0, "System Trigger"),
    TEMP_SOURCE       ((byte)0x40, (byte)0, "Temperature Source"),
    AXIS_MAP_CONFIG   ((byte)0x41, (byte)0, "Configuration Axis Map"),
    AXIS_MAP_SIGN     ((byte)0x42, (byte)0, "Axis Sign"),
    ACC_OFFSET_X_LSB  ((byte)0x55, (byte)0, ""),
    ACC_OFFSET_X_MSB  ((byte)0x56, (byte)0, ""),
    ACC_OFFSET_Y_LSB  ((byte)0x57, (byte)0, ""),
    ACC_OFFSET_Y_MSB  ((byte)0x58, (byte)0, ""),
    ACC_OFFSET_Z_LSB  ((byte)0x59, (byte)0, ""),
    ACC_OFFSET_Z_MSB  ((byte)0x5A, (byte)0, ""),
    MAG_OFFSET_X_LSB  ((byte)0x5B, (byte)0, ""),
    MAG_OFFSET_X_MSB  ((byte)0x5C, (byte)0, ""),
    MAG_OFFSET_Y_LSB  ((byte)0x5D, (byte)0, ""),
    MAG_OFFSET_Y_MSB  ((byte)0x5E, (byte)0, ""),
    MAG_OFFSET_Z_LSB  ((byte)0x5F, (byte)0, ""),
    MAG_OFFSET_Z_MSB  ((byte)0x60, (byte)0, ""),
    GYR_OFFSET_X_LSB  ((byte)0x61, (byte)0, ""),
    GYR_OFFSET_X_MSB  ((byte)0x62, (byte)0, ""),
    GYR_OFFSET_Y_LSB  ((byte)0x63, (byte)0, ""),
    GYR_OFFSET_Y_MSB  ((byte)0x64, (byte)0, ""),
    GYR_OFFSET_Z_LSB  ((byte)0x65, (byte)0, ""),
    GYR_OFFSET_Z_MSB  ((byte)0x66, (byte)0, ""),
    ACC_RADIUS_LSB    ((byte)0x67, (byte)0, ""),
    ACC_RADIUS_MSB    ((byte)0x68, (byte)0, ""),
    MAG_RADIUS_LSB    ((byte)0x69, (byte)0, ""),
    MAG_RADIUS_MSB    ((byte)0x6A, (byte)0, ""),
    //page 1 value
    ACC_CONFIG        ((byte)0x08, (byte)1, "Accelerometer Config"),
    MAG_CONFIG        ((byte)0x09, (byte)1, "Magetometer Config"),
    GYR_CONFIG_0      ((byte)0x0A, (byte)1, "Gyroscope Config 0"),
    GYR_CONFIG_1      ((byte)0x0B, (byte)1, "Gyroscope Config 1"),
    ACC_SLEEP_CONFIG  ((byte)0x0C, (byte)1, ""),
    GYR_SLEEP_CONFIG  ((byte)0x0D, (byte)1, ""),
    INT_MSK           ((byte)0x0F, (byte)1, ""),
    INT_EN            ((byte)0x10, (byte)1, ""),
    ACC_AM_THRES      ((byte)0x11, (byte)1, "Any Motion Accelerometer"),
    ACC_INT_SETTINGS  ((byte)0x12, (byte)1, "Accelerometer Interrupt Setting"),
    ACC_HG_DURATION   ((byte)0x13, (byte)1, ""),
    ACC_HG_THRES      ((byte)0x14, (byte)1, ""),
    ACC_NM_THRES      ((byte)0x15, (byte)0, "Accelerometer No Motion Threshold"),
    ACC_NM_SET        ((byte)0x16, (byte)1, "Accelerometer No Motion Setting"),
    GYR_INT_SETTING    ((byte)0x17, (byte)1, ""),
    GYR_HR_X_SET      ((byte)0x18, (byte)1, ""),
    GYR_DUR_X         ((byte)0x19, (byte)1, ""),
    GYR_HR_Y_SET      ((byte)0x1A, (byte)1, ""),
    GYR_DUR_Y         ((byte)0x1B, (byte)1, ""),
    GYR_HR_Z_SET      ((byte)0x1C, (byte)1, ""),
    GYR_DUR_Z         ((byte)0x1D, (byte)1, ""),
    GYR_AM_THRES      ((byte)0x1E, (byte)1, ""),
    GYR_AM_SET        ((byte)0x1F, (byte)1, "");
    public byte value;
    public byte pageId;
    public String description;
    private register(byte value, byte pageId, String description) {
      this.value = value;
      this.pageId = pageId;
      this.description = description;
    }
  }
  
  public enum Device {
    ACCELEROMETER((byte)0x00, "Accelerometer"),
    MAGNETOMETER((byte)0x01, "Magnetometer"),
    GYROSCOPE((byte)0x02, "Gyroscope");
    public byte value;
    public String description;
    private Device(byte value, String description) {
      this.value = value;
      this.description = description;
    }
  }
 
  public enum PowerMode {
    NORMAL  ((byte)0x00, "Normal"),
    LOW     ((byte)0x01, "Low"),
    SUSPEND ((byte)0x02, "Suspend");
    public byte value;
    public String description;
    private PowerMode(byte value, String description) {
      this.value = value;
      this.description = description;
    }
  }

  public enum OperationMode {
    CONFIG        ((byte)0x00, "Configuration Mode"),
    ACCONLY       ((byte)0x01, "Accelerometer Only"), //raw accelerometer data only
    MAGONLY       ((byte)0x02, "Magnetometer Only"), //raw magnetometer only
    GYROONLY      ((byte)0x03, "Gyroscope Only"),
    ACCMAG        ((byte)0x04, "Accelerometer and Magnetometer"),
    ACCGYRO       ((byte)0x05, "Accelerometer and Gyroscope"),
    MAGGYRO       ((byte)0x06, "Magnetometer and Gyroscope"),
    AMG           ((byte)0x07, "Accelerometer, Magnetometer, Gyroscope"),
    IMU           ((byte)0x08, "IMU"),
    COMPASS       ((byte)0x09, "Compass"),
    M4G           ((byte)0x0A, "Magnet for Gyroscope"),
    NDOF_FMC_OFF  ((byte)0x0B, "NDOF Fast Magnetometer Calibration Off"),
    NDOF          ((byte)0x0C, "NDOF");
    public byte value;
    public String description;
    private OperationMode(byte value, String description) {
      this.value = value;
      this.description = description;
    }
  }
  
  public enum AccelerometerConfig {
    RANGE_2G              ((byte)0x00, (byte)0x03, 0, "Range 2G"),
    RANGE_4G              ((byte)0x01, (byte)0x03, 0, "Range 4G"),
    RANGE_8G              ((byte)0x02, (byte)0x03, 0, "Range 8G"),
    RANGE_16G             ((byte)0x03, (byte)0x03, 0, "Range 16G"),
    BANDWITH_7_81HZ       ((byte)0x00, (byte)0b11100, 2, "Bandwith 7.81Hz"),
    BANDWITH_15_63HZ      ((byte)0x01, (byte)0b11100, 2, "Bandwith 15.63Hz"),
    BANDWITH_31_25HZ      ((byte)0x02, (byte)0b11100, 2, "Bandwith 31.25Hz"),
    BANDWITH_62_5HZ       ((byte)0x03, (byte)0b11100, 2, "Bandwith 62.5Hz"),
    BANDWITH_125HZ        ((byte)0x04, (byte)0b11100, 2, "Bandwith 125Hz"),
    BANDWITH_250HZ        ((byte)0x05, (byte)0b11100, 2, "Bandwith 250Hz"),
    BANDWITH_500HZ        ((byte)0x06, (byte)0b11100, 2, "Bandwith 500Hz"),
    BANDWITH_1000HZ       ((byte)0x07, (byte)0b11100, 2, "Bandwith 1000Hz"),
    OPR_MODE_NORMAL       ((byte)0x00, (byte)0b11100000, 5, "Operation Mode Normal"),
    OPR_MODE_SUSPEND      ((byte)0x01, (byte)0b11100000, 5, "Operation Mode Suspend"),
    OPR_MODE_LOW_POWER_1  ((byte)0x02, (byte)0b11100000, 5, "Operation Mode Low Power 1"),
    OPR_MODE_STANBY       ((byte)0x03, (byte)0b11100000, 5, "Operation Mode Stanby"),
    OPR_MODE_LOW_POWER_2  ((byte)0x04, (byte)0b11100000, 5, "Operation Mode Low Power 2"),
    OPR_MODE_DEEP_SUSPEND ((byte)0x05, (byte)0b11100000, 5, "Operation Mode Deep Suspend");
    public byte value;
    public byte mask;
    public int shift;
    public String description;
    private AccelerometerConfig(byte value, byte mask, int shift, String description) {
      this.value = value;
      this.mask = mask;
      this.shift = shift;
      this.description = description;
    }
  }

  public enum GyroscopeConfig {
    RANGE_2000DPS               ((byte)0x00, (byte)0b111, 0, (byte)0, "Range 2000 dps"),
    RANGE_1000DPS               ((byte)0x01, (byte)0b111, 0, (byte)0, "Range 1000 dps"),
    RANGE_500DPS                ((byte)0x02, (byte)0b111, 0, (byte)0, "Range 500 dps"),
    RANGE_250DPS                ((byte)0x03, (byte)0b111, 0, (byte)0, "Range 250 dps"),
    RANGE_125DPS                ((byte)0x04, (byte)0b111, 0, (byte)0, "Range 125 dps"),
    BANDWITH_523Hz              ((byte)0x00, (byte)0b111000, 3, (byte)0, "Bandwith 523Hz"),
    BANDWITH_230Hz              ((byte)0x01, (byte)0b111000, 3, (byte)0, "Bandwith 230Hz"),
    BANDWITH_116Hz              ((byte)0x02, (byte)0b111000, 3, (byte)0, "Bandwith 116Hz"),
    BANDWITH_47Hz               ((byte)0x03, (byte)0b111000, 3, (byte)0, "Bandwith 47Hz"),
    BANDWITH_23Hz               ((byte)0x04, (byte)0b111000, 3, (byte)0, "Bandwith 23Hz"),
    BANDWITH_12Hz               ((byte)0x05, (byte)0b111000, 3, (byte)0, "Bandwith 12Hz"),
    BANDWITH_64Hz               ((byte)0x06, (byte)0b111000, 3, (byte)0, "Bandwith 64Hz"),
    BANDWITH_32Hz               ((byte)0x07, (byte)0b111000, 3, (byte)0, "Bandwith 32Hz"),
    OPR_MODE_NORMAL             ((byte)0x00, (byte)0b111, 0, (byte)1, "Operation Mode Normal"),
    OPR_MODE_FAST_POWER_UP      ((byte)0x01, (byte)0b111, 0, (byte)1, "Operation Mode Fast Power Up"),
    OPR_MODE_DEEP_SUSPEND       ((byte)0x02, (byte)0b111, 0, (byte)1, "Operation Mode Deep Suspend"),
    OPR_MODE_SUSPEND            ((byte)0x03, (byte)0b111, 0, (byte)1, "Operation Mode Suspend"),
    OPR_MODE_ADVANCE_POWERSAVE  ((byte)0x04, (byte)0b111, 0, (byte)1, "Operation Mode Advance Powersave");
    public byte value;
    public byte mask;
    public int shift;
    public byte regAdd;
    public String description;
    private GyroscopeConfig(byte value, byte mask, int shift, byte regAdd, String description) {
      this.value = value;
      this.mask = mask;
      this.shift = shift;
      this.regAdd = regAdd;
      this.description = description;
    }
  }
  
  public enum MagnetometerConfig {
    DATA_OUTPUT_RATE_2HZ      ((byte)0x00, (byte)0b111, 0, "Data Output Rate 2Hz"),
    DATA_OUTPUT_RATE_6HZ      ((byte)0x01, (byte)0b111, 0, "Data Output Rate 6Hz"),
    DATA_OUTPUT_RATE_8HZ      ((byte)0x02, (byte)0b111, 0, "Data Output Rate 8Hz"),
    DATA_OUTPUT_RATE_10HZ     ((byte)0x03, (byte)0b111, 0, "Data Output Rate 10Hz"),
    DATA_OUTPUT_RATE_15HZ     ((byte)0x04, (byte)0b111, 0, "Data Output Rate 15Hz"),
    DATA_OUTPUT_RATE_20HZ     ((byte)0x05, (byte)0b111, 0, "Data Output Rate 20Hz"),
    DATA_OUTPUT_RATE_25HZ     ((byte)0x06, (byte)0b111, 0, "Data Output Rate 25Hz"),
    DATA_OUTPUT_RATE_30HZ     ((byte)0x07, (byte)0b111, 0, "Data Output Rate 30Hz"),
    OPR_MODE_LOW_POWER        ((byte)0x00, (byte)0b11000, 3, "Operation Mode Low Power"),
    OPR_MODE_REGULAR          ((byte)0x01, (byte)0b11000, 3, "Operation Mode Regular"),
    OPR_MODE_ENHANCED_REGULAR ((byte)0x02, (byte)0b11000, 3, "Operation Mode Enhanced Regular"),
    OPR_MODE_HIGH_ACCURACY    ((byte)0x03, (byte)0b11000, 3, "Operation Mode High Accuracy"),
    PWR_MODE_NORMAL           ((byte)0x00, (byte)0b1100000, 5, "Power Mode Normal"),
    PWR_MODE_SLEEP            ((byte)0x01, (byte)0b1100000, 5, "Power Mode Sleep"),
    PWR_MODE_SUSPEND          ((byte)0x02, (byte)0b1100000, 5, "Power Mode Suspend"),
    PWR_MODE_FORCE_MODE       ((byte)0x03, (byte)0b1100000, 5, "Power Mode Force Mode");
    public byte value;
    public byte mask;
    public int shift;
    public String description;
    private MagnetometerConfig(byte value, byte mask, int shift, String description) {
      this.value = value;
      this.mask = mask;
      this.shift = shift;
      this.description = description;
    }
  }
  
  public enum AxisMapConfig {
    X_AXIS  ((byte)0x00, (byte)0b11, 0, "Axis X"),
    Y_AXIS  ((byte)0x01, (byte)0b1100, 2, "Axis Y"),
    Z_AXIS  ((byte)0x02, (byte)0b110000, 4, "Axis Z");
    public byte value;
    public byte mask;
    public int shift;
    public String description;
    private AxisMapConfig(byte value, byte mask, int shift, String description) {
      this.value = value;
      this.mask = mask;
      this.shift = shift;
      this.description = description;
    }
  }
  
  public enum Unit {
    ACC_M_S2              ((byte)0x00, (byte)0b1, 0, "m/s2"),
    ACC_MG                ((byte)0x01, (byte)0b1, 0, "mg"),
    ANGULAR_RATE_DPS      ((byte)0x00, (byte)0b10, 1, "dps"),
    ANGULAR_RATE_RPS      ((byte)0x01, (byte)0b10, 1, "rps"),
    EULER_ANGLE_DEG       ((byte)0x00, (byte)0b100, 2, "degree"),
    EULER_ANGLE_RAD       ((byte)0x01, (byte)0b100, 2, "radian"),
    TEMP_C                ((byte)0x00, (byte)0b10000, 4, "C"),
    TEMP_F                ((byte)0x01, (byte)0b10000, 4, "F"),
    OUTPUT_FORMAT_WINDOWS ((byte)0x00, (byte)0b10000000, 7, "Output Format Window"), 
    OUTPUT_FORMAT_ANDROID ((byte)0x01, (byte)0b10000000, 7, "Output Format Android"), 
    MAG                   ((byte)0x00, (byte)0b0, 0, "Micro Tesla"),
    QUAT                  ((byte)0x00, (byte)0b0, 0, "");
    public byte value;
    public byte mask;
    public int shift;
    public String description;
    private Unit(byte value, byte mask, int shift, String description) {
      this.value = value;
      this.mask = mask;
      this.shift = shift;
      this.description = description;
    }
  }
  
  public enum CalibStat {
    MAG ((byte)0x00, (byte)0b11, 0, "Magnetometer Calibration Status"),
    ACC ((byte)0x01, (byte)0b1100, 2, "Accelerometer Calibration Status"),
    GYR ((byte)0x02, (byte)0b110000, 4, "Gyroscope Calibration Status"),
    SYS ((byte)0x03, (byte)0b11000000, 6, "System Calibration Status");
    public byte value;
    public byte mask;
    public int shift;
    public String description;
    private CalibStat(byte value, byte mask, int shift, String description) {
      this.value = value;
      this.mask = mask;
      this.shift = shift;
      this.description = description;
    }
  }
  
  public enum InterruptType {
    ACC_NM ((byte)0x00, (byte)0b10000000),
    ACC_SM ((byte)0x01, (byte)0b10000000),
    ACC_AM ((byte)0x02, (byte)0b1000000),
    ACC_HG ((byte)0x03, (byte)0b100000),
    GYR_HR ((byte)0x04, (byte)0b1000),
    GYR_AM ((byte)0x05, (byte)0b100);
    public byte value;
    public byte mask;
    InterruptType(byte value, byte mask) {
      this.value = value;
      this.mask = mask;
    }
  }
  private OperationMode mode;

  private PinArrayControl pinControl = null;

  private Integer pin = null;

  private boolean isEnabled = false;

  public boolean isActive = false;

  public class Bno055Data {
    public double w;
    public double x;
    public double y;
    public double z;
    public double yaw;
    public double roll;
    public double pitch;
    public double temperature;
    public long timestamp = System.currentTimeMillis();
    Unit unit;
  }

  public class Bno055Event {
    public int version;
    public long timestamp = System.currentTimeMillis();

    public class Orientation {
      public double x;
      public double y;
      public double z;
    }

    public Orientation orientation = new Orientation();
  }

  public static void main(String[] args) {
    LoggingFactory.getInstance().configure();

    try {

      int[] buffer = new int[] { (int) 0xff, (int) 0xd0 };
      int a = (byte) buffer[0] << 8 | buffer[1] & 0xff;
      log.info(String.format("0xffd0 should be -48 is = %s", a));

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public Bno055(String n) {
    super(n);
    refreshControllers();
    subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
  }

  public void onRegistered(ServiceInterface s) {
    refreshControllers();
    broadcastState();

  }

  public List<String> refreshControllers() {
    controllers = Runtime.getServiceNamesFromInterface(I2CController.class);
    return controllers;
  }

  @Override
  public void startService() {
    super.startService();
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

  public boolean isAttached() {
    return isAttached;
  }

  /**
   * This method reads all the 7 raw values in one go accelX accelY accelZ
   * temperature ( In degrees Celcius ) gyroX gyroY gyroZ
   * 
   */
  /**
   * TODO Make the way it should be Currently only used for test of data binding
   * to the webgui
   */
  public void refresh() {
    broadcastState();
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

    ServiceType meta = new ServiceType(Bno055.class);
    meta.addDescription("General BNO055 acclerometer and gyro");
    meta.addCategory("microcontroller", "sensor");
    meta.setSponsor("calamity");
    return meta;
  }

  public boolean begin() {
    return begin(OperationMode.NDOF);
  }

  public boolean begin(OperationMode mode) {

    byte[] buffer = new byte[1];
    /* Make sure we have the right device */
    i2cWriteReadReg(register.CHIP_ID, buffer, 1);
    if ((buffer[0] & 0xFF) != BNO055_ID) {
      log.info("BNO055 sensor not found");
      return false;
    }
    /* Reset */
    i2cWrite(register.SYS_TRIGGGER, (byte) 0x20);
    buffer[0] = 0;
    while ((buffer[0] & 0xFF) != BNO055_ID) {
      i2cWriteReadReg(register.CHIP_ID, buffer, 1);
      sleep(500);
    }
    /* Set to normal power mode */
    i2cWrite(register.PWR_MODE, PowerMode.NORMAL.value);
    sleep(10);
    i2cWrite(register.SYS_TRIGGGER, (byte) 0x00);
    sleep(10);
    setMode(OperationMode.NDOF);
    sleep(20);
    return true;
  }

  public void setMode(OperationMode mode) {
    this.mode = mode;
    i2cWrite(register.OPR_MODE, mode.value);
    sleep(20);
  }

  private void i2cWrite(register reg, byte value) {
    if (reg != register.PAGE_ID) {
      byte[] wbuffer = new byte[] { register.PAGE_ID.value, reg.pageId };
      controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), wbuffer, wbuffer.length);
    }
    byte[] wbuffer1 = new byte[] { reg.value, value };
    controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), wbuffer1, wbuffer1.length);
  }

  private void i2cWriteReadReg(register reg, byte[] data, int length) {
    if (reg != register.PAGE_ID) {
      byte[] wbuffer = new byte[] { register.PAGE_ID.value, reg.pageId };
      controller.i2cWrite(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), wbuffer, wbuffer.length);
    }
    controller.i2cWriteRead(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), new byte[] { reg.value }, 1, data, length);
  }

  private byte i2cWriteReadRegByte(register reg) {
    byte[] data = new byte[1];
    i2cWriteReadReg(reg, data, data.length);
    return data[0];
  }

  public void setExtCrystalUse(boolean value) {
    OperationMode modeback = mode;
    setMode(OperationMode.CONFIG);
    sleep(25);
    if (value) {
      i2cWrite(register.SYS_TRIGGGER, (byte) 0x80);
    } else {
      i2cWrite(register.SYS_TRIGGGER, (byte) 0x00);
    }
    sleep(10);
    setMode(modeback);
    sleep(20);
  }

  public Bno055Event getEvent() {
    Bno055Event event = new Bno055Event();
    byte[] wbuffer = new byte[] { register.EUL_HEADING_LSB.value };
    byte[] rbuffer = new byte[6];
    // controller.i2cWrite(this, Integer.parseInt(deviceBus),
    // Integer.decode(deviceAddress), wbuffer, wbuffer.length);
    // controller.i2cRead(this, Integer.parseInt(deviceBus),
    // Integer.decode(deviceAddress), rbuffer, rbuffer.length);
    controller.i2cWriteRead(this, Integer.parseInt(deviceBus), Integer.decode(deviceAddress), wbuffer, wbuffer.length, rbuffer, rbuffer.length);
    log.info("Bno055 i2c Read return {}", rbuffer);
    event.orientation.x = (((int) (rbuffer[0] & 0xFF)) | (((int) (rbuffer[1])) << 8)) / 16.0;
    event.orientation.y = (((int) (rbuffer[2] & 0xFF)) | (((int) (rbuffer[3])) << 8)) / 16.0;
    event.orientation.z = (((int) (rbuffer[4] & 0xFF)) | (((int) (rbuffer[5])) << 8)) / 16.0;
    return event;

  }

  public void setPowerMode(PowerMode mode) {
    OperationMode modeback = this.mode;
    setMode(OperationMode.CONFIG);
    i2cWrite(register.PWR_MODE, mode.value);
    sleep(10);
    setMode(modeback);
  }

  // detectX/Y/Z = true enable the sleep/wake on that axis
  // sleepDuration = number of second in that state to enter in sleep mode
  // (0-336s) default = 6s
  // wakeDuration = number of consecutive event over wakeThreshold to enter in
  // wake mode (1-4) default = 4
  // Threshold = value that must be exceed to enter wake mode or not exceed to
  // enter sleep mode (in mg) mg = milli G force
  public void setPowerModeLow(boolean detectX, boolean detectY, boolean detectZ, int sleepDuration, double sleepThreshold, int wakeDuration, double wakeThreshold) {
    byte acc_nm_set_value = 0;
    if (sleepDuration < 0) {
      log.info("BNO055 minimum Duration = 0");
      return;
    }
    if (sleepDuration <= 16) {
      acc_nm_set_value = (byte) ((sleepDuration - 1) << 1);
    } else if (sleepDuration <= 80) {
      acc_nm_set_value = (byte) ((((sleepDuration - 20) / 4) << 1) & 1 << 5);
    } else if (sleepDuration <= 336) {
      acc_nm_set_value = (byte) ((((sleepDuration - 88) / 8) << 1) & 1 << 6);
    } else {
      log.info("BNO055 maximum Duration = 336");
      return;
    }
    byte acc_int_setting_value = (byte) (i2cWriteReadRegByte(register.ACC_INT_SETTINGS) & 0b11100000);
    if (detectZ)
      acc_int_setting_value &= 0b00010000;
    if (detectY)
      acc_int_setting_value &= 0b00001000;
    if (detectZ)
      acc_int_setting_value &= 0b00000100;
    if (wakeDuration < 1 || wakeDuration > 4) {
      log.info("BNO055 wakeDuration value must be 1-4");
      return;
    }
    acc_int_setting_value &= (wakeDuration - 1);
    byte acc_config = (byte) (i2cWriteReadRegByte(register.ACC_CONFIG) & AccelerometerConfig.RANGE_2G.mask);
    AccelerometerConfig range = null;
    for (AccelerometerConfig c : AccelerometerConfig.values()) {
      if (c.value == acc_config) {
        range = c;
        break;
      }
    }
    byte acc_nm_thre = 0;
    byte acc_am_thre = 0;
    switch (range) {
      case RANGE_2G:
        if (sleepThreshold > 1000) {
          log.info("Maximum value for sleepThreshold with current setting is 1000mg");
          return;
        }
        acc_nm_thre = (byte) (sleepThreshold / 3.91);
        if (wakeThreshold > 1000) {
          log.info("Maximum value for wakeThreshold with current setting is 1000mg");
          return;
        }
        acc_am_thre = (byte) (sleepThreshold / 3.91);
        break;
      case RANGE_4G:
        if (sleepThreshold > 1900) {
          log.info("Maximum value for sleepThreshold with current setting is 1900mg");
          return;
        }
        acc_nm_thre = (byte) (sleepThreshold / 7.81);
        if (wakeThreshold > 1900) {
          log.info("Maximum value for wakeThreshold with current setting is 1900mg");
          return;
        }
        acc_am_thre = (byte) (sleepThreshold / 7.81);
        break;
      case RANGE_8G:
        if (sleepThreshold > 3985) {
          log.info("Maximum value for sleepThreshold with current setting is 3985mg");
          return;
        }
        acc_nm_thre = (byte) (sleepThreshold / 15.63);
        if (wakeThreshold > 3985) {
          log.info("Maximum value for wakeThreshold with current setting is 3985mg");
          return;
        }
        acc_am_thre = (byte) (sleepThreshold / 15.63);
        break;
      case RANGE_16G:
        if (sleepThreshold > 7968) {
          log.info("Maximum value for sleepThreshold with current setting is 7968mg");
          return;
        }
        acc_nm_thre = (byte) (sleepThreshold / 31.25);
        if (wakeThreshold > 7968) {
          log.info("Maximum value for wakeThreshold with current setting is 7968mg");
          return;
        }
        acc_am_thre = (byte) (sleepThreshold / 31.25);
        break;
      default:
        log.info("invalid sleepThreshold value");
        return;
    }
    OperationMode modeback = this.mode;
    setMode(OperationMode.CONFIG);
    i2cWrite(register.ACC_NM_SET, acc_nm_set_value);
    i2cWrite(register.ACC_INT_SETTINGS, acc_int_setting_value);
    i2cWrite(register.ACC_NM_THRES, acc_nm_thre);
    i2cWrite(register.ACC_AM_THRES, acc_am_thre);
    setMode(modeback);
  }
  /*
   * AxisRemap : The device mounting position should not limit the data output
   * of the BNO055 device. The axis of the device can be re-configured to the
   * new reference axis.
   */

  public void axisRemap(AxisMapConfig xAxis, AxisMapConfig yAxis, AxisMapConfig zAxis) {
    if (xAxis == yAxis || xAxis == zAxis || yAxis == zAxis) {
      log.info("BNO055 AzisRemap: duplicate axis definition");
      return;
    }
    byte axis_map_config = (byte) ((zAxis.value << zAxis.shift) | (yAxis.value << yAxis.shift) | (xAxis.value << xAxis.shift));
    OperationMode modeback = this.mode;
    setMode(OperationMode.CONFIG);
    i2cWrite(register.AXIS_MAP_CONFIG, axis_map_config);
    setMode(modeback);
  }

  /*
   * Change the direction of an Axis
   * 
   * @param positive : true = positive, false = reversed
   */
  public void axisMapSign(AxisMapConfig axis, boolean positive) {
    byte axis_map_sign = i2cWriteReadRegByte(register.AXIS_MAP_SIGN);
    switch (axis) {
      case X_AXIS:
        axis_map_sign &= 0b011;
        if (!positive)
          axis_map_sign |= 0b100;
        break;
      case Y_AXIS:
        axis_map_sign &= 0b101;
        if (!positive)
          axis_map_sign |= 0b010;
        break;
      case Z_AXIS:
        axis_map_sign &= 0b110;
        if (!positive)
          axis_map_sign |= 0b001;
        break;
    }
    OperationMode modeback = this.mode;
    setMode(OperationMode.CONFIG);
    i2cWrite(register.AXIS_MAP_SIGN, axis_map_sign);
    setMode(modeback);
  }

  public void accelerometerConfig(AccelerometerConfig config) {
    byte acc_config = i2cWriteReadRegByte(register.ACC_CONFIG);
    acc_config &= ~config.mask;
    acc_config |= (config.value << config.shift);
    OperationMode modeback = this.mode;
    setMode(OperationMode.CONFIG);
    i2cWrite(register.ACC_CONFIG, acc_config);
    setMode(modeback);
  }

  public ArrayList<AccelerometerConfig> getAccelerometerConfigs() {
    ArrayList<AccelerometerConfig> configs = new ArrayList<AccelerometerConfig>();
    byte data = i2cWriteReadRegByte(register.ACC_CONFIG);
    for (AccelerometerConfig c : AccelerometerConfig.values()) {
      if (((data & c.mask) >> c.shift) == c.value) {
        configs.add(c);
      }
    }
    return configs;
  }

  public void gyroscopeConfig(GyroscopeConfig config) {
    if (config.regAdd == 0) {
      byte gyr_config = i2cWriteReadRegByte(register.GYR_CONFIG_0);
      gyr_config &= ~config.mask;
      gyr_config |= (config.value << config.shift);
      OperationMode modeback = this.mode;
      setMode(OperationMode.CONFIG);
      i2cWrite(register.GYR_CONFIG_0, gyr_config);
      setMode(modeback);
    } else {
      OperationMode modeback = this.mode;
      setMode(OperationMode.CONFIG);
      i2cWrite(register.GYR_CONFIG_1, config.value);
      setMode(modeback);
    }
  }

  public void magnetometerConfig(MagnetometerConfig config) {
    byte mag_config = i2cWriteReadRegByte(register.MAG_CONFIG);
    mag_config &= ~config.mask;
    mag_config |= (config.value << config.shift);
    OperationMode modeback = this.mode;
    setMode(OperationMode.CONFIG);
    i2cWrite(register.MAG_CONFIG, mag_config);
    setMode(modeback);
  }

  public void unitSelection(Unit unit) {
    byte unit_sel = i2cWriteReadRegByte(register.UNIT_SEL);
    unit_sel &= ~unit.mask;
    unit_sel |= (unit.value << unit.shift);
    OperationMode modeback = this.mode;
    setMode(OperationMode.CONFIG);
    i2cWrite(register.UNIT_SEL, unit_sel);
    setMode(modeback);

  }

  public byte getRegValue(register reg) {
    return i2cWriteReadRegByte(reg);
  }

  public Bno055Data getAccelerationData() {
    Bno055Data retval = new Bno055Data();
    byte[] data = new byte[6];
    i2cWriteReadReg(register.ACC_DATA_X_LSB, data, data.length);
    byte unit = (byte) ((i2cWriteReadRegByte(register.UNIT_SEL) & Unit.ACC_M_S2.mask) >> Unit.ACC_M_S2.shift);
    retval.unit = Unit.ACC_M_S2;
    if (unit == Unit.ACC_MG.value) {
      retval.unit = Unit.ACC_MG;
      retval.x = (double) ((data[0] & 0xFF) + ((data[1]) << 8));
      retval.y = (double) ((data[2] & 0xFF) + ((data[3]) << 8));
      retval.z = (double) ((data[4] & 0xFF) + ((data[5]) << 8));
    } else {
      retval.x = (double) ((data[0] & 0xFF) + ((data[1]) << 8)) / 100;
      retval.y = (double) ((data[2] & 0xFF) + ((data[3]) << 8)) / 100;
      retval.z = (double) ((data[4] & 0xFF) + ((data[5]) << 8)) / 100;
    }
    return retval;
  }

  public Bno055Data getMagneticFieldStrength() {
    Bno055Data retval = new Bno055Data();
    byte[] data = new byte[6];
    i2cWriteReadReg(register.MAG_DATA_X_LSB, data, data.length);
    retval.unit = Unit.MAG;
    retval.x = (double) ((data[0] & 0xFF) + ((data[1]) << 8)) / 16;
    retval.y = (double) ((data[2] & 0xFF) + ((data[3]) << 8)) / 16;
    retval.z = (double) ((data[4] & 0xFF) + ((data[5]) << 8)) / 16;
    return retval;
  }

  public Bno055Data getAngularVelocity() {
    Bno055Data retval = new Bno055Data();
    byte[] data = new byte[6];
    i2cWriteReadReg(register.GYR_DATA_X_LSB, data, data.length);
    byte unit = (byte) ((i2cWriteReadRegByte(register.UNIT_SEL) & Unit.ANGULAR_RATE_DPS.mask) >> Unit.ANGULAR_RATE_DPS.shift);
    retval.unit = Unit.ANGULAR_RATE_DPS;
    if (unit == Unit.ANGULAR_RATE_RPS.value) {
      retval.unit = Unit.ANGULAR_RATE_RPS;
      retval.x = (double) ((data[0] & 0xFF) + ((data[1]) << 8) / 900);
      retval.y = (double) ((data[2] & 0xFF) + ((data[3]) << 8) / 900);
      retval.z = (double) ((data[4] & 0xFF) + ((data[5]) << 8) / 900);
    } else {
      retval.x = (double) ((data[0] & 0xFF) + ((data[1]) << 8)) / 16;
      retval.y = (double) ((data[2] & 0xFF) + ((data[3]) << 8)) / 16;
      retval.z = (double) ((data[4] & 0xFF) + ((data[5]) << 8)) / 16;
    }
    return retval;
  }

  public Bno055Data getOrientationEuler() {
    Bno055Data retval = new Bno055Data();
    byte[] data = new byte[6];
    i2cWriteReadReg(register.EUL_HEADING_LSB, data, data.length);
    byte unit = (byte) ((i2cWriteReadRegByte(register.UNIT_SEL) & Unit.EULER_ANGLE_DEG.mask) >> Unit.EULER_ANGLE_DEG.shift);
    retval.unit = Unit.EULER_ANGLE_DEG;
    if (unit == Unit.EULER_ANGLE_RAD.value) {
      retval.unit = Unit.EULER_ANGLE_RAD;
      retval.yaw = (double) ((data[0] & 0xFF) + ((data[1]) << 8) / 900);
      retval.roll = (double) ((data[2] & 0xFF) + ((data[3]) << 8) / 900);
      retval.pitch = (double) ((data[4] & 0xFF) + ((data[5]) << 8) / 900);
    } else {
      retval.yaw = (double) ((data[0] & 0xFF) + ((data[1]) << 8)) / 16;
      retval.roll = (double) ((data[2] & 0xFF) + ((data[3]) << 8)) / 16;
      retval.pitch = (double) ((data[4] & 0xFF) + ((data[5]) << 8)) / 16;
    }
    return retval;
  }

  public Bno055Data getOrientationQuaternion() {
    Bno055Data retval = new Bno055Data();
    byte[] data = new byte[8];
    i2cWriteReadReg(register.QUA_DATA_W_LSB, data, data.length);
    retval.unit = Unit.QUAT;
    retval.w = (double) ((data[0] & 0xFF) + ((data[1]) << 8)) / (1 << 14);
    retval.x = (double) ((data[2] & 0xFF) + ((data[3]) << 8)) / (1 << 14);
    retval.y = (double) ((data[4] & 0xFF) + ((data[5]) << 8)) / (1 << 14);
    retval.z = (double) ((data[6] & 0xFF) + ((data[7]) << 8)) / (1 << 14);
    return retval;
  }

  public Bno055Data getLinearAcceleration() {
    Bno055Data retval = new Bno055Data();
    byte[] data = new byte[6];
    i2cWriteReadReg(register.LIA_DATA_X_LSB, data, data.length);
    byte unit = (byte) ((i2cWriteReadRegByte(register.UNIT_SEL) & Unit.ACC_M_S2.mask) >> Unit.ACC_M_S2.shift);
    retval.unit = Unit.ACC_M_S2;
    if (unit == Unit.ACC_MG.value) {
      retval.unit = Unit.ACC_MG;
      retval.x = (double) ((data[0] & 0xFF) + ((data[1]) << 8));
      retval.y = (double) ((data[2] & 0xFF) + ((data[3]) << 8));
      retval.z = (double) ((data[4] & 0xFF) + ((data[5]) << 8));
    } else {
      retval.x = (double) ((data[0] & 0xFF) + ((data[1]) << 8)) / 100;
      retval.y = (double) ((data[2] & 0xFF) + ((data[3]) << 8)) / 100;
      retval.z = (double) ((data[4] & 0xFF) + ((data[5]) << 8)) / 100;
    }
    return retval;
  }

  public Bno055Data getGravityVector() {
    Bno055Data retval = new Bno055Data();
    byte[] data = new byte[6];
    i2cWriteReadReg(register.GRV_DATA_X_LSB, data, data.length);
    byte unit = (byte) ((i2cWriteReadRegByte(register.UNIT_SEL) & Unit.ACC_M_S2.mask) >> Unit.ACC_M_S2.shift);
    retval.unit = Unit.ACC_M_S2;
    if (unit == Unit.ACC_MG.value) {
      retval.unit = Unit.ACC_MG;
      retval.x = (double) ((data[0] & 0xFF) + ((data[1]) << 8));
      retval.y = (double) ((data[2] & 0xFF) + ((data[3]) << 8));
      retval.z = (double) ((data[4] & 0xFF) + ((data[5]) << 8));
    } else {
      retval.x = (double) ((data[0] & 0xFF) + ((data[1]) << 8)) / 100;
      retval.y = (double) ((data[2] & 0xFF) + ((data[3]) << 8)) / 100;
      retval.z = (double) ((data[4] & 0xFF) + ((data[5]) << 8)) / 100;
    }
    return retval;
  }

  public void setTemperatureSourceAccelerometer() {
    OperationMode modeback = this.mode;
    setMode(OperationMode.CONFIG);
    i2cWrite(register.TEMP_SOURCE, (byte) 0);
    setMode(modeback);
  }

  public void setTemperatureSourceGyroscope() {
    OperationMode modeback = this.mode;
    setMode(OperationMode.CONFIG);
    i2cWrite(register.TEMP_SOURCE, (byte) 1);
    setMode(modeback);
  }

  public Bno055Data getTemperature() {
    Bno055Data retval = new Bno055Data();
    byte data = i2cWriteReadRegByte(register.TEMP);
    byte unit = (byte) ((i2cWriteReadRegByte(register.UNIT_SEL) & Unit.TEMP_C.mask) >> Unit.TEMP_C.shift);
    retval.unit = Unit.TEMP_C;
    if (unit == Unit.TEMP_F.value) {
      retval.unit = Unit.TEMP_F;
      retval.temperature = (double) (data * 2);
    } else {
      retval.temperature = (double) (data);
    }
    return retval;
  }

  public void setCalibrationOffset(Device device, float xOffset, float yOffset, float zOffset, Unit unit) {
    switch (device) {
      case ACCELEROMETER: {
        if (unit == Unit.ACC_M_S2) {
          xOffset *= 100;
          yOffset *= 100;
          zOffset *= 100;
        } else if (unit == Unit.ACC_MG) {
          // nothing to do
        } else {
          log.info("Wrong unit type for {}", device.description);
          return;
        }
        unitSelection(unit);
        OperationMode modeback = this.mode;
        setMode(OperationMode.CONFIG);
        i2cWrite(register.ACC_OFFSET_X_LSB, (byte) ((byte) xOffset & 0xFF));
        i2cWrite(register.ACC_OFFSET_X_MSB, (byte) (((int) xOffset >> 8) & 0xFF));
        i2cWrite(register.ACC_OFFSET_Y_LSB, (byte) ((int) yOffset & 0xFF));
        i2cWrite(register.ACC_OFFSET_Y_MSB, (byte) (((int) yOffset >> 8) & 0xFF));
        i2cWrite(register.ACC_OFFSET_Z_LSB, (byte) ((int) zOffset & 0xFF));
        i2cWrite(register.ACC_OFFSET_Z_MSB, (byte) (((int) zOffset >> 8) & 0xFF));
        setMode(modeback);
        return;
      }
      case MAGNETOMETER: {
        xOffset *= 16;
        yOffset *= 16;
        zOffset *= 16;
        unitSelection(unit);
        OperationMode modeback = this.mode;
        setMode(OperationMode.CONFIG);
        i2cWrite(register.MAG_OFFSET_X_LSB, (byte) ((int) xOffset & 0xFF));
        i2cWrite(register.MAG_OFFSET_X_MSB, (byte) (((int) xOffset >> 8) & 0xFF));
        i2cWrite(register.MAG_OFFSET_Y_LSB, (byte) ((int) yOffset & 0xFF));
        i2cWrite(register.MAG_OFFSET_Y_MSB, (byte) (((int) yOffset >> 8) & 0xFF));
        i2cWrite(register.MAG_OFFSET_Z_LSB, (byte) ((int) zOffset & 0xFF));
        i2cWrite(register.MAG_OFFSET_Z_MSB, (byte) (((int) zOffset >> 8) & 0xFF));
        setMode(modeback);
        return;
      }
      case GYROSCOPE: {
        if (unit == Unit.ANGULAR_RATE_DPS) {
          xOffset *= 16;
          yOffset *= 16;
          zOffset *= 16;
        } else if (unit == Unit.ANGULAR_RATE_RPS) {
          xOffset *= 900;
          yOffset *= 900;
          zOffset *= 900;
        } else {
          log.info("Wrong unit type for {}", device.description);
          return;
        }
        unitSelection(unit);
        OperationMode modeback = this.mode;
        setMode(OperationMode.CONFIG);
        i2cWrite(register.GYR_OFFSET_X_LSB, (byte) ((int) xOffset & 0xFF));
        i2cWrite(register.GYR_OFFSET_X_MSB, (byte) (((int) xOffset >> 8) & 0xFF));
        i2cWrite(register.GYR_OFFSET_Y_LSB, (byte) ((int) yOffset & 0xFF));
        i2cWrite(register.GYR_OFFSET_Y_MSB, (byte) (((int) yOffset >> 8) & 0xFF));
        i2cWrite(register.GYR_OFFSET_Z_LSB, (byte) ((int) zOffset & 0xFF));
        i2cWrite(register.GYR_OFFSET_Z_MSB, (byte) (((int) zOffset >> 8) & 0xFF));
        setMode(modeback);
        return;
      }
    }
  }

  public Bno055Data getCalibrationOffset(Device device) {
    Bno055Data data = new Bno055Data();
    OperationMode modeback = this.mode;
    setMode(OperationMode.CONFIG);
    byte unitvalue = i2cWriteReadRegByte(register.UNIT_SEL);
    switch (device) {
      case ACCELEROMETER: {
        byte[] value = new byte[6];
        i2cWriteReadReg(register.ACC_OFFSET_X_LSB, value, value.length);
        data.x = (value[0] & 0xFF) + ((value[1]) << 8);
        data.y = (value[2] & 0xFF) + ((value[3]) << 8);
        data.z = (value[4] & 0xFF) + ((value[5]) << 8);
        if (((unitvalue & Unit.ACC_M_S2.mask) >> Unit.ACC_M_S2.shift) == Unit.ACC_M_S2.value) {
          data.unit = Unit.ACC_M_S2;
          data.x /= 100;
          data.y /= 100;
          data.z /= 100;
        } else {
          data.unit = Unit.ACC_MG;
        }
        setMode(modeback);
        return data;
      }
      case MAGNETOMETER: {
        byte[] value = new byte[6];
        i2cWriteReadReg(register.MAG_OFFSET_X_LSB, value, value.length);
        data.x = (value[0] & 0xFF) + ((value[1]) << 8);
        data.y = (value[2] & 0xFF) + ((value[3]) << 8);
        data.z = (value[4] & 0xFF) + ((value[5]) << 8);
        data.unit = Unit.MAG;
        data.x /= 16;
        data.y /= 16;
        data.z /= 16;
        setMode(modeback);
        return data;
      }
      case GYROSCOPE: {
        byte[] value = new byte[6];
        i2cWriteReadReg(register.GYR_OFFSET_X_LSB, value, value.length);
        data.x = (value[0] & 0xFF) + ((value[1]) << 8);
        data.y = (value[2] & 0xFF) + ((value[3]) << 8);
        data.z = (value[4] & 0xFF) + ((value[5]) << 8);
        if (((unitvalue & Unit.ANGULAR_RATE_DPS.mask) >> Unit.ANGULAR_RATE_DPS.shift) == Unit.ANGULAR_RATE_DPS.value) {
          data.unit = Unit.ANGULAR_RATE_DPS;
          data.x /= 16;
          data.y /= 16;
          data.z /= 16;
        } else {
          data.unit = Unit.ANGULAR_RATE_RPS;
          data.x /= 900;
          data.y /= 900;
          data.z /= 900;
        }
        setMode(modeback);
        return data;
      }
    }
    return data;
  }

  public void setRadius(Device device, int radius) {
    switch (device) {
      case ACCELEROMETER: {
        if (radius < -1000 || radius > 1000) {
          log.info("Out of bound radius value");
          return;
        }
        OperationMode modeback = this.mode;
        setMode(OperationMode.CONFIG);
        i2cWrite(register.ACC_RADIUS_LSB, (byte) (radius & 0xFF));
        i2cWrite(register.ACC_RADIUS_MSB, (byte) ((radius >> 8) & 0xFF));
        setMode(modeback);
        return;
      }
      case MAGNETOMETER: {
        if (radius < -960 || radius > 960) {
          log.info("Out of bound radius value");
          return;
        }
        OperationMode modeback = this.mode;
        setMode(OperationMode.CONFIG);
        i2cWrite(register.MAG_RADIUS_LSB, (byte) (radius & 0xFF));
        i2cWrite(register.MAG_RADIUS_MSB, (byte) ((radius >> 8) & 0xFF));
        setMode(modeback);
        return;
      }
      default: {
        return;
      }
    }
  }

  public int getRadius(Device device) {
    OperationMode modeback = this.mode;
    setMode(OperationMode.CONFIG);
    switch (device) {
      case ACCELEROMETER: {
        int retval = (i2cWriteReadRegByte(register.ACC_RADIUS_LSB) & 0xFF) + ((i2cWriteReadRegByte(register.ACC_RADIUS_MSB)) << 8);
        setMode(modeback);
        return retval;
      }
      case MAGNETOMETER: {
        int retval = (i2cWriteReadRegByte(register.MAG_RADIUS_LSB) & 0xFF) + ((i2cWriteReadRegByte(register.MAG_RADIUS_MSB)) << 8);
        setMode(modeback);
        return retval;
      }
      default: {
        setMode(modeback);
        return 0;
      }
    }
  }

  public void enableInterrupt(InterruptType interruptType) {
    byte int_en = (byte) (i2cWriteReadRegByte(register.INT_EN) | (0xFF & interruptType.mask));
    byte int_msk = (byte) (i2cWriteReadRegByte(register.INT_MSK) | (0xFF & interruptType.mask));
    i2cWrite(register.INT_EN, int_en);
    i2cWrite(register.INT_MSK, int_msk);
  }

  public void disableInterrupt(InterruptType interruptType) {
    byte int_en = (byte) (i2cWriteReadRegByte(register.INT_EN) & (~interruptType.mask));
    byte int_msk = (byte) (i2cWriteReadRegByte(register.INT_MSK) & (~interruptType.mask));
    i2cWrite(register.INT_EN, int_en);
    i2cWrite(register.INT_MSK, int_msk);
  }

  public void enableInterrupt(InterruptType it, boolean xAxis, boolean yAxis, boolean zAxis, float threshold, int duration) {
    byte int_en = (byte) (i2cWriteReadRegByte(register.INT_EN) | (0xFF & it.mask));
    byte int_msk = (byte) (i2cWriteReadRegByte(register.INT_MSK) | (0xFF & it.mask));
    OperationMode modeback = this.mode;
    setMode(OperationMode.CONFIG);
    switch (it) {
      case ACC_NM:
      case ACC_SM: {
        byte acc_int_set = i2cWriteReadRegByte(register.ACC_INT_SETTINGS);
        if (xAxis)
          acc_int_set |= 0b100;
        else
          acc_int_set &= 0b11111011;
        if (yAxis)
          acc_int_set |= 0b1000;
        else
          acc_int_set &= 0b11110111;
        if (zAxis)
          acc_int_set |= 0b10000;
        else
          acc_int_set &= 0b11101111;
        i2cWrite(register.ACC_INT_SETTINGS, acc_int_set);
        AccelerometerConfig range = getAccRange();
        switch (range) {
          case RANGE_2G:
            threshold /= 3.91;
            break;
          case RANGE_4G:
            threshold /= 7.81;
            break;
          case RANGE_8G:
            threshold /= 15.63;
            break;
          case RANGE_16G:
            threshold /= 31.25;
            break;
          default:
            break;
        }
        i2cWrite(register.ACC_NM_THRES, (byte) ((int) threshold & 0xFF));
        byte acc_sm_set = 0;
        if (it == InterruptType.ACC_NM) {
          acc_sm_set |= 1;
          if (duration <= 16) {
            acc_sm_set = (byte) (((duration & 0b111) - 1) << 1);
          } else if (duration <= 80) {
            acc_sm_set = (byte) ((((duration - 20) / 4) << 1) & 1 << 5);
          } else if (duration <= 336) {
            acc_sm_set = (byte) ((((duration - 88) / 8) << 1) & 1 << 6);
          }
        } else {
          acc_sm_set |= (((duration - 1) & 0b11) << 1);
        }
        i2cWrite(register.ACC_NM_SET, acc_sm_set);
        break;
      }
      case ACC_AM: {
        AccelerometerConfig range = getAccRange();
        switch (range) {
          case RANGE_2G:
            threshold /= 3.91;
            break;
          case RANGE_4G:
            threshold /= 7.81;
            break;
          case RANGE_8G:
            threshold /= 15.63;
            break;
          case RANGE_16G:
            threshold /= 31.25;
            break;
          default:
            break;
        }
        i2cWrite(register.ACC_AM_THRES, (byte) ((int) threshold & 0xFF));
        byte acc_int_set = i2cWriteReadRegByte(register.ACC_INT_SETTINGS);
        if (xAxis)
          acc_int_set |= 0b100;
        else
          acc_int_set &= 0b11111011;
        if (yAxis)
          acc_int_set |= 0b1000;
        else
          acc_int_set &= 0b11110111;
        if (zAxis)
          acc_int_set |= 0b10000;
        else
          acc_int_set &= 0b11101111;
        acc_int_set &= 0b11111100;
        acc_int_set |= ((duration - 1) & 0b11);
        i2cWrite(register.ACC_INT_SETTINGS, acc_int_set);
        break;
      }
      case ACC_HG: {
        byte acc_int_set = i2cWriteReadRegByte(register.ACC_INT_SETTINGS);
        if (xAxis)
          acc_int_set |= 0b100000;
        else
          acc_int_set &= 0b11011111;
        if (yAxis)
          acc_int_set |= 0b1000000;
        else
          acc_int_set &= 0b10111111;
        if (zAxis)
          acc_int_set |= 0b10000000;
        else
          acc_int_set &= 0b01111111;
        i2cWrite(register.ACC_INT_SETTINGS, acc_int_set);
        duration = ((duration / 2) - 1) & 0xFF;
        i2cWrite(register.ACC_HG_DURATION, (byte) duration);
        AccelerometerConfig range = getAccRange();
        switch (range) {
          case RANGE_2G:
            threshold /= 7.81;
            break;
          case RANGE_4G:
            threshold /= 15.63;
            break;
          case RANGE_8G:
            threshold /= 31.25;
            break;
          case RANGE_16G:
            threshold /= 62.5;
            break;
          default:
            break;
        }
        i2cWrite(register.ACC_HG_THRES, (byte) ((int) threshold & 0xFF));
        break;

      }
      case GYR_HR: {
        byte gyr_int_set = i2cWriteReadRegByte(register.GYR_INT_SETTING);
        if (xAxis)
          gyr_int_set |= 0b1000;
        else
          gyr_int_set &= 0b11110111;
        if (yAxis)
          gyr_int_set |= 0b10000;
        else
          gyr_int_set &= 0b11101111;
        if (zAxis)
          gyr_int_set |= 0b100000;
        else
          gyr_int_set &= 0b11011111;
        i2cWrite(register.GYR_INT_SETTING, gyr_int_set);
        duration = ((int) (duration / 2.5) - 1) & 0xFF;
        i2cWrite(register.GYR_DUR_X, (byte) duration);
        i2cWrite(register.GYR_DUR_Y, (byte) duration);
        i2cWrite(register.GYR_DUR_Z, (byte) duration);
        GyroscopeConfig range = getGyrRange();
        switch (range) {
          case RANGE_125DPS:
            threshold /= 3.91;
            break;
          case RANGE_250DPS:
            threshold /= 7.81;
            break;
          case RANGE_500DPS:
            threshold /= 15.63;
            break;
          case RANGE_1000DPS:
            threshold /= 31.25;
            break;
          case RANGE_2000DPS:
            threshold /= 62.5;
          default:
            break;
        }
        i2cWrite(register.GYR_HR_X_SET, (byte) ((int) threshold & 0b11111));
        i2cWrite(register.GYR_HR_Y_SET, (byte) ((int) threshold & 0b11111));
        i2cWrite(register.GYR_HR_Z_SET, (byte) ((int) threshold & 0b11111));
        break;
      }
      case GYR_AM: {
        byte gyr_int_set = i2cWriteReadRegByte(register.GYR_INT_SETTING);
        if (xAxis)
          gyr_int_set |= 0b1;
        else
          gyr_int_set &= 0b11111110;
        if (yAxis)
          gyr_int_set |= 0b10;
        else
          gyr_int_set &= 0b11101101;
        if (zAxis)
          gyr_int_set |= 0b100;
        else
          gyr_int_set &= 0b11011011;
        // gyr_int_set |= 0b1000000;
        i2cWrite(register.GYR_INT_SETTING, gyr_int_set);
        duration = ((int) (duration / 4) - 1) & 0b11;
        // duration |= 0b1100;
        i2cWrite(register.GYR_AM_SET, (byte) duration);
        GyroscopeConfig range = getGyrRange();
        switch (range) {
          case RANGE_125DPS:
            threshold /= 0.065;
            break;
          case RANGE_250DPS:
            threshold /= 0.125;
            break;
          case RANGE_500DPS:
            threshold /= 0.25;
            break;
          case RANGE_1000DPS:
            threshold /= 0.5;
            break;
          case RANGE_2000DPS:
            threshold /= 1;
          default:
            break;
        }
        i2cWrite(register.GYR_AM_THRES, (byte) ((int) threshold & 0xFF));
      }
    }
    i2cWrite(register.INT_EN, int_en);
    i2cWrite(register.INT_MSK, int_msk);
    setMode(modeback);
  }

  public AccelerometerConfig getAccRange() {
    byte acc_config = i2cWriteReadRegByte(register.ACC_CONFIG);
    acc_config &= AccelerometerConfig.RANGE_2G.mask;
    acc_config = (byte) (acc_config >> AccelerometerConfig.RANGE_2G.shift);
    for (AccelerometerConfig c : AccelerometerConfig.values()) {
      if (c.value == acc_config)
        return c;
    }
    return null;
  }

  public GyroscopeConfig getGyrRange() {
    byte gyr_config = i2cWriteReadRegByte(register.GYR_CONFIG_0);
    gyr_config &= GyroscopeConfig.RANGE_125DPS.mask;
    gyr_config = (byte) (gyr_config >> GyroscopeConfig.RANGE_125DPS.shift);
    for (GyroscopeConfig c : GyroscopeConfig.values()) {
      if (c.value == gyr_config)
        return c;
    }
    return null;
  }

  public byte getInterruptStatus() {
    return i2cWriteReadRegByte(register.INT_STA);
  }

  public byte getPOSTResult() {
    byte st_result = i2cWriteReadRegByte(register.ST_RESULT);
    if ((st_result & 0b1) > 0)
      log.info("Accelerator test passed");
    else
      log.info("Accelerator test failed");
    if ((st_result & 0b10) > 0)
      log.info("Magnetometer test passed");
    else
      log.info("Magnetometer test failed");
    if ((st_result & 0b100) > 0)
      log.info("Gyroscope test passed");
    else
      log.info("Gyroscope test failed");
    if ((st_result & 0b1000) > 0)
      log.info("Controller test passed");
    else
      log.info("Controller test failed");
    return st_result;
  }

  public byte testBNO055() {
    OperationMode modeback = this.mode;
    setMode(OperationMode.CONFIG);
    byte sys_trigger = i2cWriteReadRegByte(register.SYS_TRIGGGER);
    sys_trigger |= 1;
    i2cWrite(register.SYS_TRIGGGER, sys_trigger);
    sleep(1000);
    byte st_result = i2cWriteReadRegByte(register.ST_RESULT);
    if ((st_result & 0b1) > 0)
      log.info("Accelerator test passed");
    else
      log.info("Accelerator test failed");
    if ((st_result & 0b10) > 0)
      log.info("Magnetometer test passed");
    else
      log.info("Magnetometer test failed");
    if ((st_result & 0b100) > 0)
      log.info("Gyroscope test passed");
    else
      log.info("Gyroscope test failed");
    setMode(modeback);
    return st_result;
  }

  public void showCalibrationStatus() {
    byte calib_stat = i2cWriteReadRegByte(register.CALIB_STAT);
    log.info("Calibration level for Accelerator: {}", (calib_stat & 0b1100) >> 2);
    log.info("Calibration level for Magnetometer: {}", calib_stat & 0b11);
    log.info("Calibration level for Gyroscope: {}", (calib_stat & 0b110000) >> 4);
    log.info("Calibration level for System: {}", (calib_stat & 0b11000000) >> 6);
  }

  public void showSystemStatus() {
    byte sys_stat = i2cWriteReadRegByte(register.SYS_STATUS);
    switch (sys_stat) {
      case 0x00: {
        log.info("System Idle");
        break;
      }
      case 0x01: {
        log.info("System Error");
        showSystemError();
        break;
      }
      case 0x02: {
        log.info("Initializing peripherals");
        break;
      }
      case 0x03: {
        log.info("System Initialization");
        break;
      }
      case 0x04: {
        log.info("Executing self test");
        break;
      }
      case 0x05: {
        log.info("Sensor fusion algorithm running");
        break;
      }
      case 0x06: {
        log.info("System running without fusion algorithm");
        break;
      }
      default: {
        log.info("Unknow");
      }
    }
  }

  public void showSystemError() {
    byte sys_err = i2cWriteReadRegByte(register.SYS_ERR);
    switch (sys_err) {
      case 0x00: {
        log.info("No error");
        break;
      }
      case 0x01: {
        log.info("Peripheral initialization error");
        showSystemError();
        break;
      }
      case 0x02: {
        log.info("System initialization error");
        break;
      }
      case 0x03: {
        log.info("Self test result failed");
        break;
      }
      case 0x04: {
        log.info("Register map value out of range");
        break;
      }
      case 0x05: {
        log.info("Register map address out of range");
        break;
      }
      case 0x06: {
        log.info("Register map write error");
        break;
      }
      case 0x07: {
        log.info("BNO low power mode not available for selected operation mode");
        break;
      }
      case 0x08: {
        log.info("Accelerometer power mode not available");
        break;
      }
      case 0x09: {
        log.info("Fusion algorithm configuration error");
        break;
      }
      case 0x0A: {
        log.info("Sensor configuration error");
        break;
      }
      default: {
        log.info("Unknow");
      }
    }
  }

  public void attachInterruptPin(PinArrayControl control, int pin) {
    pinControl = control;
    this.pin = pin;
    control.attach(this, pin);
  }

  @Override
  public void onPin(PinData pindata) {

    boolean sense = (pindata.value != 0);

    if (!isActive && sense) {
      // state change
      isActive = true;
      int data = 1;// (int)(i2cWriteReadRegByte(register.INT_STA))
      invoke("publishInterrupt", data);
    }
  }

  public void disablePin() {
    if (pinControl == null) {
      error("pin control not set");
      return;
    }

    if (pin == null) {
      error("pin not set");
      return;
    }

    pinControl.disablePin(pin);
    isEnabled = false;
    broadcastState();
  }

  public void enablePin() {
    if (pinControl == null) {
      error("pin control not set");
      return;
    }

    if (pin == null) {
      error("pin not set");
      return;
    }

    pinControl.enablePin(pin, 10);
    isEnabled = true;
    broadcastState();
  }

  public int publishInterrupt(int data) {
    return data;
  }

  public void resetInterrupt() {
    byte sys_trigger = 1 << 6;
    i2cWrite(register.SYS_TRIGGGER, sys_trigger);
    isActive = false;
  }

  /**
   * GOOD DESIGN - this method is the same pretty much for all Services could be
   * a Java 8 default implementation to the interface
   */
  @Override
  public boolean isAttached(String name) {
    return (controller != null && controller.getName().equals(name));
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
}
