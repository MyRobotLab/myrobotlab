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

import org.myrobotlab.framework.interfaces.Attachable;

public interface NeoPixel2Controller extends Attachable {

  /**
   * Attach to a neopixel
   * 
   * @param neopixel
   * @param pin
   * @param numberOfPixels
   * @param depth 3 RGB 4 RGBW
   */
  public void neoPixel2Attach(String neopixel, int pin, int numberOfPixels, int depth);

  /**
   * Write a series of bytes to the neopixel
   * Format is 5 bytes for each pixel - since the address is sent with each pixel - optimizations
   * "could" be done for updating only the changing pixels in an animation
   * 
   * |address | red | green | blue | white  
   * 
   * @param neopixel
   * @param buffer
   */
  public void neoPixel2WriteMatrix(String neopixel, int[] buffer);

  /**
   * Sets an "onboard" animation's color and speed values then starts the animation
   * 
   * @param neopixel
   * @param animation
   * @param red
   * @param green
   * @param blue
   * @param white
   * @param wait_ms
   */
  public void neoPixel2SetAnimation(String neopixel, int animation, int red, int green, int blue, int white, int wait_ms);
  
  /**
   * Optimized fill of a continuous segment of pixels
   * 
   * @param neopixel
   * @param beginAddress
   * @param count
   * @param red
   * @param green
   * @param blue
   * @param white
   */
  public void neoPixel2Fill(String neopixel, int beginAddress, int count, int red, int green, int blue, int white);

  /**
   * Sets the brightness of all pixels
   * @param neopixel
   * @param brightness
   */
  public void neoPixel2SetBrightness(String neopixel, int brightness);
  
  /**
   * Optimize clear of all the pixels, does a memset on board
   */
  public void neoPixel2Clear(String neopixel);

}
