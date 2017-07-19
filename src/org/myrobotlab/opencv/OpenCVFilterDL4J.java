package org.myrobotlab.opencv;

import java.io.IOException;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Deeplearning4j;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class OpenCVFilterDL4J extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterDL4J.class.getCanonicalName());

  private transient Deeplearning4j dl4j;
  
  public OpenCVFilterDL4J() {
    super();
    loadDL4j();
  }
  
  public OpenCVFilterDL4J(String name) {
    super(name);
    loadDL4j();
  }
  
  
  private void loadDL4j() {
    dl4j = (Deeplearning4j)Runtime.createAndStart("dl4j", "Deeplearning4j");
    log.info("Loading VGG 16 Model.");
    try {
      dl4j.loadVGG16();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      log.warn("Error loading vgg16 model!");
      return;
      
    }
    log.info("Done loading model..");
  }
  
  @Override
  public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {
    // we need a dl4j service as a peer! so we can call classify on it with an image.
    try {
      String result = dl4j.classifyImageVGG16(image);
      log.info("Classification Result : {}", result);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return image;
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub

  }

}
