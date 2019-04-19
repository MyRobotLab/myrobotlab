package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.EncoderController;
import org.myrobotlab.service.interfaces.EncoderListener;
import org.slf4j.Logger;

/**
 * Service to encode a stream of pulses (absolute or relative) for other
 * services. This can handle multiple streams of pulses to multiple services -
 * ie typically only one is needed for all TimeEncoding purposes.
 * 
 * Because this is implemented without hardware it does not need to be attached
 * to a EncoderController
 * 
 * @author GroG
 *
 */
public class TimeEncoder extends Service implements EncoderControl {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(TimeEncoder.class);
  
  Map<String, EncoderListener> listeners = new HashMap<>();
  
  public class EncoderConfig {
    String type; // FIXME - make enum ... absolute | relative | other ..
    Long intervalMs;
  }

  public TimeEncoder(String n) {
    super(n);
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(TimeEncoder.class);
    meta.addDescription("general purpose timing encoder used in place of real hardware");
    meta.setAvailable(true);
    meta.setAvailable(false);
    meta.addCategory("general");
    return meta;
  }

  @Override // from EncoderController
  public EncoderData publishEncoderData(EncoderData data) {
    // TODO Auto-generated method stub
    return null;
  }

  public void attach(EncoderController controller) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void disable() {
    // TODO Auto-generated method stub

  }

  @Override
  public void enable() {
    // TODO Auto-generated method stub

  }

  @Override
  public Boolean isEnabled() {
    // TODO Auto-generated method stub
    return null;
  }
}