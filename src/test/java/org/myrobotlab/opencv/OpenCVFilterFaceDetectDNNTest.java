package org.myrobotlab.opencv;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.OpenCV;

import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class OpenCVFilterFaceDetectDNNTest {

  boolean debug = false;

  @Before
  public void setUp() {
    // LoggingFactory.init("WARN");
  }

  @Test
  public void testFaceFilterDNN() throws IOException {

    // opencv needs to be installed if it's not
    if (!Repo.getInstance().isInstalled("OpenCV"))
      Repo.getInstance().install("OpenCV");

    OpenCVFilterFaceDetectDNN filter = new OpenCVFilterFaceDetectDNN("filter");
    String filename = "src/test/resources/OpenCV/multipleFaces.jpg";
    // String filename = "pose.jpg";
    // String filename = "pose2.jpg";
    // String filename = "pose3.jpg";
    // String filename = "pose4.jpg";

    IplImage image = cvLoadImage(filename);
    filter.setData(new OpenCVData(filename, 0, 0, OpenCV.toFrame(image)));
    assertNotNull(image);
    if (debug)
      filter.show(image, "Image");
    IplImage result = filter.process(image);
    if (debug) {
      filter.enabled = true;
      filter.displayEnabled = true;
      BufferedImage bi = filter.processDisplay();
      IplImage displayVal = OpenCV.toImage(bi);
      filter.show(displayVal, "Resulting image");
      System.out.println("Press the any key...");
      System.in.read();
    }
    int numFound = filter.bb.size();
    assertEquals(numFound, 5);

  }

}
