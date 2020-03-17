package org.myrobotlab.service;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractPinEncoder;
import org.myrobotlab.service.interfaces.EncoderControl;

/**
 * AS5048A - SPI based 14 bit magnetic absolute position encoder.
 * 
 * @author kwatters
 *
 */
public class As5048AEncoder extends AbstractPinEncoder implements EncoderControl {

  private static final long serialVersionUID = 1L;

  public As5048AEncoder(String n, String id) {
    super(n, id);
    // 14 bit encoder is 2^16 steps of resolution
    resolution = 4096*4;
  }
  
  @Override
  public void setZeroPoint() {
    log.warn("Setting the Zero point not supported on AS5048A because memory register is OTP");
  }

  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(As5048AEncoder.class.getCanonicalName());
    meta.addDescription("AS5048A Encoder - 14 bit - Absolute position encoder");
    meta.addCategory("encoder", "sensors");
    return meta;
  }

  public static void main(String[] args) throws Exception {

    LoggingFactory.init("INFO");

    String port = "COM3";
    Runtime.start("gui", "SwingGui");
    Arduino ard = (Arduino) Runtime.start("ard", "Arduino");
    ard.connect(port);
    ard.setDebug(true);
    As5048AEncoder encoder = (As5048AEncoder) Runtime.start("encoder", "As5048AEncoder");
    encoder.setPin(10);
    ard.attach(encoder);
    Thread.sleep(10000);
    encoder.setZeroPoint();
    
    log.info("Here we are..");
  }

}
