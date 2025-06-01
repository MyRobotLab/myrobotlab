package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_UNCHANGED;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.io.FilenameUtils;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * This is the ImageFileFrameGrabber. It can be used as a source of images to
 * the OpenCV service. This grabber will load all of the image files from a
 * specified directory and iterate thought them. The resulting images are
 * converted in to JavaCV Frames.
 * 
 */
public class ImageFileFrameGrabber extends FrameGrabber {
  public final static Logger log = LoggerFactory.getLogger(ImageFileFrameGrabber.class.getCanonicalName());

  private ArrayList<File> imageFiles = new ArrayList<File>();
  private int grabCount = 0;
  private transient Mat image;
  public String path;
  CloseableFrameConverter converter = new CloseableFrameConverter();

  // supported formats for imread
  // https://docs.opencv.org/4.5.3/d4/da8/group__imgcodecs.html#ga288b8b3da0892bd651fce07b3bbd3a56
  private transient HashSet<String> validFormats = new HashSet<String>(Arrays.asList("bmp", "jpg", "jpeg", "jpe", "jp2", "png", "tiff", "tif", "hdr", "pic"));

  public ImageFileFrameGrabber(String path) {
    this.path = path;
    load();
  }

  public void load() {
    if (path == null) {
      log.error("cannot load image from null path");
      return;
    }
    File target = new File(path);
    if (!target.isDirectory()) {
      imageFiles.add(target);
    } else {
      File[] listOfFiles = target.listFiles();
      for (File file : listOfFiles) {
        if (file.isFile()) {
          // if the file extension is one of the supported ones, let's assume
          // it's actually an image.
          String ext = FilenameUtils.getExtension(file.getName()).toLowerCase();
          if (validFormats.contains(ext)) {
            // It's an image file! (in theory)
            imageFiles.add(file);
          }
        }
      }
    }
  }

  @Override
  public Frame grab() {
    // Grab the file to load based on the grabCount
    path = imageFiles.get(grabCount).getAbsolutePath();
    log.debug("Grabbing file {} - {}", grabCount, path);
    // grab it.
    try {
      image = imread(path, IMREAD_UNCHANGED);
    } catch (Throwable e) {
      log.error("ImageFileFrameGrabber cvLoadImage threw - could not load {}", path, e);
      return null;
    }
    // increment our count.
    grabCount++;
    grabCount = grabCount % imageFiles.size();
    if (converter == null) {
      // not ready ?
      return null;
    }
    if (image == null) {
      return null;
    }
    return converter.toFrame(image);
  }

  public String getDirectory() {
    return path;
  }

  public void setDirectory(String directory) {
    this.path = directory;
  }

  @Override
  public void start() throws Exception {
    // Nothing done on start
  }

  @Override
  public void stop() throws Exception {
    // Nothing done on stop
  }

  @Override
  public void trigger() throws Exception {
    // nothing done on trigger
  }

  @Override
  public void release() throws Exception {
    // Close up the frame converter.
    converter.close();
  }

}
