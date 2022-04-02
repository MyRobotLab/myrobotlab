package org.myrobotlab.opencv;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;
import org.junit.Ignore;

// ignoring this test for now, because there's not an easy way
// to assert that it's working as expected because the classification
// happens on a different thread and doesn't show up in the cv data..
@Ignore
public class OpenCVFilterMiniXceptionTest extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = true;
  }

  @Override 
  public List<OpenCVFilter> createFilters() {
    // this requires a pipeline of filters..
    // first to detect a face.. second to run the emotion detection on the
    // detected bounding box.
    ArrayList<OpenCVFilter> filters = new ArrayList<OpenCVFilter>();
    // filters.add(new OpenCVFilterFaceDetect("face"));
    filters.add(new OpenCVFilterFaceDetectDNN("facednn"));
    filters.add(new OpenCVFilterMiniXception("mini"));
    return filters;
  }
  
  @Override
  public OpenCVFilter createFilter() {
    // we create a pipeline, not a single filter for this test.
    return null;
  }

  @Override
  public List<IplImage> createTestImages() {
    // we need two images. (same resolution i guess?
    ArrayList<IplImage> images = new ArrayList<IplImage>();
    images.add(defaultImage());
    images.add(defaultImage());
    return images;
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    // Make sure we found 5 faces.
    log.info("CVData: {}", filter.data);
    assertNotNull(output);
    // TODO: verify something useful.
    if (debug) {
    	waitOnAnyKey();
    }
  }

  @Override
  public IplImage createTestImage() {
    return defaultImage();
  }

}
