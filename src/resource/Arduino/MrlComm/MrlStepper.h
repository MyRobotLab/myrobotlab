#ifndef MrlStepper_h
#define MrlStepper_h

/**
 * Stepper Device
 */
 
class MrlStepper : public Device {
  // details of the different motor controls/types
  public:
    MrlStepper() : Device(DEVICE_TYPE_STEPPER) {

    }
    void update() {
    }
};


#endif
