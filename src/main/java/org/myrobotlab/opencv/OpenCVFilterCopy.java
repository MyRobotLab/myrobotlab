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

// http://stackoverflow.com/questions/11515072/how-to-identify-optimal-parameters-for-cvcanny-for-polygon-approximation
package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterCopy extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterCopy.class.getCanonicalName());

	public OpenCVFilterCopy() {
		super();
	}

	public OpenCVFilterCopy(String name) {
		super(name);
	}

	/*
	 * 
	 * void getSubImg(IplImage* img, IplImage* subImg, CvRect roiRect) {
	 * 
	 * cvSetImageROI(img, roiRect); subImg = cvCreateImage(cvGetSize(img),
	 * img->depth, img->nChannels); cvCopy(img, subImg, NULL);
	 * cvResetImageROI(img); }
	 */

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub

	}

	@Override
	public IplImage process(IplImage img, OpenCVData data) {
		IplImage copy = null;

		// CvRect roiRect = new CvRect(0, 0, 30, 120);
		// cvSetImageROI(img, roiRect);
		copy = cvCreateImage(cvGetSize(img), img.depth(), img.nChannels());

		cvCopy(img, copy, null);
		// vp.sources.put(vp.boundServiceName, String.format("%s.copy",
		// vp.boundServiceName, name), img);
		// cvResetImageROI(img);

		return copy;
	}

}
