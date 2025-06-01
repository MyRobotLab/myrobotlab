package org.myrobotlab.opencv;

import static org.junit.Assert.assertNotNull;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.junit.Test;
import org.myrobotlab.test.AbstractTest;

public class ImageFileFrameGrabberTest extends AbstractTest {

  @Test
  public void testImageFileFrameGrabber() throws Exception {
    String testImagePath = "src/test/resources/OpenCV";
    ImageFileFrameGrabber grabber = new ImageFileFrameGrabber(testImagePath);
    for (int i = 0 ; i < 20 ; i++) {
      Frame f = grabber.grab();
      assertNotNull(f);
    }
    grabber.close();

  }
}
