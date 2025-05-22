package org.myrobotlab.sensor;

public interface EncoderListener {  
  
  public String getName();

  public void onEncoderData(EncoderData encoderData);
  
  default public void attachEncoderPublisher(EncoderPublisher publisher) {
    attachEncoderPublisher(publisher.getName());
  }

  default public void attachEncoderPublisher(String name) {
    send(name, "attachEncoderListener", getName());
  }

  default public void detachEncoderPublisher(EncoderPublisher publisher) {
    detachEncoderPublisher(publisher.getName());
  }

  default public void detachEncoderPublisher(String name) {
    send(name, "detachEncoderListener", getName());
  }

  public void send(String name, String method, Object... data);

}
