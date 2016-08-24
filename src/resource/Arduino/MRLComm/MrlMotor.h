#ifndef MrlMotor_h
#define MrlMotor_h

/**
 * Motor Device
 */
 
#include "Device.h" 
 
class MrlMotor : public Device {
  // details of the different motor controls/types
  public:
    MrlMotor() : Device(DEVICE_TYPE_MOTOR) {
      // TODO: implement classes or custom control for different motor controller types
      // usually they require 2 PWM pins, direction & speed...  sometimes the logic is a
      // little different to drive it.
      // GroG: 3 Motor Types so far - probably a single MrlMotor could handle it
      // they are MotorDualPwm MotorSimpleH and MotorPulse
      // Stepper should be its own MrlStepper
    }
    void update() {
      // we should update the pwm values for the control of the motor device  here
      // this is potentially where the hardware specific logic could go for various motor controllers
      // L298N vs IBT2 vs other...  maybe consider a subclass for the type of motor-controller.
      // GroG : All motor controller I know of which can be driven by the Arduino fall in the
      // MotorDualPwm MotorSimpleH and MotorPulse - categories
    }
};

#endif
