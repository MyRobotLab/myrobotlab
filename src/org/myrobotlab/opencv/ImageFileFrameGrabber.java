package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_calib3d.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_features2d.*;
import static org.bytedeco.javacpp.opencv_flann.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_ml.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;
import static org.bytedeco.javacpp.opencv_photo.*;
import static org.bytedeco.javacpp.opencv_shape.*;
import static org.bytedeco.javacpp.opencv_stitching.*;
import static org.bytedeco.javacpp.opencv_video.*;
import static org.bytedeco.javacpp.opencv_videostab.*;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class ImageFileFrameGrabber extends FrameGrabber {

	public final static Logger log = LoggerFactory.getLogger(ImageFileFrameGrabber.class.getCanonicalName());

	private IplImage image;
	private IplImage lastImage;
	private IplImage cache;
	private int frameCounter = 0;
	String path;
	transient OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

	public ImageFileFrameGrabber(String path) {
		this.path = path;
		
		
	}

	@Override
	public Frame grab() {


		if (cache == null) {
			cache = cvLoadImage(path);
		}

		image = cache.clone();

		++frameCounter;

		if (frameCounter > 1) {
			lastImage.release();
		}

		lastImage = image;
		return converter.convert(image);
	}

	@Override
	public void release() throws Exception {
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void trigger() throws Exception {
	}

}
