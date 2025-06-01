package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_ximgproc.createSelectiveSearchSegmentation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_video;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_ximgproc.SelectiveSearchSegmentation;

/**
 * This stage uses the image segmentation support in OpenCV to extract
 * interesting regions from the image. This is based on :
 * https://www.pyimagesearch.com/2020/06/29/opencv-selective-search-for-object-detection/
 * 
 * @author kwatters
 *
 */
public class OpenCVFilterImageSegmenter extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  String method = "fast";

  RectVector regions = null;
  // default values
  int baseK = 150;
  int incrK = 150;
  float sigma = 0.8f;
  // purely for display purposes, only consider the first N regions?
  int numToDisplay = 50;
  transient private CloseableFrameConverter converter = new CloseableFrameConverter();

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

  @Override
  public void release() {
    super.release();
    converter.close();
  }

  private void initModel() {
    // Known issue with JavaCV, you need to load opencv_video first before
    // loading
    // the ximgproc class.
    Loader.load(opencv_video.class);
  }

  @Override
  public void imageChanged(IplImage image) {
    // NoOp
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    // set the image and segment it.
    SelectiveSearchSegmentation ss = createSelectiveSearchSegmentation();
    ss.setBaseImage(converter.toMat(image));
    if ("fast".equalsIgnoreCase(method)) {
      ss.switchToSelectiveSearchFast(baseK, incrK, sigma);
    } else {
      ss.switchToSelectiveSearchQuality(baseK, incrK, sigma);
    }

    regions = new RectVector();
    ss.process(regions);
    // TODO: an easier rect list object.
    data.put("regions", regions);
    // return the original image un-altered.
    return image;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    graphics.setColor(Color.RED);
    if (regions != null) {
      int i = 0;
      for (Rect r : regions.get()) {
        graphics.drawRect(r.x(), r.y(), r.width(), r.height());
        i++;
        if (i > numToDisplay) {
          break;
        }
      }
    }
    return image;
  }

}
