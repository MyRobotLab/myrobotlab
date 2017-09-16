package org.myrobotlab.opencv;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

public class BlockingQueueGrabber extends FrameGrabber {

  public final static Logger log = LoggerFactory.getLogger(BlockingQueueGrabber.class.getCanonicalName());

  transient BlockingQueue<Frame> blockingData;
  
  public BlockingQueueGrabber(BlockingQueue<Frame> queue) {
    blockingData = queue;
  }

  public BlockingQueueGrabber(int cameraIndex) {
  }

  public BlockingQueueGrabber(String filename) {
  }

  public void add(Frame image) {
    blockingData.add(image);
  }

  @Override
  public Frame grab() {
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
