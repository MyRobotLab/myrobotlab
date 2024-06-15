package org.myrobotlab.service;

import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractPinEncoder;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.EncoderControl;

/**
 * AS5048A - SPI based 14 bit magnetic absolute position encoder.
 * 
 * @author kwatters
 *
 */
public class As5048AEncoder extends AbstractPinEncoder<ServiceConfig> implements EncoderControl {

  private static final long serialVersionUID = 1L;

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

}
