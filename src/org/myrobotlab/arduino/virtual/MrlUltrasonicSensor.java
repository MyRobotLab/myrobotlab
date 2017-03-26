package org.myrobotlab.arduino.virtual;

import org.myrobotlab.arduino.VirtualMsg;
import org.myrobotlab.service.VirtualArduino;

public class MrlUltrasonicSensor extends Device {
  NewPing newping;
  private boolean isRanging;

  public class NewPing {

    public NewPing(int trigPin, int echoPin, int i) {
      // TODO Auto-generated constructor stub
    }

    public Integer ping_cm() {
      return MrlComm.getRandom(34, 98);
    }
  }

  public int getRandom(int min, int max) {
    return min + (int) (Math.random() * ((max - min) + 1));
  }
  
  String F(String x){
    return x;
  }

  String String(int x) {
    return "" + (x);
  }

  public MrlUltrasonicSensor(int deviceId, VirtualArduino virtual) {
    super(deviceId, VirtualMsg.DEVICE_TYPE_ULTRASONICSENSOR, virtual);
    msg.publishDebug("ctor NewPing " + String(deviceId));
  }

  void attach(int trigPin, int echoPin) {
    msg.publishDebug("Ultrasonic.attach " + String(trigPin) + " " + String(echoPin));
    newping = new NewPing(trigPin, echoPin, 500);
  }

  void startRanging() {
    msg.publishDebug("Ultrasonic.startRanging");
    // this should be public in NewPing
    // newping.set_max_distance(maxDistanceCm);
    isRanging = true;
  }

  void stopRanging() {
    msg.publishDebug(F("Ultrasonic.stopRanging"));
    isRanging = false;
  }

  void update() {
    if (!isRanging) {
      return;
    }
    msg.publishUltrasonicSensorData(id, newping.ping_cm());
  }

}
