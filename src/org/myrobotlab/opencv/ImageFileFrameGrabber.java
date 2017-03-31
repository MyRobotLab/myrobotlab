package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;

import java.util.HashMap;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class ImageFileFrameGrabber extends FrameGrabber {

  public final static Logger log = LoggerFactory.getLogger(ImageFileFrameGrabber.class.getCanonicalName());

  transient private IplImage image;
  transient private IplImage lastImage;
  transient private HashMap<String, IplImage> cache = new HashMap<String, IplImage>();
  private int frameCounter = 0;
  public String path;
  transient OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

  public ImageFileFrameGrabber(String path) {
    this.path = path;
  }

  @Override
  public Frame grab() {
    if (!cache.containsKey(path)) {
      image = cvLoadImage(path);
      cache.put(path, image);
    } else {
      image = cache.get(path).clone();
    }

    ++frameCounter;

    if (frameCounter > 1) {
      lastImage.release();
    }

    lastImage = image;
    return converter.convert(image);
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
