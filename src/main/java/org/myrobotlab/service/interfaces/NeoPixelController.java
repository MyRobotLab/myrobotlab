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

public interface NeoPixelController extends Attachable {

  /**
   * Attach to a neopixel
   * 
   * @param neopixel
   *          - the neopixle service
   * @param pin
   *          - pin number it attached to on the controller
   * @param numberOfPixels
   *          - number of pixels it has
   * @param depth
   *          either 3 RGB or 4 RGBW
   */
  public void neoPixelAttach(String neopixel, int pin, int numberOfPixels, int depth);

  /**
   * Write a series of bytes to the neopixel Format is 5 bytes for each pixel -
   * since the address is sent with each pixel - optimizations "could" be done
   * for updating only the changing pixels in an animation
   * 
   * |address | red | green | blue | white
   * 
   * @param neopixel
   *          - neopixel service
   * @param buffer
   *          - array of pixel values and addresses sent to MrlComm
   */
  public void neoPixelWriteMatrix(String neopixel, int[] buffer);

  /**
   * Sets an "onboard" animation's color and speed values then starts the
   * animation
   * 
   * @param neopixel
   *          - the neopixel service
   * @param animation
   *          - the index of the animation to play
   * @param red
   *          - value 0-255
   * @param green
   *          - value 0-255
   * @param blue
   *          - value 0-255
   * @param white
   *          - value 0-255
   * @param wait_ms
   *          - number of ms to wait in "show" pixels
   */
  public void neoPixelSetAnimation(String neopixel, int animation, int red, int green, int blue, int white, int wait_ms);

  /**
   * Optimized fill of a continuous segment of pixels
   * 
   * @param neopixel
   *          - the service
   * @param beginAddress
   *          - the begin address to fil
   * @param count
   *          - number of pixels to set after the begin address
   * @param red
   *          - value 0-255
   * @param green
   *          - value 0-255
   * @param blue
   *          - value 0-255
   * @param white
   *          - value 0-255
   */
  public void neoPixelFill(String neopixel, int beginAddress, int count, int red, int green, int blue, int white);

  /**
   * Sets the brightness of all pixels
   * 
   * @param neopixel
   *          the neopixel address
   * @param brightness
   *          the brightness level 0-255 (logarithmic)
   */
  public void neoPixelSetBrightness(String neopixel, int brightness);

  /**
   * Optimize clear of all the pixels, does a memset on board
   * 
   * @param neopixel
   *          - the neopixel service
   */
  public void neoPixelClear(String neopixel);

}
