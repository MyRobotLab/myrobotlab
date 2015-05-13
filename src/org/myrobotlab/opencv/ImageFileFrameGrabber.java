package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_highgui.cvLoadImage;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacpp.opencv_core.IplImage;

public class ImageFileFrameGrabber extends FrameGrabber {

	public final static Logger log = LoggerFactory.getLogger(ImageFileFrameGrabber.class.getCanonicalName());

	private IplImage image;
	private IplImage lastImage;
	private IplImage cache;
	private int frameCounter = 0;
	String path;

	public ImageFileFrameGrabber(String path) {
		this.path = path;
		
		
	}

	@Override
	public IplImage grab() {


		if (cache == null) {
			cache = cvLoadImage(path);
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
