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

import static org.bytedeco.javacpp.opencv_core.cvGetSize;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import org.bytedeco.javacpp.opencv_core.CvSize;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

public abstract class OpenCVFilter implements Serializable {
  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilter.class.toString());

  // communal data store
  OpenCVData data;

  final public String name;

  boolean enabled = true;
  boolean displayEnabled = false;
  boolean displayExport = false;
  boolean displayMeta = false;

  /**
   * color of display if any overlay
   */
  transient Color displayColor;

  // FIXME - deprecate - not needed or duplicated or in OpenCV framework
  // pipeline...
  public boolean useFloatValues = true;
  public boolean publishDisplay = false;
  public boolean publishData = true;
  public boolean publishImage = false;

  // input image attributes
  int width;
  int height;
  int channels;

  transient CvSize imageSize;

  public String sourceKey;

  // TODO change name to opencv
  transient protected OpenCV opencv;

  protected transient Boolean running;

  public OpenCVFilter() {
    this.name = this.getClass().getSimpleName().substring("OpenCVFilter".length());
  }

  public OpenCVFilter(String name) {
    this.name = name;
  }

  // TODO - refactor this back to single name constructor - the addFilter's new
  // responsiblity it to
  // check to see if inputkeys and other items are valid
  public OpenCVFilter(String filterName, String sourceKey) {
    this.name = filterName;
    this.sourceKey = sourceKey;
  }

  public abstract IplImage process(IplImage image) throws InterruptedException;

  public abstract void imageChanged(IplImage image);

  public void setOpenCV(OpenCV opencv) {
    if (displayColor == null) {
      displayColor = opencv.getColor();
    }
    this.opencv = opencv;
  }

  public OpenCV getOpenCV() {
    return opencv;
  }

  public OpenCVFilter setState(OpenCVFilter other) {
    return (OpenCVFilter) Service.copyShallowFrom(this, other);
  }

  public void invoke(String method, Object... params) {
    opencv.invoke(method, params);
  }

  public void broadcastFilterState() {
    FilterWrapper fw = new FilterWrapper(this.name, this);
    opencv.invoke("publishFilterState", fw);
  }

  public ArrayList<String> getPossibleSources() {
    ArrayList<String> ret = new ArrayList<String>();
    ret.add(name);
    return ret;
  }

  /**
   * when a filter is removed from the pipeline its given a chance to return
   * resourcs
   */
  public void release() {
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

  public void samplePoint(Integer x, Integer y) {
    //
    log.info("Sample point called " + x + " " + y);
  }

  public IplImage setData(OpenCVData data) {
    this.data = data;
    data.setSelectedFilter(name);
    // FIXME - determine source of incoming image ...
    // FIXME - getImage(filter.sourceKey) => if null then use getImage()
    // grab the incoming image ..
    IplImage image = data.getOutputImage(); // <-- getting input from output

    if (image.width() != width || image.nChannels() != channels) {
      width = image.width();
      channels = image.nChannels();
      height = image.height();
      imageSize = cvGetSize(image);
      imageChanged(image);
    }
    return image;
  }

  public void enableDisplay(boolean b) {
    displayEnabled = b;
  }

  public void enable(boolean b) {
    enabled = b;
  }

  // GET THE BUFFERED IMAGE FROM "MY" Iplimage !!!!
  /*
  public BufferedImage getBufferedImage() {
    return data.getDisplay();
  }
  */

  // GET THE Graphics IMAGE FROM "MY" BufferedImage !!!!
  /*
   * public Graphics2D getGraphics() { return data.getGraphics(); }
   */

  /**
   * method which determines if this filter to process its display TODO - have
   * it also decide if its cumulative display or not
   */
  public void processDisplay() {

    if (enabled && displayEnabled) {
      // TODO - this determines our "source" of image
      // and appends meta data

      // to make a decision about "source" you have to put either
      // "current display" cv.display
      //  previous buffered image <== aggregate
      //  "input" buffered image ?
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
      }
    }
  }

  abstract public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image);

  /**
   * This is NOT the filter's image, but really the output of the previous
   * filter ! to be used as input for "this" filters process method
   * 
   * @return
   */
  public IplImage getImage() {
    return data.getImage();
  }

  /*
   * FIXME - TODO public Mat getMat() { return data.getMat(); }
   */

  public void put(String keyPart, Object object) {
    data.put(keyPart, object);
  }

  /**
   * put'ing all the data into output and/or display
   */
  public void postProcess(IplImage processed) {
    data.put(processed);
  }

}