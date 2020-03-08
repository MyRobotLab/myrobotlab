/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.cvCopy;
import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_UNCHANGED;
import static org.bytedeco.opencv.global.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.document.Classification;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.PointCloud;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

public abstract class OpenCVFilter implements Serializable {
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilter.class.toString());
  private static final long serialVersionUID = 1L;

  static public String getCacheFile(String url) {
    return OpenCV.getCacheFile(url);
  }

  static public String getImageFromUrl(String url) {
    return OpenCV.getImageFromUrl(url);
  }

  static private IplImage load(String filename) {
    return cvLoadImage(filename, IMREAD_UNCHANGED);
  }

  static public IplImage loadImage(String infile) {
    String tryfile = infile;

    if (tryfile.startsWith("http")) {
      tryfile = getImageFromUrl(tryfile);
    }

    // absolute file exists ?
    File f = new File(tryfile);
    if (f.exists()) {
      return load(tryfile);
    } else {
      log.warn("could load Mat {}", tryfile);
    }

    // service resources - when jar extracts ?
    tryfile = "resource" + File.separator + infile;
    f = new File(tryfile);
    if (f.exists()) {
      return load(tryfile);
    } else {
      log.warn("could load Mat {}", tryfile);
    }

    // source/ide
    // e.g. src\main\resources\resource\OpenCV
    tryfile = Util.getResourceDir() + File.separator + OpenCV.class.getSimpleName() + File.separator
        + infile;
    f = new File(tryfile);
    if (f.exists()) {
      return load(tryfile);
    } else {
      log.warn("could load Mat {}", tryfile);
    }

    // src\test\resources\OpenCV 
    tryfile = "src" + File.separator + "test" + File.separator + "resources" + File.separator + OpenCV.class.getSimpleName() + File.separator + infile;
    f = new File(tryfile);
    if (f.exists()) {
      return load(tryfile);
    } else {
      log.warn("could load Mat {}", tryfile);
    }

    log.error("could not load Mat {}", infile);
    return null;
  }

  static public Mat loadMat(String infile) {
    String tryfile = infile;

    if (tryfile.startsWith("http")) {
      tryfile = getImageFromUrl(tryfile);
    }

    // absolute file exists ?
    File f = new File(tryfile);
    if (f.exists()) {
      return read(tryfile); // load alpha
    } else {
      log.warn("could load Mat {}", tryfile);
    }

    // service resources - when jar extracts ?
    tryfile = Util.getResourceDir() + File.separator + infile;
    f = new File(tryfile);
    if (f.exists()) {
      return read(tryfile);
    } else {
      log.warn("could load Mat {}", tryfile);
    }

    // source/ide
    // e.g. src\main\resources\resource\OpenCV
    tryfile = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "resource" + File.separator + OpenCV.class.getSimpleName() + File.separator
        + infile;
    f = new File(tryfile);
    if (f.exists()) {
      return read(tryfile);
    } else {
      log.warn("could load Mat {}", tryfile);
    }

    // src\test\resources\OpenCV
    tryfile = "src" + File.separator + "test" + File.separator + "resources" + File.separator + OpenCV.class.getSimpleName() + File.separator + infile;
    f = new File(tryfile);
    if (f.exists()) {
      return read(tryfile);
    } else {
      log.warn("could load Mat {}", tryfile);
    }

    log.error("could not load Mat {}", infile);
    return null;
  }

  static public String putCacheFile(String url, byte[] data) {
    return putCacheFile(url, data);
  }

  static private Mat read(String filename) {
    return imread(filename, IMREAD_UNCHANGED);
  }

  /**
   * number of channels of incoming image
   */
  int channels;

  /**
   * converters for the filter
   */
  transient OpenCVFrameConverter.ToIplImage converterToImage = new OpenCVFrameConverter.ToIplImage();

  /**
   * converter for the filter
   */
  transient OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();

  /**
   * reference to the last OpenCVData processed and the one this filter will
   * modify
   */
  OpenCVData data;

  /**
   * color of display if any overlay
   */
  transient Color displayColor;

  /**
   * This allows the display method to be processed in the filter typically its
   * a conversion from opencv-jni-land to java-land and associated processing
   * for human consumption
   */
  boolean displayEnabled = false;

  boolean displayExport = false;

  /**
   * This will enable/disable the filter in the pipeline
   */
  boolean enabled = true;

  int height;

  // transient CvSize imageSize;

  transient Java2DFrameConverter jconverter = new Java2DFrameConverter();

  final public String name;

  transient protected OpenCV opencv;

  protected Boolean running;

  public String sourceKey;

  int width;

  public OpenCVFilter() {
    this(null);
  }

  public OpenCVFilter(String name) {
    if (name == null) {
      this.name = this.getClass().getSimpleName().substring("OpenCVFilter".length());
    } else {
      this.name = name;
    }
  }

  // TODO - refactor this back to single name constructor - the addFilter's new
  // responsiblity it to
  // check to see if inputkeys and other items are valid
  public OpenCVFilter(String filterName, String sourceKey) {
    this.name = filterName;
    this.sourceKey = sourceKey;
  }

  public void broadcastFilterState() {
    FilterWrapper fw = new FilterWrapper(this.name, this);
    if (opencv != null) {
      opencv.invoke("publishFilterState", fw);
    }
  }

  public IplImage copy(final IplImage image) {
    IplImage copy = cvCreateImage(image.cvSize(), image.depth(), image.nChannels());
    cvCopy(image, copy, null);
    return copy;
  }

  protected ImageIcon createImageIcon(String path, String description) {
    java.net.URL imgURL = getClass().getResource(path);
    if (imgURL != null) {
      return new ImageIcon(imgURL, description);
    } else {
      System.err.println("Couldn't find file: " + path);
      return null;
    }
  }

  public void disable() {
    enabled = false;
  }

  public void disableDisplay() {
    displayEnabled = false;
  }

  public void enable() {
    enabled = true;
  }

  public void enableDisplay() {
    displayEnabled = true;
  }

  public void error(String format, Object... args) {
    if (opencv == null) {
      log.error(String.format(format, args));
    } else {
      opencv.error(format, args);
    }
  }

  /**
   * This is NOT the filter's image, but really the output of the previous
   * filter ! to be used as input for "this" filters process method
   * 
   * @return - return IplImage
   */
  public IplImage getImage() {
    return data.getImage();
  }

  public OpenCV getOpenCV() {
    return opencv;
  }

  public ArrayList<String> getPossibleSources() {
    ArrayList<String> ret = new ArrayList<String>();
    ret.add(name);
    return ret;
  }

  public abstract void imageChanged(IplImage image);

  public void invoke(String method, Object... params) {
    opencv.invoke(method, params);
  }

  /**
   * put'ing all the data into output and/or display
   * @param processed - the already processed image
   */
  public void postProcess(IplImage processed) {
    data.put(processed);
  }

  public abstract IplImage process(IplImage image) throws InterruptedException;

  /**
   * method which determines if this filter to process its display TODO - have
   * it also decide if its cumulative display or not
   */
  public BufferedImage processDisplay() {

    if (enabled && displayEnabled) {
      // TODO - this determines our "source" of image
      // and appends meta data

      // to make a decision about "source" you have to put either
      // "current display" cv.display
      // previous buffered image <== aggregate
      // "input" buffered image ?
      BufferedImage input = null;

      // displayExport displayMeta displayEnabled enabled
      if (displayExport) {
        // FIXME - be direct ! data.data.getBufferedImage(filter.name)
        input = data.getBufferedImage();
      } else {
        // else cumulative display
        input = data.getDisplay();
      }

      if (input != null) {
        Graphics2D graphics = input.createGraphics();
        BufferedImage bi = processDisplay(graphics, input);
        data.put(bi);
        data.putDisplay(bi);
        return bi;
      }
    }
    return null;
  }

  abstract public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image);

  public void publishClassification(Map<String, List<Classification>> data) {
    if (opencv != null) {
      opencv.invoke("publishClassification", data);
    }
  }

  /**
   * <pre>
                Perhaps don't do a bunch of publishing points
                alot of different types of data can come from a pipeline
                color, depth point cloud, bounding boxes, faces, classifcations 
                Make it simple, make a single subscript where the data published can
                potentially contain all these things....
                
  public void publishPointCloud(PointCloud pointCloud) {
    if (opencv != null) {
      opencv.invoke("publishPointCloud", new Object[] {pointCloud});
    }
  }
   * </pre>
   */

  public void put(String keyPart, Object object) {
    data.put(keyPart, object);
  }

  /**
   * when a filter is removed from the pipeline its given a chance to return
   * resourcs
   */
  public void release() {
  }

  public void samplePoint(Integer x, Integer y) {
    //
    log.info("Sample point called " + x + " " + y);
  }

  public void saveToFile(String filename, IplImage image) {
    OpenCV.saveToFile(filename, image);
  }

  public IplImage setData(OpenCVData data) {
    this.data = data;
    data.setSelectedFilter(name);
    // FIXME - determine source of incoming image ...
    // FIXME - getImage(filter.sourceKey) => if null then use getImage()
    // grab the incoming image ..
    IplImage image = data.getOutputImage(); // <-- getting input from output
    if (image != null && (image.width() != width || image.nChannels() != channels)) {
      width = image.width();
      channels = image.nChannels();
      height = image.height();
      // imageSize = cvGetSize(image);
      imageChanged(image);
    }
    return image;
  }

  public void setOpenCV(OpenCV opencv) {
    if (displayColor == null) {
      displayColor = opencv.getColor();
    }
    this.opencv = opencv;
  }

  public OpenCVFilter setState(OpenCVFilter other) {
    return (OpenCVFilter) Service.copyShallowFrom(this, other);
  }

  public CanvasFrame show(final IplImage image, final String title) {
    CanvasFrame canvas = new CanvasFrame(title);
    // canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    canvas.showImage(toFrame(image));
    return canvas;
  }

  public void show(final Mat image, final String title) {
    CanvasFrame canvas = new CanvasFrame(title);
    // canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    canvas.showImage(toFrame(image));
  }

  /**
   * converting IplImages to BufferedImages
   */
  public BufferedImage toBufferedImage(IplImage image) {
    return jconverter.convert(converterToImage.convert(image));
  }

  public BufferedImage toBufferedImage(Mat image) {
    return jconverter.convert(converterToImage.convert(image));
  }

  public Frame toFrame(IplImage image) {
    return converterToImage.convert(image);
  }

  public Frame toFrame(Mat image) {
    return converterToImage.convert(image);
  }

  /**
   * convert BufferedImages to IplImages
   */
  public IplImage toImage(BufferedImage src) {
    return converterToImage.convert(jconverter.convert(src));
  }

  public IplImage toImage(Frame image) {
    return converterToImage.convertToIplImage(image);
  }

  public IplImage toImage(Mat image) {
    return converterToImage.convert(converterToMat.convert(image));
  }

  public Mat toMat(Frame image) {
    return converterToImage.convertToMat(image);
  }

  public Mat toMat(IplImage image) {
    return converterToMat.convert(converterToMat.convert(image));
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void put(PointCloud pc) {
    data.put(pc);
  }

}