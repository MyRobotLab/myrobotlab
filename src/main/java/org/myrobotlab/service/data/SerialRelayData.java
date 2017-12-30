package org.myrobotlab.service.data;


public class SerialRelayData {
    public SerialRelayData(Integer deviceId, int[] data) {
      this.deviceId = deviceId;
      this.data = data;
    }
    public Integer deviceId = null;
    public int[] data;
  }
