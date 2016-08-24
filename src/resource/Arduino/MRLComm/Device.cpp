#include "Device.h"

Device::Device(int deviceType) {
  type = deviceType;
}

void Device::attachDevice() {
  id = nextDeviceId;
  nextDeviceId++;
}

int Device::nextDeviceId=1; // device 0 is Arduino

bool Device::deviceAttach(unsigned char[], int) {
  return false; 
}
