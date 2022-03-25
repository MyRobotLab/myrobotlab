package org.myrobotlab.opencv;

import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.test.AbstractTest;

// ignore the abstract classes.
@Ignore
public abstract class AbstractOpenCVFilterTest extends AbstractTest {

  public boolean debug = false;

  public abstract OpenCVFilter createFilter();

  public abstract IplImage createTestImage();

  private CanvasFrame sourceImage = null;
  private CanvasFrame outputImage = null;

  @Test
  public void testFilter() throws InterruptedException {
    OpenCVFilter filter = createFilter();
    assertNotNull("Filter was null.", filter);
    List<IplImage> inputs = createTestImages();
    OpenCV opencv = (OpenCV) Runtime.start("opencv", "OpenCV");
    filter.setOpenCV(opencv);
    int frameIndex = -1;
    for (IplImage input : inputs) {
      frameIndex++;
      if (debug) {
        sourceImage = filter.show(input, "Input Image " + frameIndex);
      }
      // we need to set the CV Data object on the filter before we process.
      // This calls imageChanged .. (some filters initialize their stuff in that
      // method!
      long now = System.currentTimeMillis();
      IplImage output = processTestImage(filter, input, now, frameIndex);
      // TODO: we want to verify the resulting opencv data? and methods that are
      // invoked , we probably need to know what frame number it was.
      verify(filter, input, output);
    }

    // now test the sample point method
    // TODO: what happens if we pass in nulls and stuff to samplePoint?
    filter.samplePoint(0, 0);

    // Ok now we should probably enable / disable
    filter.enable();
    assertTrue(filter.enabled);

    filter.disable();
    assertTrue(!filter.enabled);

    filter.release();
    // TODO: release the filter?
    // Runtime.releaseService("opencv");
    Runtime.release("opencv");
    // other stuff that comes along with runtime to shutdown.
    Runtime.release("security");
    // Runtime.releaseAll();

    // clean up the other runtime stuffs
  }

  public List<IplImage> createTestImages() {
    // Default behavior, return a list of one default test image.
    ArrayList<IplImage> images = new ArrayList<IplImage>();
    images.add(createTestImage());
    return images;
  }

  private IplImage processTestImage(OpenCVFilter filter, IplImage input, long now, int frameIndex) throws InterruptedException {
    filter.setData(new OpenCVData("testimg", now, frameIndex, OpenCV.toFrame(input)));
    // call process on the filter with the input image.
    long start = System.currentTimeMillis();
    IplImage output = filter.process(input);
    long delta = System.currentTimeMillis() - start;
    log.info("Process method took {} ms", delta);
    filter.enabled = true;
    filter.displayEnabled = true;
    // verify that processDisplay doesn't blow up!
    BufferedImage bi = filter.processDisplay();
    if (debug) {
      IplImage displayVal = OpenCV.toImage(bi);
      outputImage = filter.show(displayVal, "Output Image");
    }
    return output;
  }

  public IplImage defaultImage() {
    // a default image to use
    IplImage lena = cvLoadImage("src/test/resources/OpenCV/rachel.jpg");
    
    return lena;
  }

  public abstract void verify(OpenCVFilter filter, IplImage input, IplImage output);

  // a helper function that can pause the VM until you press any key in the
  // console. (nice for debugging sometimes.)
  public void waitOnAnyKey() {
    // show the images side by side.. not sure what we should verify? how do we
    // get the opencv data?
    System.out.println("Press the any key...");
    try {
      System.in.read();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  // TODO: i'm not sure why i needed this? i think the idea is we want to clean
  // up the debug windows when done.
  // @After
  public void cleanup() {
    //
    if (debug) {
      // clean up debug images

      if (sourceImage != null) {
        sourceImage.paint(null);
      }
      if (outputImage != null) {
        outputImage.paint(null);
      }

    }
  }

}
