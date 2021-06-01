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

public interface NeoPixel2Control extends NameProvider {

  /**
   * high level "attach" which internally will call attachDevice(Device device,
   * int[] config)
   * 
   * @param control
   * @param pin
   * @param numPixel
   * @param depth
   * @throws Exception
   */
  public void attach(String control, int pin, int numPixel, int depth) throws Exception;

  /*
   * high level "detach" with internally will call detachDevice(Device device)
   */
  public void detach(String controller);

  /**
   * SetPixel: defining the pixels of the pixel matrix. Setting pixels are not
   * send directly to the Neopixel Hardware but send with the sendPixelMatrix()
   * method
   * 
   * @param address
   * @param red
   * @param green
   * @param blue
   */
  public void setPixel(int address, int red, int green, int blue);

  /**
   * Send a matrix of pixels to the neopixel hardware
   */
  public void writeMatrix();

  public Integer getPin();

  public int getNumPixel();

  public void turnOff();

  public void turnOn();

  /*
   * <pre> setAnimation &#64;param animation - preprogramed animation &#64;param
   * red - value 0-255 - set base color for the animation &#64;param green -
   * value 0-255 - set base color for the animation &#64;param blue - value
   * 0-255 - set base color for the animation &#64;param speed - set speed of
   * the animation 1 = fastest (update every ~30ms), 100 = 100 times slower than
   * 1 value </pre>
   */
  public void setAnimation(int animation, int red, int green, int blue, int speed);

  public void setAnimation(String animation, int red, int green, int blue, int speed);

  public void setAnimation(String animation, String red, String green, String blue, String speed);

  public void setAnimationSetting(String animation);

}
