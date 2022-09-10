package org.myrobotlab.opencv;

import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;

/**
 * Test a sample image through text detection and ocr
 */
public class OpenCVFilterTextDetectorTest extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    OpenCVFilterTextDetector filter = new OpenCVFilterTextDetector("td");
    // filter.xPadding = 0;
    return filter;
  }

  @Override
  public IplImage createTestImage() {
    String filename = "src/test/resources/OpenCV/hiring_humans.jpg";
    return cvLoadImage(filename);
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    log.info("CVData: {}", filter.data);
    assertNotNull(output);
    // assert that we got something semi readable
    String fullString = stitchText(filter);
    // System.out.println("TEXT: >>>" + fullString + "<<");
    // waitOnAnyKey();

    String expected = "WERE STILL . HIRING IUMANS Carnegie 2 Robotics. AMAR)";
    fullString = fullString.toLowerCase();
    assertTrue(fullString.contains("robotics"));
    assertTrue(fullString.contains("carnegie"));
    assertTrue(fullString.contains("hiring"));   
    // assertEquals(expected, fullString);
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
