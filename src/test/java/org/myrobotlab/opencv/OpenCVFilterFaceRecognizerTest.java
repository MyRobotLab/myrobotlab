package org.myrobotlab.opencv;

import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;
import static org.junit.Assert.assertNotNull;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;
import org.junit.Assert;

public class OpenCVFilterFaceRecognizerTest extends AbstractOpenCVFilterTest {
  transient public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFaceRecognizerTest.class);

  // a temporary folder for service tests to use
  @ClassRule
  public static TemporaryFolder testFolder = new TemporaryFolder();
  
  String baseDirectory = "src/test/resources/OpenCV/FaceRecognizer/";
  String[] names = new String[] {"Tony Stark", "Natasha Romanoff", "Steve Rogers"};

  @Before
  public void before() {
    debug = false;
    // LoggingFactory.init("DEBUG");
  }

  @Override
  public OpenCVFilter createFilter() {
    // create our test filter.
    log.info("Create filter.");
    OpenCVFilterFaceRecognizer filter = new OpenCVFilterFaceRecognizer("facerec");
    // set the training directory:
    filter.setTrainingDir(testFolder.getRoot().getAbsolutePath()+File.separator+"OpenCVFaceRecognizer");
    // Here we want to train the model.. 
    filter.setMode(OpenCVFilterFaceRecognizer.Mode.TRAIN);   
    // now we need to pass some images in for tony.
    for (String name : names) {
      filter.setTrainName(name);
      String testDir = baseDirectory + name;
      try {
        processDirectory(filter, testDir);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    // now that we've passed through each of the training images..
    // lets train the model
    log.info("Processed the directories.");
    try {
      filter.train();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    log.info("Training done.");
    // TODO: do i need to make this switch to recognize mode?
    filter.setMode(OpenCVFilterFaceRecognizer.Mode.RECOGNIZE);
    return filter;
  }

  private void processDirectory(OpenCVFilterFaceRecognizer filter, String directory) throws InterruptedException {
    // 
    filter.enabled = true;
    filter.displayEnabled = true;
    
    File dir = new File(directory);
    log.info("Directory : " + dir.getAbsolutePath());
    for (File f : dir.listFiles()) {
      // load this file as an image.
      log.info("Loading Image: {}", f.getAbsolutePath());
      IplImage image = cvLoadImage(f.getAbsolutePath());
      if (image == null) {
        System.out.print("Image unable to be loaded..." + f.getAbsolutePath());
        continue;
      }
      
      if (debug) {
        filter.show(image, "Input Image");
      }

      
      //filter.setData(new OpenCVData("testimg", 0 ,0 , OpenCV.toFrame(image)));
      filter.setData(new OpenCVData("testimg", 0 ,0 , OpenCV.toFrame(image)));
      filter.process(image);
      filter.enabled = true;
      filter.displayEnabled = true;
      // verify that processDisplay doesn't blow up!
      BufferedImage bi = filter.processDisplay();

      if (debug) {
        IplImage displayVal = OpenCV.toImage(bi);
        filter.show(displayVal, "Output Image");
      }

     // waitOnAnyKey();
    }
  }

  @Override
  public IplImage createTestImage() {
    // let's use lena.png.
    IplImage input = cvLoadImage(baseDirectory + "Test/1.jpg");
    assertNotNull("bad input image", input);
    return input;
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    // it should have found 1 face
    OpenCVFilterFaceRecognizer f = (OpenCVFilterFaceRecognizer)filter;
    // expected vs actual
    Assert.assertEquals("Natasha Romanoff", f.getLastRecognizedName());
    
    // waitOnAnyKey();
  }

}
