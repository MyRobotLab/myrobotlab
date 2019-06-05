package org.myrobotlab.opencv;

import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class ImageFileFrameGrabber extends FrameGrabber {
  public final static Logger log = LoggerFactory.getLogger(ImageFileFrameGrabber.class.getCanonicalName());

  // delay in ms between grabs.
  // public int delay = 31; // 32 fps is 31.5 ms per frame
  public int delay = 16;
  private ArrayList<File> imageFiles = new ArrayList<File>();
  private int grabCount = 0;

  protected transient IplImage image;
  transient private IplImage lastImage;

  // transient private HashMap<String, IplImage> cache = new HashMap<String,
  // IplImage>();
  private int frameCounter = 0;
  public String path;
  transient OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

  public ImageFileFrameGrabber(String path) {
    this.path = path;
    load();
  }

  public void load() {
    File target = new File(path);
    if (!target.isDirectory()) {
      imageFiles.add(target);
    } else {
      File[] listOfFiles = target.listFiles();
      for (File file : listOfFiles) {
        if (file.isFile()) {
          // TODO: check what formats opencv's cvLoadImage supports and add that
          // here.
          if (file.getName().toLowerCase().endsWith("png") || file.getName().toLowerCase().endsWith("jpg")) {
            // It's an image file! ish...
            imageFiles.add(file);
          }
        }
      }
    }
  }

  @Override
  public Frame grab() {
    /*
     * not needed as opencv can self regulat - or adjust fps try { // pause for
     * the specified delay before loading the image. Thread.sleep(delay); }
     * catch (InterruptedException e) {} // set the file path
     * 
     */
    path = imageFiles.get(grabCount).getAbsolutePath();
    log.debug("Grabbing file {} - {}", grabCount, path);
    // grab it.
    try {
      image = cvLoadImage(path);
    } catch (Throwable e) {
      log.error("cvLoadImage threw", e);
      return null;
    }
    /*
     * if (!cache.containsKey(path)) { image = cvLoadImage(path,
     * CV_LOAD_IMAGE_UNCHANGED); cache.put(path, image); } else { IplImage
     * cachedImage = cache.get(path); if (image != null) { image =
     * cachedImage.clone(); } else { log.error("could not get cached image {}",
     * path); }
     * 
     * }
     */

    ++frameCounter;

    if (frameCounter > 1) {
      if (lastImage != null) {
        lastImage.release();
      }
    }

    lastImage = image;

    // increment out count.
    grabCount++;
    grabCount = grabCount % imageFiles.size();

    return converter.convert(image);
  }

  public int getDelay() {
    return delay;
  }

  public void setDelay(int delay) {
    this.delay = delay;
  }

  public String getDirectory() {
    return path;
  }

  public void setDirectory(String directory) {
    this.path = directory;
  }

  @Override
  public void start() throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void stop() throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void trigger() throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void release() throws Exception {
    // TODO Auto-generated method stub

  }

}
