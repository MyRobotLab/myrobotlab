package org.myrobotlab.service;

import static org.myrobotlab.service.Adafruit16CServoDriver.SERVOMAX;
import static org.myrobotlab.service.Adafruit16CServoDriver.SERVOMIN;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.FileIO.FileComparisonException;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SerialDevice;

@Ignore
public class Adafruit16CServoDriverTest {

  static Adafruit16CServoDriver driver = null;
  static Arduino arduino = null;
  static SerialDevice serial = null;
  static VirtualDevice virtual = null;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    LoggingFactory.init("INFO");
    driver = (Adafruit16CServoDriver) Runtime.start("driver", "Adafruit16CServoDriver");
    // arduino = driver.getArduino();
    arduino = (Arduino) Runtime.start("arduino", "Arduino");
    serial = arduino.getSerial();
  }

  @Test
  public final void test() throws IOException, FileComparisonException {
    // virtual.create
    virtual.createVirtualSerial("v1");
    // FIXME - make virtual UART

    Serial uart = virtual.getUart("v1");
    uart.open("v1");
    uart.record();
    arduino.connect("v0");

    driver.setServo(0, SERVOMIN);
    driver.setServo(0, SERVOMAX);
    driver.setServo(0, SERVOMIN);
    driver.setServo(0, SERVOMAX);
    driver.setServo(0, SERVOMIN);
    driver.setServo(0, SERVOMAX);
    driver.setServo(0, SERVOMIN);
    driver.setServo(0, SERVOMAX);
    driver.setServo(0, SERVOMIN);
    driver.setServo(0, SERVOMAX);
    driver.setServo(0, SERVOMIN);
    driver.setServo(0, SERVOMAX);

    // begin();
    driver.setPWMFreq(0, 60);

    for (int i = SERVOMIN; i < SERVOMAX; ++i) {
      driver.setPWM(0, 0, i);
    }

    driver.setPWM(0, 0, 0);

    driver.setPWM(0, 0, SERVOMIN);
    driver.setPWM(0, 0, SERVOMAX);
    driver.setPWM(0, 0, SERVOMIN);
    driver.setPWM(0, 0, SERVOMAX);
    driver.setPWM(0, 0, SERVOMIN);
    driver.setPWM(0, 0, SERVOMAX);
    driver.setPWM(0, 0, SERVOMAX);

    // need to allow time :P
    // to flush serial thread
    // sleep(1000);
    // disconnect / close arduino port
    // flush cable
    // stop recording
    arduino.disconnect();
    // cable.close();
    uart.stopRecording();

    FileIO.compareFiles("test/Adafruit16CServoDriver/test.rx", "test/Adafruit16CServoDriver/control/test.rx");

  }

}