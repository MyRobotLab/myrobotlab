/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.SensorEvent;

/** 
* Sensor Data Publisher Interface:
* not a service
* e.g. UltrasonicSensor /AnalogPinSensor / DigitialPinSensor
*/
public interface SensorDataPublisher {
  // this is what gets invoked in mrl
  public SensorEvent publishSensorData(SensorEvent data);
  // this is to attach something that listens to the output of this sensor data.
  // public void addSensorDataListener(SensorDataListener listener, int[] config);
 
  public String getName();
  // hmm. Should take a microcontroller?
  
}

