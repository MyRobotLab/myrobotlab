package org.myrobotlab.opencv;

import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class ImageFileFrameGrabber extends FrameGrabber {

	public final static Logger log = LoggerFactory.getLogger(ImageFileFrameGrabber.class.getCanonicalName());

	private IplImage image;
	private IplImage lastImage;
	private IplImage cache;
	private int frameCounter = 0;
	String filename;

	public ImageFileFrameGrabber(String filename) {
		this.filename = filename;
	}

	@Override
	public IplImage grab() {

		if (cache == null) {
			cache = cvLoadImage(filename);
		}

		image = cache.clone();

		++frameCounter;

		if (frameCounter > 1) {
			lastImage.release();
		}

		lastImage = image;
		return image;
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
