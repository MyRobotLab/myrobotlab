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

import static com.googlecode.javacv.cpp.opencv_core.CV_FONT_HERSHEY_PLAIN;
import static com.googlecode.javacv.cpp.opencv_core.cvCircle;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvPutText;

import java.util.ArrayList;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.Point2Df;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_features2d.KeyPoint;
import com.googlecode.javacv.cpp.opencv_features2d.SimpleBlobDetector;

public class OpenCVFilterSimpleBlobDetector extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterSimpleBlobDetector.class.getCanonicalName());

	public ArrayList<Point2Df> pointsToPublish = new ArrayList<Point2Df>();
	transient CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN, 1, 1);
	
	public OpenCVFilterSimpleBlobDetector()  {
		super();
	}
	
	public OpenCVFilterSimpleBlobDetector(String name)  {
		super(name);
	}
	

	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		if (image == null) {
			log.error("image is null");
		}
		
		// TODO: track an array of blobs , not just one.
		SimpleBlobDetector o = new SimpleBlobDetector();
		KeyPoint point = new KeyPoint();
		o.detect(image, point, null);
		System.out.println(point.toString());
		float x = point.pt().x();
		float y = point.pt().y();
		pointsToPublish.clear();
		pointsToPublish.add(new Point2Df(x, y));
		return image;
	}

	@Override
	public IplImage display(IplImage frame, OpenCVData data) {
		float x, y;
		int xPixel, yPixel;
		for (int i = 0; i < pointsToPublish.size(); ++i) {
			Point2Df point = pointsToPublish.get(i);
			x = point.x;
			y = point.y;
			// graphics.setColor(Color.red);
			//if (useFloatValues) {
			//	xPixel = (int) (x * width);
			//	yPixel = (int) (y * height);
			//} else {
				xPixel = (int) x;
				yPixel = (int) y;
			//}
			cvCircle(frame, cvPoint(xPixel, yPixel), 5, CvScalar.GREEN, -1, 8, 0);
		}
		cvPutText(frame, String.format("valid %d", pointsToPublish.size()), cvPoint(10,10), font, CvScalar.GREEN);
		return frame;
	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub
		
	}

}
