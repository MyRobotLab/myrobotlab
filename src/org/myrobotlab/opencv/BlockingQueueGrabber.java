package org.myrobotlab.opencv;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;


public class BlockingQueueGrabber extends FrameGrabber {

	public final static Logger log = LoggerFactory.getLogger(BlockingQueueGrabber.class.getCanonicalName());
	

	BlockingQueue<IplImage> blockingData;

	public BlockingQueueGrabber(String filename) {
	}
	
	public BlockingQueueGrabber(int cameraIndex) {
	}

	public BlockingQueueGrabber(BlockingQueue<IplImage> queue) {
		blockingData = queue;
	}
	
	public void setQueue(BlockingQueue<IplImage> queue)
	{
		blockingData = queue;
	}

	@Override
	public void start() {
		if (blockingData == null)
		{
			blockingData = new LinkedBlockingQueue<IplImage>();
		}
	}

	@Override
	public void stop() {
	}
	
	public void add(IplImage image)
	{
		blockingData.add(image);
	}

	@Override
	public void trigger() throws Exception {
	}

	@Override
	public IplImage grab() {
		try {
			return blockingData.take();
		} catch (InterruptedException e) {
			Logging.logException(e);
			return null;
		}
	}
	
	@Override
	public void release() throws Exception {
	}

}
