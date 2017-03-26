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

package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.cvGetSize;

import java.io.Serializable;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.CvSize;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
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

  transient protected VideoProcessor vp;

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

  public abstract IplImage process(IplImage image, OpenCVData data) throws InterruptedException;

  public IplImage display(IplImage image, OpenCVData data) {
    return image;
  }

  public abstract void imageChanged(IplImage image);

  public void setVideoProcessor(VideoProcessor vp) {
    this.vp = vp;
  }

  public VideoProcessor getVideoProcessor() {
    return vp;
  }

  public OpenCVFilter setState(OpenCVFilter other) {
    return (OpenCVFilter) Service.copyShallowFrom(this, other);
  }

  public IplImage preProcess(int frameIndex, IplImage frame, OpenCVData data) {
    // Logging.logTime(String.format("preProcess begin %s", data.filtername));
    if (frame.width() != width || frame.nChannels() != channels) {
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
    vp.getOpencv().invoke(method, params);
  }

  public void broadcastFilterState() {
    FilterWrapper fw = new FilterWrapper(this.name, this);
    vp.getOpencv().invoke("publishFilterState", fw);
  }

  public ArrayList<String> getPossibleSources() {
    ArrayList<String> ret = new ArrayList<String>();
    ret.add(name);
    return ret;
  }

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

}
