package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.service.interfaces.EncoderControl;
import org.myrobotlab.service.interfaces.EncoderController;
import org.slf4j.Logger;

public class TimeEncoder extends Service implements EncoderController {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(TimeEncoder.class);
  
  // Map<String, PinDefinition>

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
    meta.addDescription("used as a general template");
    meta.setAvailable(true);
    meta.setAvailable(false);
    meta.addCategory("general");
    return meta;
  }

  @Override // from EncoderController
  public void attach(EncoderControl control, Integer pin) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override // from EncoderController
  public EncoderData publishEncoderPosition(EncoderData data) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override // from EncoderController
  public void setZeroPoint(EncoderControl encoder) {
    // TODO Auto-generated method stub
    
  }
}