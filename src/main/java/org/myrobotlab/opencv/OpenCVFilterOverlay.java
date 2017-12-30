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

// http://stackoverflow.com/questions/11515072/how-to-identify-optimal-parameters-for-cvcanny-for-polygon-approximation
package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.cvAddWeighted;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvScalar;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_DUPLEX;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_COMPLEX_SMALL;
import static org.bytedeco.javacpp.opencv_imgproc.cvInitFont;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_calib3d.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_features2d.*;
import static org.bytedeco.javacpp.opencv_flann.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_ml.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;
import static org.bytedeco.javacpp.opencv_photo.*;
import static org.bytedeco.javacpp.opencv_shape.*;
import static org.bytedeco.javacpp.opencv_stitching.*;
import static org.bytedeco.javacpp.opencv_video.*;
import static org.bytedeco.javacpp.opencv_videostab.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;


// helpful url - https://www.pyimagesearch.com/2016/04/25/watermarking-images-with-opencv-and-python/
public class OpenCVFilterOverlay extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterOverlay.class);

  public class ImageOverlay {
    public IplImage image = null;
    public double alpha = 0.5;

    public ImageOverlay(String imageFilename, double alpha) {
      this.image = cvLoadImage(imageFilename, CV_LOAD_IMAGE_UNCHANGED);
      // this.image = cvLoadImage(imageFilename, -1); // -1 == unchanged which supports 4 channel transparencies
      // this.image = cvLoadImage(imageFilename);
      this.alpha = alpha;
    }
  }
  
  public class TextOverlay {
    public String key;
    public String text;
    public int x;
    public int y;
    public CvFont font;
    public CvScalar color;
    
    public TextOverlay(String key, int x, int y, double size, String text){
      this(key, x, y, size, text, 0, 0, 0);
    }
    
    public TextOverlay(String key, int x, int y, double size, String text, double red, double green, double blue) {
      this.key = key;
      this.x = x;
      this.y = y;
      this.font = new CvFont();
      this.text = text;
      //cvInitFont(font, CV_FONT_HERSHEY_COMPLEX_SMALL, 0.5, 0.5);
      cvInitFont(font, CV_FONT_HERSHEY_PLAIN, size, size);
      
      color = cvScalar( blue, green, red, 0 );
      // cvInitFont(font, CV_FONT_HERSHEY_DUPLEX, 0.4, 0.4);
    }
    
  }
  
  transient List<ImageOverlay> imageOverlays = new ArrayList<ImageOverlay>();
  transient Map<String, TextOverlay> textOverlays = new LinkedHashMap<String,TextOverlay>();

  public OpenCVFilterOverlay() {
    super();
  }

  public OpenCVFilterOverlay(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    // nothing needed to adjust
  }

  @Override
  public IplImage process(IplImage image, OpenCVData data) {
    // IPL_DEPTH_8U
    
    // make 4 channel buffer based on attributes of 3 channel image coming in...
    IplImage channel4 = IplImage.create(image.width(),image.height(), image.depth(), 4);
    
    // create "mixer" extra channel(2) --goes to--> channel(3)
    int from_to[] = { 0,0, 1,1, 2,2, 2,3 };
    
    //cvMixChannels(image, 1, channel4, 1, from_to, 4);
    cvMixChannels(image, 1, channel4, 1, from_to, 4);
   
    /*
    for (int i = 0; i < imageOverlays.size(); ++i) {
      ImageOverlay overlay = imageOverlays.get(i);      
      // cvAddWeighted(image, overlay.alpha, overlay.image, 1 - overlay.alpha, 0.0, image);
      // cvAddWeighted(overlay.image, overlay.alpha, channel4, 1 - overlay.alpha, 0.0, channel4);
      cvAddWeighted(channel4, overlay.alpha, overlay.image, 1 - overlay.alpha, 0.0, channel4);
    }
    
    for (TextOverlay overlay : textOverlays.values()){
      cvPutText(image, overlay.text, cvPoint(overlay.x, overlay.y), overlay.font, overlay.color);
    }
    */
    
    return imageOverlays.get(0).image;
  }

  public void addImage(String imageFilename, double alpha) {
    imageOverlays.add(new ImageOverlay(imageFilename, alpha));
  }

  public void addImage(String imageFilename) {
    addImage(imageFilename, 0.5);
  }
  
  public void addText(String key, int x, int y, double d, String text) {
    textOverlays.put(key, new TextOverlay(key, x, y, d, text));
  }
  
  public void clear(){
    imageOverlays = new ArrayList<ImageOverlay>();
  }

}
