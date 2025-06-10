package org.myrobotlab.service;

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.math3.util.Precision;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.sensor.EncoderListener;
import org.myrobotlab.sensor.EncoderPublisher;
import org.myrobotlab.service.abstracts.AbstractPinEncoder;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.EncoderControl;

/**
 * AS5048A - SPI based 14 bit magnetic absolute position encoder.
 * 
 * @author kwatters
 *
 */
public class As5048AEncoder extends AbstractPinEncoder<ServiceConfig> implements EncoderControl, EncoderPublisher {

  private static final int HISTORY_SIZE = 5;

  private static final long serialVersionUID = 1L;

  private LinkedBlockingQueue<EncoderData> history = new LinkedBlockingQueue<EncoderData>(HISTORY_SIZE);
  
  public As5048AEncoder(String n, String id) {
    super(n, id);
    // 14 bit encoder is 2^16 steps of resolution
    resolution = 4096 * 4;
  }

  @Override
  public void setZeroPoint() {
    log.warn("Setting the Zero point not supported on AS5048A because memory register is OTP");
  }

  public static void main(String[] args) throws Exception {

    LoggingFactory.init("INFO");
    String port = "COM4";
    Runtime.start("gui", "SwingGui");
    Arduino ard = (Arduino) Runtime.start("ard", "Arduino");
    ard.connect(port);
    ard.setDebug(true);
    As5048AEncoder encoder = (As5048AEncoder) Runtime.start("encoder", "As5048AEncoder");
    encoder.setPin(10);
    ard.attachEncoderControl(encoder);
    Thread.sleep(10000);
    encoder.setZeroPoint();
    log.info("Here we are..");
  }
  
  public void updateEncoderData(EncoderData data) {
    // publish the updated encoder data (this is updated from the arduino..)
    // log.info("Encoder data: {}", data);
    // pop the first value
    if (history.remainingCapacity() == 0) {
      history.poll();
    }
    history.offer(data);
    // This is computing a moving average for the encoder value to smooth it out a bit.
    double avgVal = history.stream().mapToDouble(EncoderData::getValue).average().orElse(0.0);
    double avgAngle = Precision.round(history.stream().mapToDouble(EncoderData::getAngle).average().orElse(0.0), 1);    
    
   // Precision.round(avgAngle, 2);
   // double avgVal = (previousData.value + data.value)/2;
   // double avgAngle = (previousData.angle + data.angle)/2;
    // 0.1 degree resolution... truncate and filter the value for stability..
    EncoderData filteredData = new EncoderData( data.source, data.pin, avgVal, avgAngle);
   //  log.info("Original Angle: {}  Filtered Angle: {}", data.angle, filteredData.angle);
    
    // previousData = data;
    //invoke("publishEncoderData", data); 

    invoke("publishEncoderData", filteredData); 
  }

}
