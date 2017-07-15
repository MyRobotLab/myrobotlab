package org.myrobotlab.vision;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         A pipeline frame grabber can attach to another OpenCV's output and
 *         perform its own processing. They can be stacked creating much more
 *         complex image pipelines.
 *
 */
public class PipelineFrameGrabber extends FrameGrabber {

  public final static Logger log = LoggerFactory.getLogger(PipelineFrameGrabber.class.getCanonicalName());
  transient BlockingQueue<Frame> blockingData;
  String sourceKey = "";

  public PipelineFrameGrabber(BlockingQueue<Frame> queue) {
    blockingData = queue;
  }

  public PipelineFrameGrabber(int cameraIndex) {
  }

  public PipelineFrameGrabber(String sourceKey) {
    log.info("attaching video feed to {}");
    this.sourceKey = sourceKey;
  }

  public void add(Frame image) {
    blockingData.add(image);
  }

  @Override
  public Frame grab() {

    try {
      // added non blocking allowing thread to terminate
      Frame image = blockingData.poll(1000, TimeUnit.MILLISECONDS);
      return image;
    } catch (InterruptedException e) {
      Logging.logError(e);
      return null;
    }

  }

  @Override
  public void release() throws Exception {
  }

  public void setQueue(BlockingQueue<Frame> queue) {
    blockingData = queue;
  }

  @Override
  public void start() {
    if (blockingData == null) {
      blockingData = new LinkedBlockingQueue<Frame>();
    }
  }

  @Override
  public void stop() {
  }

  @Override
  public void trigger() throws Exception {
  }

}
