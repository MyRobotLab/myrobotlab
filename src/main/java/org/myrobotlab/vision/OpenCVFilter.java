/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.vision;

import static org.bytedeco.javacpp.opencv_core.cvGetSize;

import java.io.Serializable;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.CvSize;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.VideoProcessor;
import org.slf4j.Logger;

public abstract class OpenCVFilter implements Serializable {

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilter.class.toString());

  private static final long serialVersionUID = 1L;
  final public String name;

  public boolean useFloatValues = true;

  public boolean publishDisplay = false;
  public boolean publishData = true;
  public boolean publishImage = false;

  int width;
  int height;
  int channels;
  int frameIndex;
  
  

  transient CvSize imageSize;

  public String sourceKey;

  transient protected VideoProcessor processor;

  /**
   * When a filter is locked it cannot be removed from 
   * processing.  This is useful when the user is interested
   * in dynamically removing all filters except for a set which
   * creates the initial video feed.  
   * 
   * For example - when we want to create a new pipeline,
   * we usually don't want to re-initialize all the inputs,
   * or pyramid down filters
   */
  boolean locked = false;

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

  public abstract IplImage process(IplImage image, VisionData data) throws InterruptedException;

  public IplImage display(IplImage image, VisionData data) {
    return image;
  }

  public abstract void imageChanged(IplImage image);

  public void setVideoProcessor(VideoProcessor vp) {
    this.processor = vp;
  }

  public VideoProcessor getVideoProcessor() {
    return processor;
  }

  public OpenCVFilter setState(OpenCVFilter other) {
    return (OpenCVFilter) Service.copyShallowFrom(this, other);
  }

  public IplImage preProcess(int frameIndex, IplImage frame, VisionData data) {
    if (frame != null && (frame.width() != width || frame.nChannels() != channels)) {
      width = frame.width();
      channels = frame.nChannels();
      height = frame.height();
      imageSize = cvGetSize(frame);
      imageChanged(frame);
      // Logging.logTime(String.format("image Changed !!! %s",
      // data.filtername));
    }
    return frame;
  }

  public void invoke(String method, Object... params) {
    processor.invoke(method, params);
  }

  public void broadcastFilterState() {
    FilterWrapper fw = new FilterWrapper(this.name, this);
    processor.invoke("publishFilterState", fw);
  }

  public ArrayList<String> getPossibleSources() {
    ArrayList<String> ret = new ArrayList<String>();
    ret.add(name);
    return ret;
  }

  public void info(String format, Object... args){
	  processor.info(format, args);
  }
  
  public void warn(String format, Object... args){
	  processor.warn(format, args);
  }

  public void error(String format, Object... args){
	  processor.error(format, args);
  }


  @Deprecated // not used remove ..
  public void release() {
  }

  /*
   * public IplImage postProcess(IplImage image, OpenCVData data) { return
   * image; }
   */

  public void samplePoint(Integer x, Integer y) {
    //
    log.info("Sample point called " + x + " " + y);
  }
  
  
  public void lock(Boolean b) {
   locked = b;
  }
  

  public boolean isLocked() {
    return locked;
  }
  
  

}
