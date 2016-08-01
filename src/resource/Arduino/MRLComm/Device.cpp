#include "Device.h"

Device::Device(int deviceType) {
  type = deviceType;
}

void Device::attachDevice() {
  id = nextDeviceId;
  nextDeviceId++;
}

unsigned int Device::nextDeviceId=1; // device 0 is Arduino

/**
 * deviceAttach: virtual function, will only execute if a class that inherit
 * Device.h have not implemented deviceAttach
 */
bool Device::deviceAttach(unsigned char config[], int configSize) {
  return false; 
}
