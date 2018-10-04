package org.myrobotlab.opencv;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;
import net.sf.jipcam.axis.MjpegInputStream;

/**
 * This is an MJpeg stream frame grabber.  This grabber will drop frames to minimize video latency.
 * This frame grabber takes a url to an mjpeg video stream.
 * 
 * @author kwatters
 *
 */
public class MJpegFrameGrabber extends FrameGrabber {

  private static final int MJPEG_STREAM_LAG_THRESHOLD = 500;

  transient public final static Logger log = LoggerFactory.getLogger(MJpegFrameGrabber.class);

  private URL url;
  private MjpegInputStream mStream;
  transient private Java2DFrameConverter converter = new Java2DFrameConverter();

  public MJpegFrameGrabber(String uri) {
    super();
    log.info("Startring MJpeg frame grabber for uri {}", uri);
    try {
      url = new URL(uri);
    } catch (MalformedURLException e) {
      log.warn("Error starting mjpeg frame grabber!", e);
    }
  }

  @Override
  public void start() throws Exception {
    try {
      mStream = new MjpegInputStream(url.openStream());
    } catch (IOException e) {
      log.warn("Error starting the mjpeg stream grabber.", e);
      return;
    }
    log.info("MJPEG Stream Open {}", url.toString());
  }

  @Override
  public void stop() throws Exception {
    log.info("Framegrabber stop called");
    try {
      mStream.close();
    } catch (IOException e) {
      log.info("Error closing mjpeg frame grabber: {}", e);
      return;
    }
  }

  @Override
  public void trigger() throws Exception {
    log.info("Framegrabber tigger called");
  }

  @Override
  public Frame grab() throws Exception {
    BufferedImage img;
    try {
      int numAvailable = mStream.available();
      // under normal operation the numAvailable is usually between 22-26 bytes.
      if (numAvailable > MJPEG_STREAM_LAG_THRESHOLD) {
        // we've got some data to burn
        log.info("MJpeg frame buffer large, skipping frame. {} bytes" , numAvailable);
        // read a frame and ignore the result.
        mStream.readMjpegFrame();
      }
      img = (BufferedImage)(mStream.readMjpegFrame().getImage());
      Frame frame = converter.getFrame(img);
      return frame;
    } catch (IOException e) {
      // This is a javacv specific frame grabber exception.
      throw new Exception("MJpeg Frame grabber exception grabbing.", e);
    }
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

}
