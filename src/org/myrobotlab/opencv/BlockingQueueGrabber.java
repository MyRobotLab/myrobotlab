package org.myrobotlab.opencv;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacpp.opencv_core.IplImage;

public class BlockingQueueGrabber extends FrameGrabber {

	public final static Logger log = LoggerFactory.getLogger(BlockingQueueGrabber.class.getCanonicalName());

	BlockingQueue<IplImage> blockingData;

	public BlockingQueueGrabber(BlockingQueue<IplImage> queue) {
		blockingData = queue;
	}

	public BlockingQueueGrabber(int cameraIndex) {
	}

	public BlockingQueueGrabber(String filename) {
	}

	public void add(IplImage image) {
		blockingData.add(image);
	}

	@Override
	public IplImage grab() {
		try {
			return blockingData.take();
		} catch (InterruptedException e) {
			Logging.logError(e);
			return null;
		}
	}

	@Override
	public void release() throws Exception {
	}

	public void setQueue(BlockingQueue<IplImage> queue) {
		blockingData = queue;
	}

	@Override
	public void start() {
		if (blockingData == null) {
			blockingData = new LinkedBlockingQueue<IplImage>();
		}
	}

	@Override
	public void stop() {
	}

	@Override
	public void trigger() throws Exception {
	}

}
