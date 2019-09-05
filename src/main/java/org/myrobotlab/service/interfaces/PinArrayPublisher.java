package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.PinData;

/**
 * 
 * @author GroG
 *
 */
public interface PinArrayPublisher extends NameProvider {

  public void attach(PinArrayListener listener);
  
  public PinData[] publishPinArray(PinData[] pinData);
  
}
