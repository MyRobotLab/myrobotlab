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

import static com.googlecode.javacv.cpp.opencv_core.CV_FONT_HERSHEY_PLAIN;
import static com.googlecode.javacv.cpp.opencv_core.CV_RGB;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvDrawLine;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_HOUGH_PROBABILISTIC;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCanny;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvHoughLines2;

import java.awt.image.BufferedImage;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.googlecode.javacpp.Pointer;
import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterHoughLines2 extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterHoughLines2.class.getCanonicalName());

	IplImage gray = null;
	
	double lowThreshold = 0.0;
	double highThreshold = 50.0;
	int apertureSize = 5;
	CvMemStorage storage = null;
	IplImage inlines = null;

	CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN, 1, 1);
	
	public OpenCVFilterHoughLines2()  {
		super();
	}
	
	public OpenCVFilterHoughLines2(String name)  {
		super(name);
	}


	
	CvPoint p0 = new CvPoint(0, 0);
	CvPoint p1 = new CvPoint(0, 0);

	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		if (image == null) {
			log.error("image is null");
		}

		if (gray == null) {
			gray = cvCreateImage(cvGetSize(image), 8, 1);
		}

		if (storage == null) {
			// storage = cvCreateMemStorage(0);
			storage = CvMemStorage.create();
		}

		if (inlines == null) {
			inlines = cvCreateImage(cvGetSize(image), 8, 1);
		}

		if (image.nChannels() > 1) {
			cvCvtColor(image, gray, CV_BGR2GRAY);
		} else {
			gray = image.clone();
		}

		// TODO - use named inputs and outputs
		lowThreshold = 5.0;
		highThreshold = 400.0;
		apertureSize = 3;
		cvCanny(gray, inlines, lowThreshold, highThreshold, apertureSize);

		// http://www.aishack.in/2010/04/hough-transform-in-opencv/ -
		// explanation of hough transform parameters

		// CV_HOUGH_MULTI_SCALE || CV_HOUGH_STANDARD
		CvSeq lines = cvHoughLines2(inlines, storage, CV_HOUGH_PROBABILISTIC, 1, Math.PI / 180, 10, 40, 10);

		for (int i = 0; i < lines.total(); i++) {

			Pointer line = cvGetSeqElem(lines, i);
			CvPoint pt1 = new CvPoint(line).position(0);
			CvPoint pt2 = new CvPoint(line).position(1);

			System.out.println("Line spotted: ");
			System.out.println("\t pt1: " + pt1);
			System.out.println("\t pt2: " + pt2);
			// cvLine(image, pt1, pt2, CV_RGB(255, 0, 0), 3, CV_AA, 0); // draw
			// the segment on the image
			cvDrawLine(image, p0, p1, CV_RGB(255, 255, 255), 2, 8, 0);
		}

		// cxcore.cvPutText(image, "x", cvPoint(10, 14), font, CvScalar.WHITE);

		return image;
	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub
		
	}

}
