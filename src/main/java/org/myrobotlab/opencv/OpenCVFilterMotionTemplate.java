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


import static org.bytedeco.javacpp.helper.opencv_core.CV_RGB;
import static org.bytedeco.javacpp.opencv_core.CV_L1;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_32F;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvAbsDiff;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvCreateMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvCvtScale;
import static org.bytedeco.javacpp.opencv_core.cvMerge;
import static org.bytedeco.javacpp.opencv_core.cvNorm;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvRect;
import static org.bytedeco.javacpp.opencv_core.cvReleaseImage;
import static org.bytedeco.javacpp.opencv_core.cvResetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvSetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_core.cvZero;
import static org.bytedeco.javacpp.opencv_imgproc.CV_AA;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCircle;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvLine;
import static org.bytedeco.javacpp.opencv_imgproc.cvThreshold;

import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.CvSize;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterMotionTemplate extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterMotionTemplate.class.getCanonicalName());

	// various tracking parameters (in seconds)
	final double MHI_DURATION = 1;
	final double MAX_TIME_DELTA = 0.5;
	final double MIN_TIME_DELTA = 0.05;
	// number of cyclic frame buffer used for motion detection
	// (should, probably, depend on FPS)
	final int N = 4;

	// ring image buffer
	transient IplImage[] buf = null;
	int last = 0;

	// temporary images
	transient IplImage mhi = null; // MHI
	transient IplImage orient = null; // orientation
	transient IplImage mask = null; // valid orientation mask
	IplImage segmask = null; // motion segmentation map
	CvMemStorage storage = null; // temporary storage

	transient IplImage motion = null;

	public OpenCVFilterMotionTemplate() {
		super();
	}

	public OpenCVFilterMotionTemplate(String name) {
		super(name);
	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub

	}

	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		// what can you expect? nothing? - if data != null then error?
		if (motion == null) {
			motion = cvCreateImage(cvSize(image.width(), image.height()), 8, 3);
			cvZero(motion);
			motion.origin(image.origin());
		}

		update_mhi(image, motion, 30);

		return motion;
	}

	// parameters:
	// img - input video frame
	// dst - resultant motion picture
	// args - optional parameters
	void update_mhi(IplImage img, IplImage dst, int diff_threshold) {
		double timestamp = 0.0;
		// TODO FIX double timestamp = (double)clock()/CLOCKS_PER_SEC; // get
		// current time in seconds
		CvSize size = cvSize(img.width(), img.height()); // get current frame
															// size
		int i, idx1 = last, idx2;
		IplImage silh;
		CvSeq seq;
		CvRect comp_rect;
		double count;
		double angle;
		CvPoint center;
		double magnitude;
		CvScalar color;

		// allocate images at the beginning or
		// reallocate them if the frame size is changed
		if (mhi == null || mhi.width() != size.width() || mhi.height() != size.height()) {
			if (buf == null) {
				buf = new IplImage[10];// IplImage.create(arg0, arg1, arg2,
										// arg3);
			}

			for (i = 0; i < N; i++) {
				if (buf[i] != null) {
					cvReleaseImage(buf[i]);
				}
				buf[i] = cvCreateImage(size, IPL_DEPTH_8U, 1);
				cvZero(buf[i]);
			}
			if (mhi != null) {
				cvReleaseImage(mhi);
			}
			if (orient != null) {
				cvReleaseImage(orient);
			}
			if (segmask != null) {
				cvReleaseImage(segmask);
			}
			if (mask != null) {
				cvReleaseImage(mask);
			}

			mhi = cvCreateImage(size, IPL_DEPTH_32F, 1);
			cvZero(mhi); // clear MHI at the beginning
			orient = cvCreateImage(size, IPL_DEPTH_32F, 1);
			segmask = cvCreateImage(size, IPL_DEPTH_32F, 1);
			mask = cvCreateImage(size, IPL_DEPTH_8U, 1);
		}

		cvCvtColor(img, buf[last], CV_BGR2GRAY); // convert frame to
													// grayscale

		idx2 = (last + 1) % N; // index of (last - (N-1))th frame
		last = idx2;

		silh = buf[idx2];
		cvAbsDiff(buf[idx1], buf[idx2], silh); // get difference between frames

		cvThreshold(silh, silh, diff_threshold, 1, CV_THRESH_BINARY); // and
																		// threshold
																		// it
		cvUpdateMotionHistory(silh, mhi, timestamp, MHI_DURATION); // update
																	// MHI

		// convert MHI to blue 8u image
		cvCvtScale(mhi, mask, 255. / MHI_DURATION, (MHI_DURATION - timestamp) * 255. / MHI_DURATION);
		cvZero(dst);
		cvMerge(mask, null, null, null, dst);

		// calculate motion gradient orientation and valid orientation mask
		cvCalcMotionGradient(mhi, mask, orient, MAX_TIME_DELTA, MIN_TIME_DELTA, 3);

		if (storage == null)
			storage = cvCreateMemStorage(0);
		else
			cvClearMemStorage(storage);

		// segment motion: get sequence of motion components
		// segmask is marked motion components map. It is not used further
		seq = cvSegmentMotion(mhi, segmask, storage, timestamp, MAX_TIME_DELTA);

		// iterate through the motion components,
		// One more iteration (i == -1) corresponds to the whole image (global
		// motion)
		for (i = -1; i < seq.total(); i++) {
			comp_rect = null;
			if (i < 0) { // case of the whole image
				comp_rect = cvRect(0, 0, size.width(), size.height());
				color = CV_RGB(255, 255, 255);
				magnitude = 100;
			} else { // i-th motion component
				// TODO - fix CvConnectedComp connected_comp = new
				// CvConnectedComp(cvGetSeqElem( seq, i ));
				// TODO -fix comp_rect = connected_comp.rect;
				if (comp_rect != null && comp_rect.width() + comp_rect.height() < 100) // reject
																						// very
					// small
					// components
					continue;
				color = CV_RGB(255, 0, 0);
				magnitude = 30;
			}

			// select component ROI
			cvSetImageROI(silh, comp_rect);
			cvSetImageROI(mhi, comp_rect);
			cvSetImageROI(orient, comp_rect);
			cvSetImageROI(mask, comp_rect);

			// calculate orientation
			angle = cvCalcGlobalOrientation(orient, mask, mhi, timestamp, MHI_DURATION);
			angle = 360.0 - angle; // adjust for images with top-left origin

			count = cvNorm(silh, null, CV_L1, null); // calculate number of
														// points within
														// silhouette ROI

			cvResetImageROI(mhi);
			cvResetImageROI(orient);
			cvResetImageROI(mask);
			cvResetImageROI(silh);

			// check for the case of little motion
			if (count < comp_rect.width() * comp_rect.height() * 0.05)
				continue;

			// draw a clock with arrow indicating the direction
			center = cvPoint((comp_rect.x() + comp_rect.width() / 2), (comp_rect.y() + comp_rect.height() / 2));

			// cvCircle( dst, center, cvRound(magnitude*1.2), color, 3, CV_AA, 0
			// );
			cvCircle(dst, center, (int) (magnitude * 1.2), color, 3, CV_AA, 0);
			cvLine(dst, center, cvPoint((int) (center.x() + magnitude * Math.cos(angle * Math.PI / 180)), (int) (center.y() - magnitude * Math.sin(angle * Math.PI / 180))), color,
					3, CV_AA, 0);
		}
	}

}
