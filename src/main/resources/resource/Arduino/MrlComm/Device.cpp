#include "ArduinoMsgCodec.h"
#include "Msg.h"
#include "Device.h"

Device::Device(byte deviceId, byte deviceType) {
  id = deviceId;
  type = deviceType;
  msg = Msg::getInstance();
}
