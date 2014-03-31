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
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCanny;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;

import java.awt.image.BufferedImage;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterFauvist extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFauvist.class.getCanonicalName());

	IplImage gray = null;
	IplImage inlines = null;
	
	double lowThreshold = 0.0;
	double highThreshold = 50.0;
	int apertureSize = 5;

	public OpenCVFilterFauvist()  {
		super();
	}
	
	public OpenCVFilterFauvist(String name)  {
		super(name);
	}

	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		if (image == null) {
			log.error("image is null");
		}

		if (gray == null) {
			gray = cvCreateImage(cvGetSize(image), 8, 1);
		}
		if (inlines == null) {
			inlines = cvCreateImage(cvGetSize(image), 8, 1);
		}

		if (image.nChannels() == 3) {
			cvCvtColor(image, gray, CV_BGR2GRAY);
		} else {
			gray = image.clone();
		}
		/*
		 * lowThreshold = 600.0; highThreshold = 1220.0; apertureSize = 5;
		 */
		lowThreshold = 40.0;
		highThreshold = 110.0;
		apertureSize = 3;
		cvCanny(gray, inlines, lowThreshold, highThreshold, apertureSize);

		return inlines;
	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub
		
	}

}
