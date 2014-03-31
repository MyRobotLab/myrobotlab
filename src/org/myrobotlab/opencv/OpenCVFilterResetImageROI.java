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

package org.myrobotlab.opencv;

import java.awt.image.BufferedImage;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterResetImageROI extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterResetImageROI.class.getCanonicalName());

	public OpenCVFilterResetImageROI()  {
		super();
	}
	
	public OpenCVFilterResetImageROI(String name)  {
		super(name);
	}

	IplImage src = null;
	IplImage dst = null;

	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		/*
		cfg.set(USE_INPUT_IMAGE_NAME, false);
		cfg.set(USE_OUTPUT_IMAGE_NAME, false);

		if (cfg.getBoolean(USE_INPUT_IMAGE_NAME)) {
			String srcName = cfg.get(INPUT_IMAGE_NAME);
			if (globalData.containsKey(srcName)) {
				src = (IplImage) globalData.get(srcName);
			} else {
				src = image.clone();
				globalData.put(srcName, src);
			}
		} else {
			src = image;
		}

		if (cfg.getBoolean(USE_OUTPUT_IMAGE_NAME)) {
			String dstName = cfg.get(OUTPUT_IMAGE_NAME);
			if (globalData.containsKey(dstName)) {
				dst = (IplImage) globalData.get(dstName);
			} else {
				dst = image.clone();
				globalData.put(dstName, dst);
			}

		} else {
			dst = src;
		}

		// if (cfg.getBoolean(USE_ROI))
		// {
		cvResetImageROI(dst);
		// }
*/
		return image; // TODO - src dst or image? consistency?
	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub
		
	}

}
