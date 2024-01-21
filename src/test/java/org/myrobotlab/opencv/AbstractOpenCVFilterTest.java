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
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.test.AbstractTest;

// ignore the abstract classes.
@Ignore
public abstract class AbstractOpenCVFilterTest extends AbstractTest {

  public static final String CVSERVICENAME = "opencv";
  public boolean debug = false;

  public abstract OpenCVFilter createFilter();

  public abstract IplImage createTestImage();

  private CanvasFrame sourceImage = null;
  private CanvasFrame outputImage = null;
  public int frameIndex = 0;
  public int numFrames = 0;
  private CloseableFrameConverter converter1 = new CloseableFrameConverter();
  private CloseableFrameConverter converter2 = new CloseableFrameConverter();

  @Test
  public void testFilter() throws InterruptedException {
    List<OpenCVFilter> filters = createFilters();
    assertNotNull("Filter was null.", filters);
    List<IplImage> inputs = createTestImages();
    numFrames = inputs.size();
    OpenCV opencv = (OpenCV) Runtime.start("opencv", "OpenCV");

    for (OpenCVFilter filter : filters) {
      // Verify that the filters can be serialized!
      String json = CodecUtils.toJson(filter);
      assertNotNull(json);
      filter.setOpenCV(opencv);
    }

    for (IplImage input : inputs) {
      frameIndex++;
      long now = System.currentTimeMillis();
      // create the OpenCVData object that will run with this image through the
      // pipeline.
      OpenCVData data = new OpenCVData(CVSERVICENAME, now, frameIndex, converter1.toFrame(input));
      for (OpenCVFilter filter : filters) {
        if (debug) {
          sourceImage = filter.show(input, "Filter " + filter.name + " Input Image " + frameIndex);
        }
        // we need to set the CV Data object on the filter before we process.
        // This calls imageChanged .. (some filters initialize their stuff in that
        // method!
        IplImage output = processTestImage(filter, data, input, frameIndex);
        // TODO: we want to verify the resulting opencv data? and methods that are
        // invoked , we probably need to know what frame number it was.
        verify(filter, input, output);
      }
    }

    for (OpenCVFilter filter : filters) {
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
    }
    // clean up the other runtime stuffs
  }

  public List<OpenCVFilter> createFilters() {
    // default impl will just return a list from createFilter();
    ArrayList<OpenCVFilter> filters = new ArrayList<OpenCVFilter>();
    filters.add(createFilter());
    return filters;
  }

  public List<IplImage> createTestImages() {
    // Default behavior, return a list of one default test image.
    ArrayList<IplImage> images = new ArrayList<IplImage>();
    images.add(createTestImage());
    return images;
  }

  private IplImage processTestImage(OpenCVFilter filter, OpenCVData data, IplImage input, int frameIndex)
      throws InterruptedException {
    filter.setData(data);
    // call process on the filter with the input image.
    long start = System.currentTimeMillis();
    IplImage output = filter.process(input);
    filter.postProcess(output);
    long delta = System.currentTimeMillis() - start;
    log.info("Process method took {} ms", delta);
    filter.enabled = true;
    filter.displayEnabled = true;
    // verify that processDisplay doesn't blow up!
    BufferedImage bi = filter.processDisplay();

    if (debug) {
      IplImage displayVal = converter2.toImage(bi);
      outputImage = filter.show(displayVal, "Filter " + filter.name + " Output Image " + frameIndex);
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

  @After
  public void afterTest() {
    converter1.close();
    converter2.close();
  }
}
