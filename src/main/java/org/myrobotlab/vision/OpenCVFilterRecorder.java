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

import static org.bytedeco.javacpp.opencv_imgproc.cvDilate;

import java.util.HashMap;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameRecorder;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

public class OpenCVFilterRecorder extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterRecorder.class.getCanonicalName());

  public int numberOfIterations = 1;

  transient FrameRecorder outputFileStreams;
  
  // recording
  // boolean recordOutput = false;
  // boolean closeOutputs = false;

  boolean recording = false;
  FrameRecorder recorder;
  int frameRate = 15;
  int pixelFormat = 1;

  public OpenCVFilterRecorder() {
    super();
  }

  public OpenCVFilterRecorder(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  @Override
  public IplImage process(IplImage image, VisionData data) {

    cvDilate(image, image, null, numberOfIterations);
    if (recording) {
      if (recorder == null) {
        try {
          recorder = new OpenCVFrameRecorder(String.format("%s.%s.avi", processor.getName(), name), width,
              height);
          // recorder.setCodecID(CV_FOURCC('M','J','P','G'));
          // TODO - set frame rate to framerate
          recorder.setFrameRate(frameRate);
          recorder.setPixelFormat(pixelFormat);
          recorder.start();
        } catch (Exception e) {
          recorder = null;
          error("could not start recorder");
          log.error("error starting recorder", e);
        }
      }
      
      // GAH ! FIXME - i need a frame !
      // FIXME - FRAME_KEY for input
      // recorder.record(arg0);
    }
    return image;
  }

  /**
   * thread safe recording of avi
   * 
   * @param key
   *            - input, filter, or display
   * @param data
   */
  public void record(VisionData data) {
    try {

      /*
       * 
       <pre> TODO - implement
      if (closeOutputs) {
        OpenCVFrameRecorder output = (OpenCVFrameRecorder) outputFileStreams.get(recordingSource);
        outputFileStreams.remove(output);
        output.stop();
        output.release();
        recordOutput = false;
        closeOutputs = false;
      }
      */

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
