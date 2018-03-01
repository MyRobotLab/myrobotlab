package org.myrobotlab.integration;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVFilterYolo;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;

public class YoloFilterTest {

  public static void main(String[] args) {
    // TODO Auto-generated method stub

    
    Runtime.start("gui", "SwingGui");
    org.apache.log4j.BasicConfigurator.configure();
    LoggingFactory.getInstance().setLevel(Level.INFO);
    
    OpenCV opencv = (OpenCV) Runtime.start("opencv", "OpenCV");
    opencv.setStreamerEnabled(false);

    OpenCVFilterYolo yolo = new OpenCVFilterYolo("yolo");
    opencv.addFilter(yolo);

    
    opencv.setCameraIndex(0);
    opencv.capture();
    
  }

}
