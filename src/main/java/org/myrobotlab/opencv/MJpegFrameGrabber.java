package org.myrobotlab.opencv;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import net.sf.jipcam.axis.MjpegFrame;
import net.sf.jipcam.axis.MjpegInputStream;

/**
 * This is an MJpeg stream frame grabber. This grabber will drop frames to
 * minimize video latency. This frame grabber takes a url to an mjpeg video
 * stream.
 * 
 * @author kwatters
 *
 */
public class MJpegFrameGrabber extends FrameGrabber {

  transient public final static Logger log = LoggerFactory.getLogger(MJpegFrameGrabber.class);
  private URL url;
  private MjpegInputStream mStream;
  transient private Java2DFrameConverter converter = new Java2DFrameConverter();
  // This tracks the largest frame that has been seen for this grabber to
  // determine how
  // far behind we are, so we can drop frames adaptively.
  private int maxFrameSize = 0;
  // the max number of sequential frames to skip before returning at least 1
  // frame.
  public int maxSkippedFrames = 5;
  // the percentage of bytes available with respect to the max frame size that
  // determines if we have the most recent frame.
  private static final double MAX_BUFFER_RATIO = 0.25;

  public MJpegFrameGrabber(String uri) {
    super();
    log.info("Startring MJpeg frame grabber for uri {}", uri);
    try {
      url = new URL(uri);
    } catch (MalformedURLException e) {
      log.warn("Error starting mjpeg frame grabber! URL: {}", uri, e);
    }
  }

  @Override
  public void start() throws Exception {
    try {
      mStream = new MjpegInputStream(url.openStream());
      maxFrameSize = 0;
    } catch (IOException e) {
      log.warn("Error starting the mjpeg stream grabber.", e);
      return;
    }
    log.info("MJPEG Stream Started {}", url);
  }

  @Override
  public void stop() throws Exception {
    if (mStream == null) {
      // we aren't started. just return
      return;
    }
    log.info("MJpeg Frame grabber stop called");
    try {
      mStream.close();
    } catch (IOException e) {
      log.info("Error closing mjpeg frame grabber.", e);
      return;
    }
  }

  @Override
  public void trigger() throws Exception {
    log.info("MJpeg Frame grabber tigger called");
  }

  @Override
  public Frame grab() throws Exception {
    // for timing how long it takes to clear the buffer of old frames.
    long start = System.currentTimeMillis();
    try {
      // under normal operation the numAvailable is usually between 22-26 bytes
      int numSkipped = -1;
      MjpegFrame mf = null;
      while (true) {
        // discard as data up to the most recent frame.
        numSkipped++;
        mf = mStream.readMjpegFrame();
        maxFrameSize = Math.max(mf.getBytes().length, maxFrameSize);
        // this is buffer size after reading a frame.
        int numAvailable = mStream.available();
        // log.info("Bytes Available: {} Max FrameSize: {}", numAvailable,
        // maxFrameSize);
        // if there's less than 25% a frame available, we're good. let's break
        // out of the loop
        // this is a recent frame.
        if (numAvailable < maxFrameSize * MAX_BUFFER_RATIO || numSkipped >= maxSkippedFrames) {
          if (numSkipped == 1) {
            log.info("Skipped {} frame in {} ms. Frame Size {}", numSkipped, System.currentTimeMillis() - start, mf.getBytes().length);
          } else if (numSkipped > 1) {
            log.info("Skipped {} frames in {} ms. Frame Size {}", numSkipped, System.currentTimeMillis() - start, mf.getBytes().length);
          }
          break;
        }
      }
      BufferedImage img = (BufferedImage) (mf.getImage());
      if (imageHeight > 0 && imageWidth > 0) {
        img = resizeImage(img, imageWidth, imageHeight);
      }
      Frame frame = converter.getFrame(img);
      return frame;
    } catch (IOException e) {
      // catch the exception and re-throw it as a javacv exception
      throw new Exception("MJpeg Frame grabber exception grabbing.", e);
    }
  }

  private BufferedImage resizeImage(BufferedImage original, int newWidth, int newHeight) {
    // sample the incoming image
    BufferedImage resized = new BufferedImage(newWidth, newHeight, original.getType());
    Graphics2D g = resized.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(original, 0, 0, newWidth, newHeight, 0, 0, original.getWidth(), original.getHeight(), null);
    g.dispose();
    return resized;
  }

  @Override
  public void release() throws Exception {
    // should we close here? or somewhere else?
    log.info("Framegrabber release called");
    try {
      mStream.close();
    } catch (IOException e) {
      log.warn("Error releasing the MJpeg frame grabber. ", e);
    }
  }

  public int getMaxSkippedFrames() {
    return maxSkippedFrames;
  }

  public void setMaxSkippedFrames(int maxSkippedFrames) {
    this.maxSkippedFrames = maxSkippedFrames;
  }

}
