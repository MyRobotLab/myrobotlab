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

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.service.NeoPixel;

public interface NeoPixelController extends Attachable {
  
  public void neoPixelAttach(NeoPixel neopixel, int pin, int numberOfPixels);
  public void neoPixelWriteMatrix(NeoPixel neopixel, List<Integer> msg);
  public void neoPixelSetAnimation(NeoPixel neopixel, int animation, int red, int green, int blue, int speed);
  
}
