/**
 *                    
 * @author Christian Beliveau
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

import org.myrobotlab.framework.interfaces.NameProvider;

public interface NeoPixelControl extends NameProvider {

  /**
   * explicit attach no additional parameters
   * 
   * @param controller
   *          attach this neopixel controller
   */
  public void attachNeoPixelController(NeoPixelController controller);

  /**
   * explicit detach
   * 
   * @param controller
   *          - detach this neopixel controller
   */
  public void detachNeoPixelController(NeoPixelController controller);

  /**
   * SetPixel: defining the pixels of the pixel matrix. Setting pixels are not
   * send directly to the Neopixel Hardware but send with the sendPixelMatrix()
   * method
   * 
   * @param address
   *          - address of neopixel pixel
   * @param red
   *          - color value 0 - 255
   * @param green
   *          - color value 0 - 255
   * @param blue
   *          - color value 0 - 255
   */
  public void setPixel(int address, int red, int green, int blue);

  /**
   * Send a matrix of pixels to the neopixel hardware
   */
  public void writeMatrix();

  public void setPin(String pin);
  
  public void setPin(int pin);

  public Integer getPin();

  public int getNumPixel();

  public void clear();

  public void playAnimation(String animation);

  public void setAnimation(int animation, int red, int green, int blue, int wait_ms);

  public void setAnimation(String animation, int red, int green, int blue, int wait_ms);

  public void setAnimationSetting(String animation);

}
