package org.myrobotlab.service;

import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractMotor;
import org.myrobotlab.service.config.MotorConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.MotorController;

/**
 * A general motor implementation with a "simple H-bridge" where one control
 * line is power with pwm and the other control line is determines direction of
 * spin.
 * 
 */

public class Motor extends AbstractMotor {

  private static final long serialVersionUID = 1L;

  String pwrPin;
  String dirPin;
  Integer pwmFreq;

  public Motor(String n, String id) {
    super(n, id);
  }

  public void setPwrDirPins(String pwrPin, String dirPin) {
    this.pwrPin = pwrPin;
    this.dirPin = dirPin;
    broadcastState();
  }

  public void setPwrDirPins(int pwrPin, int dirPin) {
    this.pwrPin = pwrPin + "";
    this.dirPin = dirPin + "";
    broadcastState();
  }

  public String getPwrPin() {
    return pwrPin;
  }

  public void setPwrPin(Integer pwrPin) {
    this.pwrPin = pwrPin + "";
  }

  public void setPwrPin(String pwrPin) {
    this.pwrPin = pwrPin;
  }

  public void setDirPin(String dirPin) {
    this.dirPin = dirPin;
  }

  public String getDirPin() {
    return dirPin;
  }

  public void setDirPin(Integer dirPin) {
    this.dirPin = dirPin + "";
  }

  public Integer getPwmFreq() {
    return pwmFreq;
  }

  public void setPwmFreq(Integer pwmfreq) {
    this.pwmFreq = pwmfreq;
  }

  @Override
  public ServiceConfig getConfig() {
    // FIXME - may need to do call super.config for config that has parent :(
    MotorConfig config = new MotorConfig();
    config.dirPin = getDirPin();
    config.pwrPin = getPwrPin();
    config.pwmFreq = getPwmFreq();
    return config;
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    super.apply(c);
    MotorConfig config = (MotorConfig) c;
    if (config.pwrPin != null) {
      setPwrPin(pwrPin);
    }
    if (config.dirPin != null) {
      setDirPin(dirPin);
    }
    if (config.pwmFreq != null) {
      setPwmFreq(pwmFreq);
    }
    return c;
  }

  public static void main(String[] args) {

    LoggingFactory.init("info");

    try {

      Runtime.start("gui", "SwingGui");
      Runtime.start("webgui", "WebGui");
      Runtime.start("motor", "Motor");
      Runtime.start("arduino", "Arduino");
      boolean done = true;
      if (done) {
        return;
      }

      // FIXME - all testing or replacing of main code should be new JUnit
      // tests - with virtual arduino !!!)
      String port = "COM15";

      // Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      // Runtime.createAndStart("gui", "SwingGui");
      // Runtime.createAndStart("webgui", "WebGui");
      // arduino.setBoard(Arduino.BOARD_TYPE_ATMEGA2560);
      // arduino.connect(port);
      // arduino.broadcastState();

      // Runtime.createAndStart("python", "Python");

      int pwmPin = 6;
      int dirPin = 7;

      // int leftPwm = 6;
      // int rightPwm = 7;

      // virtual hardware
      /*
       * VirtualDevice virtual = (VirtualDevice)Runtime.start("virtual",
       * "VirtualDevice"); virtual.createVirtualArduino(port);
       */

      // int encoderPin= 7;
      Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
      arduino.connect(port);

      arduino.pinMode(6, Arduino.OUTPUT);
      arduino.pinMode(7, Arduino.OUTPUT);

      arduino.digitalWrite(7, 1);
      // arduino.digitalWrite(6, 1);

      arduino.analogWrite(6, 255);
      arduino.analogWrite(6, 200);
      arduino.analogWrite(6, 100);
      arduino.analogWrite(6, 0);

      Motor m1 = (Motor) Runtime.start("m1", "Motor");

      /*
       * m1.setType2Pwm(leftPwm, rightPwm); m1.setTypeStepper();
       * m1.setTypePulseStep(pwmPin, dirPin);
       */
      // Runtime.start("webgui", "WebGui");
      // m1.attach(arduino, Motor.TYPE_PULSE_STEP, pwmPin, dirPin);
      // m1.attach(arduino, Motor.TYPE_2_PWM, pwmPin, dirPin);
      // m1.attach(arduino, Motor.TYPE_SIMPLE, pwmPin, dirPin);
      m1.attachMotorController(arduino);

      m1.move(1.0);
      m1.move(-1.0);

      arduino.enableBoardInfo(true);
      arduino.enableBoardInfo(false);
      m1.stop();
      m1.move(0.5);
      m1.stop();

      // Runtime.start("webgui", "WebGui");

      // arduino.motorAttach("m1", 8, 7, 54);
      // m1.setType(Motor.TYPE_PWM_DIR_FE);
      // arduino.setSampleRate(8000);
      // m1.setSpeed(0.95);
      /*
       * arduino.motorAttach("m1", Motor.TYPE_FALSE_ENCODER, 8, 7);
       * m1.moveTo(30); m1.moveTo(230); m1.moveTo(430); m1.moveTo(530);
       * m1.moveTo(130); m1.moveTo(330);
       */
      // with encoder
      // m1.moveTo(600);

      /*
       * m1.stop(); m1.move(0.94); m1.stop(); m1.move(-0.94); m1.stop();
       * 
       * // arduino.motorAttach("m1", 8, 7, 54) ;
       * 
       * m1.moveTo(600f);
       */
    } catch (Exception e) {
      Logging.logError(e);
    }

  }

}
