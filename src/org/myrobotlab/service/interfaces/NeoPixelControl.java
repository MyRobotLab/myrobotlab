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

import org.myrobotlab.framework.interfaces.NameProvider;

public interface NeoPixelControl extends NameProvider {

	/**
	 * high level "attach" which internally will call attachDevice(Device
	 * device, int[] config)
	 * @param controller c 
	 * 
	 * @param numPixel
	 *            - All of the config needed for the device -numPixel=number of
	 *            pixel of the neopixel hardware
	 * @param pin p
	 *            -
	 * @throws Exception e 
	 */
	public void attach(NeoPixelController controller, int pin, int numPixel) throws Exception;

	/*
	 * high level "detach" with internally will call detachDevice(Device device)
	 */
	public void detach(NeoPixelController controller);

	/**
	 * SetPixel: defining the pixels of the pixel matrix. Setting pixels are not
	 * send directly to the Neopixel Hardware but send with the
	 * sendPixelMatrix() method
	 * 
	 * @param address
	 *            - value 1 to numPixel
	 * @param red
	 *            - value 0-255
	 * @param green
	 *            - value 0-255
	 * @param blue
	 *            - value 0-255
	 */
	public void setPixel(int address, int red, int green, int blue);

	/**
	 * Send a matrix of pixel to the neopixel hardware
	 */
	public void writeMatrix();

	public Integer getPin();

	public int getNumPixel();

	public void turnOff();

	public void turnOn();

	/*
	 * <pre>
	 * setAnimation
	 * &#64;param animation - preprogramed animation
	 * &#64;param red       - value 0-255 - set base color for the animation
	 * &#64;param green     - value 0-255 - set base color for the animation
	 * &#64;param blue      - value 0-255 - set base color for the animation
	 * &#64;param speed     - set speed of the animation 1 = fastest (update every ~30ms), 100 = 100 times slower than 1 value
	 * </pre>
	 */
	public void setAnimation(int animation, int red, int green, int blue, int speed);

	public void setAnimation(String animation, int red, int green, int blue, int speed);

	public void setAnimation(String animation, String red, String green, String blue, String speed);

	public void setAnimationSetting(String animation);

}
