/**
 *                    
 * @author Christian Beliveau
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

import java.util.List;

import org.myrobotlab.service.Neopixel;

public interface NeopixelController extends DeviceController {
  
  /**
   * high level "attach" which internally will call attachDevice(Device device, int[] config)
   * 
   * @param neopixel
   * @param numPixel - All of the config needed for the device -numPixel=number of pixel of the neopixel hardware 
   * @param pin -   
   */
  public void attach(Neopixel neopixel, int pin, int  numPixel);
  
  /**
   * high level "detach" with internally will call detachDevice(Device device)    * 
   * @param neopixel
   */
  public void detach(Neopixel neopixel);
  
  public void neopixelWriteMatrix(Neopixel neopixel, List<Integer> msg);
  
}
