package org.myrobotlab.service;

import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;

import java.util.HashMap;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.myrobotlab.framework.Service;

public class Deeplearning4jTest extends AbstractServiceTest {

  @Override
  public Service createService() throws Exception {
    // TODO Auto-generated method stub
    Deeplearning4j dl4j = (Deeplearning4j)Runtime.start("dl4j", "Deeplearning4j");
    return dl4j;
  }

  @Override
  public void testService() throws Exception {
    // TODO Auto-generated method stub
    Deeplearning4j dl4j = (Deeplearning4j)service;
    
    // miniXception test.
    dl4j.loadMiniEXCEPTION();
    double confidence = 0.001;
    IplImage testImage = cvLoadImage("src/test/resources/OpenCV/rachel.jpg");
    HashMap<String,Double> res = dl4j.classifyImageMiniEXCEPTION(testImage, confidence);
    System.out.println(res);
  }

}
