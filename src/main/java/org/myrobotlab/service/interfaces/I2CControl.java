/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.NameProvider;

public interface I2CControl extends NameProvider, Attachable {

  @Deprecated /* use setBus */
  public void setDeviceBus(String deviceBus);

  @Deprecated /* use setAddress */
  public void setDeviceAddress(String deviceAddress);

  public void setBus(String bus);

  public void setAddress(String address);

  public String getBus();

  public String getAddress();

  @Deprecated /* use getBus */
  public String getDeviceBus();

  @Deprecated /* use getAddress */
  public String getDeviceAddress();

  public void attach(I2CController controller, String deviceBus, String deviceAddress);

  public void attachI2CController(I2CController controller);

  public void detachI2CController(I2CController controller);
}
