package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.PinData;

/**
 * 
 * @author GroG
 *
 */
public interface PinArrayPublisher extends NameProvider {

  public void attachPinArrayListener(PinArrayListener listener);

  public PinData[] publishPinArray(PinData[] pinData);

}
