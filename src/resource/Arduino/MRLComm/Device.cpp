#include "Device.h"

Device::Device(int deviceType) {
  type = deviceType;
}

void Device::attachDevice() {
  id = nextDeviceId;
  nextDeviceId++;
}
int Device::nextDeviceId=0;

