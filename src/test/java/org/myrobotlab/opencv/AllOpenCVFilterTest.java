package org.myrobotlab.opencv;

import org.myrobotlab.service.OpenCV;
import org.myrobotlab.test.AbstractTest;

public class AllOpenCVFilterTest extends AbstractTest {

  public void testAllFilters() {
    OpenCV cv = new OpenCV("opencv");
    for (String filter : OpenCV.POSSIBLE_FILTERS) {
      // 
    }
  }
  
}
