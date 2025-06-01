package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.AnalogData;

public interface AnalogPublisher extends NameProvider {

  public AnalogData publishAnalog(AnalogData data);

  public void attachAnalogListener(AnalogListener listener);

  public void detachAnalogListener(AnalogListener listener);

}
