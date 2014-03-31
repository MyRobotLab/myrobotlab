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
import static com.googlecode.javacv.cpp.opencv_core.CV_TERMCRIT_EPS;
import static com.googlecode.javacv.cpp.opencv_core.CV_TERMCRIT_ITER;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_32F;
import static com.googlecode.javacv.cpp.opencv_core.cvCircle;
import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvPutText;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_core.cvTermCriteria;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvGoodFeaturesToTrack;
import static com.googlecode.javacv.cpp.opencv_video.cvCalcOpticalFlowPyrLK;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.Point2Df;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.CvTermCriteria;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterLKOpticalTrack extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterLKOpticalTrack.class.getCanonicalName());

	// good features related - use good features filter ????
	// quality - Multiplier for the maxmin eigenvalue; specifies minimal
	// accepted quality of image corners
	public double qualityLevel = 0.05;
	// minDistance - Limit, specifying minimum possible distance between
	// returned corners; Euclidian distance is used
	public double minDistance = 5.0;
	// blockSize - Size of the averaging block, passed to underlying
	// cvCornerMinEigenVal or cvCornerHarris used by the function
	public int blockSize = 3;
	// If nonzero, Harris operator (cvCornerHarris) is used instead of default
	// cvCornerMinEigenVal.
	public int useHarris = 0;
	// Free parameter of Harris detector; used only if useHarris != 0
	public double k = 0.0;
	
	public boolean clearPoints = false;

	public ArrayList<Point2Df> pointsToPublish = new ArrayList<Point2Df>();

	public int MAX_POINT_COUNT = 30;
	public boolean needTrackingPoints = false;
	public boolean nightMode = false;
	public boolean addRemovePt = false;
	public int windowSize = 15;

	private int[] count = { 0 };
	byte[] status;
	float[] error;
	
	private boolean addRemovePoint = false;

	private Point2Df samplePoint = new Point2Df();

	// display graphic structures
	// transient BufferedImage frameBuffer = null;

	// opencv data structures
	transient CvTermCriteria termCriteria;
	transient CvSize winSize;
	transient IplImage preGrey, grey, eig, tmp, prePyramid, pyramid, swap, mask, image;
	transient CvPoint2D32f prePoints, points, swapPoints;
	
	public OpenCVFilterLKOpticalTrack()  {
		super();
	}
	
	public OpenCVFilterLKOpticalTrack(String name)  {
		super(name);
	}

	
	@Override
	public void imageChanged(IplImage image) {

		points = new CvPoint2D32f(MAX_POINT_COUNT);
		prePoints = new CvPoint2D32f(MAX_POINT_COUNT);

		eig = IplImage.create(imageSize, IPL_DEPTH_32F, 1);
		tmp = IplImage.create(imageSize, IPL_DEPTH_32F, 1);

		termCriteria = cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03);

		winSize = cvSize(windowSize, windowSize);
		grey = IplImage.create(imageSize, 8, 1); 
		preGrey = IplImage.create(imageSize, 8, 1);
		cvCopy(grey, preGrey);
		
		// CvSize pyr_sz = cvSize(preGrey.width + 8, grey.height / 3);
		// prePyramid = cvCreateImage(pyr_sz, IPL_DEPTH_32F, 1);
		// pyramid = cvCreateImage(pyr_sz, IPL_DEPTH_32F, 1);

		status = new byte[MAX_POINT_COUNT];
		error = new float[MAX_POINT_COUNT];
	}

	public void samplePoint(Integer x, Integer y) {
		if (count[0] < MAX_POINT_COUNT) {
			samplePoint.x = x;
			samplePoint.y = y;
			addRemovePoint = true;
		} else {
			clearPoints();
		}
	}

	public void samplePoint(Float x, Float y) {
		samplePoint((int) (x * width), (int) (y * height));
	}

	public void clearPoints() {
		count[0] = 0;
		clearPoints = false;
	}

	transient CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN, 1, 1);
	@Override	
	public IplImage display(IplImage frame, OpenCVData data) {
		float x, y;
		int xPixel, yPixel;
		for (int i = 0; i < pointsToPublish.size(); ++i) {
			Point2Df point = pointsToPublish.get(i);
			x = point.x;
			y = point.y;

			// graphics.setColor(Color.red);
			if (useFloatValues) {
				xPixel = (int) (x * width);
				yPixel = (int) (y * height);
			} else {
				xPixel = (int) x;
				yPixel = (int) y;
			}
			cvCircle(frame, cvPoint(xPixel, yPixel), 1, CvScalar.GREEN, -1, 8, 0);
		}
		cvPutText(frame, String.format("valid %d", pointsToPublish.size()), cvPoint(10,10), font, CvScalar.GREEN);
		return frame;
	}

	
	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		if (channels == 3) {
			cvCvtColor(image, grey, CV_BGR2GRAY);
		} else {
			grey = image;
		}
		
		if (clearPoints)
		{
			clearPoints();
			pointsToPublish.clear();
		}

		if (addRemovePoint && count[0] < MAX_POINT_COUNT) {
			prePoints.position(count[0]).x(samplePoint.x);
			prePoints.position(count[0]).y(samplePoint.y);
			count[0]++;
			// why bother
			//cvFindCornerSubPix(grey, features.position(count[0] - 1), 1, cvSize(win_size, win_size), cvSize(-1, -1), cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03));
			addRemovePoint = false;
		}
		
		if (preGrey != null) { // need at least 2 images to track

			int win_size = 15;

			if (needTrackingPoints) { // use good features filter ?
				count[0] = MAX_POINT_COUNT;
				cvGoodFeaturesToTrack(preGrey, eig, tmp, prePoints, count, 0.05, 5.0, mask, 3, 0, 0.04);
				// why should I find sub-pixel resolution ???
				// cvFindCornerSubPix(preGrey, prePoints, count[0],
				// cvSize(win_size, win_size), cvSize(-1, -1),
				// cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20,
				// 0.03));
				needTrackingPoints = false;
			}

			if (count[0] > 0) {

				// Call Lucas Kanade algorithm
				// clear status and error arrays
				for (int i = 0; i < MAX_POINT_COUNT; ++i) {
					status[i] = 0;
					error[i] = 0.0f;
				}

//				CvPoint2D32f points = new CvPoint2D32f(MAX_POINT_COUNT); // WTF?
																			

				cvCalcOpticalFlowPyrLK(preGrey, grey, null, null, prePoints, points, count[0], cvSize(win_size, win_size), 5, status, error,
						cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.3), 0);

				/*
				 * getting ready to publish - so let's use Pojos - not JNI
				 * OpenCV nor Swing because remote consumers may not have those
				 * definitions available (e.g. Android)
				 */
				pointsToPublish = new ArrayList<Point2Df>();
				int validPointCount = 0;
				float x, y;
				for (int i = 0; i < count[0]; i++) {
					if (status[i] == 0 || error[i] > 550) {
						// System.out.println("Error is " + error[i] + "/n");
						continue;
					}
					++validPointCount;
					prePoints.position(i);
					points.position(i);
	
					x = points.x();
					y = points.y();
					log.debug(String.format("%d %s, %s", i, x, y));

					// puting new points in previous buffer
					prePoints.put(points.get());

					// you have to "re-position" after a get ?? YOWZA
					// points.position(i);
					if (useFloatValues) {
						pointsToPublish.add(new Point2Df(x / width, y / height));
					} else {
						pointsToPublish.add(new Point2Df(x, y));
					}
					// cvLine(imgC, p0, p1, CV_RGB(255, 0, 0), 2, 8, 0);
				}
				count[0] = validPointCount;
				
				if (publishData)
				{
					data.set(pointsToPublish);
				}
				
				//invoke("publish", pointsToPublish); 

			}

		}


		// swap
		// TODO - release what preGrey pointed to?
		cvCopy(grey, preGrey);
		// prePyramid = pyramid;

		return image;
	}

}
