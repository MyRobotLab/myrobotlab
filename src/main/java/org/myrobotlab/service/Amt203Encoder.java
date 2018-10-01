package org.myrobotlab.service;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractEncoder;
import org.myrobotlab.service.interfaces.EncoderControl;

/**
 * This is the service that will support the AMT-203 encoder from CUI. It is a
 * capacitive 12 bit resolution encoder that operates with an SPI bus.
 * 
 * The "chip select" or CS/SS/SCB pin is connected to a digitial pin on the
 * arduino. When this pin is pulled low, it enables the device to make an SPI
 * data transfer request. The result is the angular position of the encoder. The
 * MrlAmt203Encoder converts the 12 bits into a floating point value scaled from
 * 0 to 360.
 * 
 * More info here:
 * http://myrobotlab.org/content/code-cui-amt203-absolute-encoder
 * http://forum.arduino.cc/index.php?topic=158790.0
 * https://www.cui.com/product/motion/rotary-encoders/absolute/modular/amt20-series
 * 
 * @author kwatters
 *
 */
public class Amt203Encoder extends AbstractEncoder implements EncoderControl {

  private static final long serialVersionUID = 1L;

  public Amt203Encoder(String reservedKey) {
    super(reservedKey);
    // 12 bit encoder is 4096 steps of resolution
    resolution = 4096;
  }

  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(Amt203Encoder.class.getCanonicalName());
    meta.addDescription("AMT203 Encoder - Absolute position encoder");
    meta.addCategory("encoder", "sensor");
    return meta;
  }

  public static void main(String[] args) throws Exception {

    LoggingFactory.init("INFO");

    String port = "COM4";
    Runtime.start("gui", "SwingGui");
    Arduino ard = (Arduino) Runtime.start("ard", "Arduino");
    ard.connect(port);
    ard.setDebug(true);
    Amt203Encoder encoder = (Amt203Encoder) Runtime.start("encoder", "Amt203Encoder");
    encoder.pin = 3;
    ard.attach(encoder);
    Thread.sleep(10000);
    encoder.setZeroPoint();
    System.out.println("Here we are..");
  }

}
