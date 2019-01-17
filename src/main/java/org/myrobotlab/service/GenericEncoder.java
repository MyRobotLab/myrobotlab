package org.myrobotlab.service;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractEncoder;
import org.myrobotlab.service.interfaces.EncoderControl;

/**
 *
 * Generic Encoder is a generalized encoder which can be configured on startup
 * 
 * @author kwatters
 *
 */
public class GenericEncoder extends AbstractEncoder implements EncoderControl {

  private static final long serialVersionUID = 1L;

  public GenericEncoder(String reservedKey) {
    super(reservedKey);
    // default is 12 bit encoder is 4096 steps of resolution
    resolution = 4096;
  }

  public void setResolution(int resolution) {
    this.resolution = resolution;
  }

  public void setMode(String mode) {
    // FIXME IMPLEMENT - (ABSOLUTE | ANALOG ?) | RELATIVE
  }

  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(GenericEncoder.class.getCanonicalName());
    meta.addDescription("a configurable generic encoder");
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
    GenericEncoder encoder = (GenericEncoder) Runtime.start("encoder", "GenericEncoder");
    encoder.pin = 3;
    ard.attach(encoder);
    Thread.sleep(10000);
    encoder.setZeroPoint();
    System.out.println("Here we are..");
  }

}
