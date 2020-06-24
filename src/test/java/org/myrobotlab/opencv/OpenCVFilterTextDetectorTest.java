package org.myrobotlab.opencv;

import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;

// @Ignore
public class OpenCVFilterTextDetectorTest  extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = true;
  }

  @Override
  public OpenCVFilter createFilter() {
    OpenCVFilterTextDetector filter = new OpenCVFilterTextDetector("td");
    return filter;
  }

  @Override
  public IplImage createTestImage() {
    // String filename = "src/test/resources/OpenCV/i_am_a_droid.jpg";
    String filename = "src/test/resources/OpenCV/hiring_humans.jpg";
    //String filename = "US Treasury/1.png";
    //String filename = "US Treasury/1a.png";
    // String filename = "US Treasury/1b.png";
    // String filename = "US Treasury/2.png";
    // String filename = "US Treasury/3.png";
    return cvLoadImage(filename);
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    log.info("CVData: {}", filter.data);
    assertNotNull(output);
    // assert that we got something semi readable
    String fullString = stitchText(filter);
    System.out.println("TEXT: " + fullString);
    //assertTrue(filter.data.getDetectedText().get(0).text.contains(" HIRING "));
    assertEquals("WERE STILL. HIRING UMANS Carneqie ) Robotics, (2l", fullString);
    waitOnAnyKey();
  }

  private String stitchText(OpenCVFilter filter) {
    StringBuilder fullText = new StringBuilder();
    for (DetectedText dt : filter.data.getDetectedText()) {
      fullText.append(dt.text.trim()).append(" ");
    }
    String fullString = fullText.toString().trim();
    return fullString;
  }

}
