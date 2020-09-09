package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_ximgproc.createSelectiveSearchSegmentation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_video;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_ximgproc.SelectiveSearchSegmentation;
import org.myrobotlab.service.OpenCV;

/**
 * This stage uses the image segmentation support in OpenCV to extract interesting regions from the image.
 * This is based on :  
 * 
 * @author kwatters
 *
 */
public class OpenCVFilterImageSegmenter extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  
  String method = "fast";
  SelectiveSearchSegmentation ss = null;
  RectVector regions = null;
  
  public OpenCVFilterImageSegmenter() {
    super();
    initModel();
  }

  public OpenCVFilterImageSegmenter(String filterName, String sourceKey) {
    super(filterName, sourceKey);
    initModel();
  }

  public OpenCVFilterImageSegmenter(String name) {
    super(name);
    initModel();
  }

  private void initModel() {
    // detector = readNet(modelFile);
    // TODO: for some reason, we need to explicitly load this class 
    // if it hasn't already been loaded prior to attempting to load
    // the ximgproc classes.  odd. 
    Loader.load(opencv_video.class);
    ss = createSelectiveSearchSegmentation();
    if ("fast".equalsIgnoreCase(method)) {
      ss.switchToSelectiveSearchFast();
    } else {
      ss.switchToSelectiveSearchQuality();
    }
  }

  @Override
  public void imageChanged(IplImage image) {
    // NoOp 
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    // set the image and segment it.
    ss.setBaseImage(OpenCV.toMat(image));
    regions = new RectVector();
    ss.process(regions);
    // TODO: an easier rect list object.
    this.data.put("regions", regions);
    // return the original image un-altered.
    return image;
  } 


  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    graphics.setColor(Color.RED);
    if (regions != null) {
      for (Rect r : regions.get()) {
        graphics.drawRect(r.x(), r.y(), r.width(), r.height());
      }
    }
    return image;
  }

}
