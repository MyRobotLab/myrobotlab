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
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.beans.Transient;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.myrobotlab.cv.CVFilter;
import org.myrobotlab.document.Classification;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.PointCloud;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

public abstract class OpenCVFilter implements Serializable, CVFilter {
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilter.class.toString());

  private static final long serialVersionUID = 1L;

  protected String type = null;

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
    tryfile = Service.getResourceDir(OpenCV.class, infile);
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
    }

    // service resources - when jar extracts ?
    tryfile = Service.getResourceDir(OpenCV.class, infile);
    f = new File(tryfile);
    if (f.exists()) {
      return read(tryfile);
    }

    // source/ide
    // e.g. src\main\resources\resource\OpenCV
    tryfile = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "resource" + File.separator + OpenCV.class.getSimpleName() + File.separator
        + infile;
    f = new File(tryfile);
    if (f.exists()) {
      return read(tryfile);
    }

    // src\test\resources\OpenCV
    tryfile = "src" + File.separator + "test" + File.separator + "resources" + File.separator + OpenCV.class.getSimpleName() + File.separator + infile;
    f = new File(tryfile);
    if (f.exists()) {
      return read(tryfile);
    }

    log.error("could not load Mat {}", infile);
    return null;
  }

  static private Mat read(String filename) {
    return imread(filename, IMREAD_UNCHANGED);
  }

  /**
   * number of channels of incoming image
   */
  protected int channels;

  /**
   * reference to the last OpenCVData processed and the one this filter will
   * modify
   */
  transient protected OpenCVData data;

  /**
   * color of display if any overlay
   */
  transient protected Color displayColor;

  /**
   * This allows the display method to be processed in the filter typically its
   * a conversion from opencv-jni-land to java-land and associated processing
   * for human consumption
   */
  protected boolean displayEnabled = false;

  protected boolean displayExport = false;

  /**
   * This will enable/disable the filter in the pipeline
   */
  protected volatile boolean enabled = true;

  protected int height;

  final public String name;

  transient protected OpenCV opencv;

  private String sourceKey;

  protected int width;

  public OpenCVFilter() {
    this(null);
  }

  public OpenCVFilter(String name) {
    if (name == null) {
      this.name = this.getClass().getSimpleName().substring("OpenCVFilter".length());
    } else {
      this.name = name;
    }
    this.type = this.getClass().getSimpleName().substring("OpenCVFilter".length());
  }

  // TODO - refactor this back to single name constructor - the addFilter's new
  // responsiblity it to
  // check to see if inputkeys and other items are valid
  public OpenCVFilter(String filterName, String sourceKey) {
    this.name = filterName;
    this.setSourceKey(sourceKey);
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

  @Override
  public void disable() {
    enabled = false;
  }

  public void disableDisplay() {
    displayEnabled = false;
  }

  @Override
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
   * 
   * @param processed
   *          - the already processed image
   */
  public void postProcess(IplImage processed) {
    data.put(processed);
  }

  public abstract IplImage process(IplImage image) throws InterruptedException;

  /**
   * method which determines if this filter to process its display TODO - have
   * it also decide if its cumulative display or not
   * 
   * @return the buffered image to display
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
   * 
   * @param keyPart
   *          the key
   * @param object
   *          the object
   */

  public void put(String keyPart, Object object) {
    data.put(keyPart, object);
  }

  /**
   * when a filter is removed from the pipeline its given a chance to return
   * resources
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

  @Transient /* annotation to remove from yml dump */
  public void setOpenCV(OpenCV opencv) {
    if (displayColor == null) {
      displayColor = opencv.getColor();
    }
    this.opencv = opencv;
  }

  public OpenCVFilter setState(OpenCVFilter other) {
    return (OpenCVFilter) Service.copyShallowFrom(this, other);
  }

  public static CanvasFrame show(final IplImage image, final String title) {
    CanvasFrame canvas = new CanvasFrame(title);
    // canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    CloseableFrameConverter convert = new CloseableFrameConverter();
    canvas.showImage(convert.toFrame(image));
    // TODO: verify that this doesn't blow up
    convert.close();
    return canvas;
  }

  public static void show(final Mat image, final String title) {
    CanvasFrame canvas = new CanvasFrame(title);
    // canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    CloseableFrameConverter conv = new CloseableFrameConverter();
    canvas.showImage(conv.toFrame(image));
    // TODO: does this cause the canvas to blow up?
    conv.close();
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public void put(PointCloud pc) {
    data.put(pc);
  }

  public String getSourceKey() {
    return sourceKey;
  }

  public void setSourceKey(String sourceKey) {
    this.sourceKey = sourceKey;
  }

}
