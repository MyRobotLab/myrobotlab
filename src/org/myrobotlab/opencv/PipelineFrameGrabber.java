package org.myrobotlab.opencv;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;


/**
 * @author GroG
 * 
 * A pipeline frame grabber can attach to another OpenCV's output and
 * perform its own processing.  They can be stacked creating much more
 * complex image pipelines.
 *
 */
public class PipelineFrameGrabber extends FrameGrabber {

	public final static Logger log = LoggerFactory.getLogger(PipelineFrameGrabber.class.getCanonicalName());
	BlockingQueue<IplImage> blockingData;
	String sourceKey = "";

	public PipelineFrameGrabber(String sourceKey) {
		log.info("attaching video feed to {}");
		this.sourceKey = sourceKey;
	}
	
	public PipelineFrameGrabber(int cameraIndex) {
	}

	public PipelineFrameGrabber(BlockingQueue<IplImage> queue) {
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
			// added non blocking allowing thread to terminate
			IplImage image = blockingData.poll(1000, TimeUnit.MILLISECONDS);
			return image;
		} catch (InterruptedException e) {
			Logging.logException(e);
			return null;
		}
		
	}
	
	@Override
	public void release() throws Exception {
	}

}
