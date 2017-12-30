package org.myrobotlab.opencv;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.github.sarxos.webcam.Webcam;

public class SarxosFrameGrabber extends FrameGrabber {

  transient public final static Logger log = LoggerFactory.getLogger(SarxosFrameGrabber.class);

  private Webcam webcam;
  private int cameraIndex = 0;
  public int desiredWidth = 640;
  public int desiredHeight = 480;
  transient private Java2DFrameConverter converter = new Java2DFrameConverter();

  public SarxosFrameGrabber(int cameraIndex) {
    super();
    this.cameraIndex = cameraIndex;
  }

  @Override
  public void start() throws Exception {
    List<Webcam> found = Webcam.getWebcams();
    webcam = found.get(cameraIndex);
    Dimension[] sizes = webcam.getViewSizes();
    int bestError = Integer.MAX_VALUE;
    Dimension best = null;
    for (Dimension d : sizes) {
      int error = (d.width - desiredWidth) * (d.height - desiredHeight);
      if (error < bestError) {
        bestError = error;
        best = d;
      }
    }
    webcam.setViewSize(best);
    webcam.open();
    Dimension actualSize = webcam.getViewSize();
    log.info("Camera Open With Resolution {}", actualSize);
  }

  @Override
  public void stop() throws Exception {
    log.info("Framegrabber stop called");
    webcam.close();
  }

  @Override
  public void trigger() throws Exception {
    log.info("Framegrabber tigger called");
  }

  @Override
  public Frame grab() throws Exception {
    if (webcam == null) {
      // start should have been called
      start();
    }
    BufferedImage img = webcam.getImage();
    // Seems like we need to flip the color chanels .. also gamma is set to 1.0
    // ? (what ever that is.)
    Frame frame = converter.getFrame(img, 1.0, true);
    // Frame frame = converter.convert(img);
    return frame;
  }

  @Override
  public void release() throws Exception {
    // should we close here? or somewhere else?
    log.info("Framegrabber release called");
    webcam.close();
  }

}
