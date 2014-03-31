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

/*
 *  HSV changes in OpenCV -
 *  https://code.ros.org/trac/opencv/ticket/328 H is only 1-180
 *  H <- H/2 (to fit to 0 to 255)
 *  
 *  CV_HSV2BGR_FULL uses full 0 to 255 range
 */

import static com.googlecode.javacv.cpp.opencv_core.CV_FONT_HERSHEY_PLAIN;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvPutText;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_RGB2HSV;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;

import java.awt.Graphics;
import java.nio.ByteBuffer;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterHSV extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterHSV.class.getCanonicalName());

	IplImage hsv = null;
	IplImage hue = null;
	IplImage value = null;
	IplImage saturation = null;
	IplImage mask = null;

	int x = 0;
	int y = 0;
	int clickCounter = 0;
	int frameCounter = 0;
	Graphics g = null;
	String lastHexValueOfPoint = "";

	
	transient CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN, 1, 1);
		
	public OpenCVFilterHSV()  {
		super();
	}
	
	public OpenCVFilterHSV(String name)  {
		super(name);
	}

	public void samplePoint(Integer inX, Integer inY) {
		++clickCounter;
		x = inX;
		y = inY;

	}

	@Override
	public IplImage display(IplImage image, OpenCVData data) {

		++frameCounter;
		if (x != 0 && clickCounter % 2 == 0) {

			if (frameCounter % 10 == 0) {
				//frameBuffer = hsv.getBufferedImage(); // TODO - ran out of memory here
				ByteBuffer buffer = image.getByteBuffer();
				int index = y * image.widthStep() + x * image.nChannels();
				 // Used to read the pixel value - the 0xFF is needed to cast from
		        // an unsigned byte to an int.
		        int value = buffer.get(index) & 0xFF;
				lastHexValueOfPoint = Integer.toHexString(value & 0x00ffffff);
			}
			
			cvPutText(image, lastHexValueOfPoint, cvPoint(x,y), font, CvScalar.BLACK);
		}

		return image;
	}

	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		// CV_BGR2HSV_FULL - uses full 0-255 vs 0-180
		// CV_HSV2BGR_FULL
		cvCvtColor(image, hsv, CV_RGB2HSV);

		// cvSetImageCOI( hsv, 1);
		// cvCopy(hsv, hue );

		/*
		 * http://cgi.cse.unsw.edu.au/~cs4411/wiki/index.php?title=OpenCV_Guide#
		 * Calculating_color_histograms //Split out hue component and store in
		 * hue cxcore.cvSplit(hsv, hue, null, null, null);
		 */

		return hsv;

	}

		@Override
		public void imageChanged(IplImage image) {
			hsv = IplImage.createCompatible(image);
		}

}
