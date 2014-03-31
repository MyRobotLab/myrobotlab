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

import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvPyrUp;

import java.awt.image.BufferedImage;
import java.util.HashMap;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterPyramidUp extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterPyramidUp.class.getCanonicalName());

	IplImage dst = null;
	
	int filter = 7;
	
	public OpenCVFilterPyramidUp()  {
		super();
	}
	
	public OpenCVFilterPyramidUp(String name)  {
		super(name);
	}

	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		if (image == null) {
			log.error("image is null");
		}

		if (dst == null) {
			dst = cvCreateImage(cvSize(2 * image.width(), 2 * image.height()), 8, image.nChannels());
		}

		cvPyrUp(image, dst, filter);

		return dst;
	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub
		
	}

}
