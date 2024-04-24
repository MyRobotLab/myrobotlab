package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class OpenCVMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public OpenCVMeta() {

    addDescription("OpenCV (computer vision) service wrapping many of the functions and filters of OpenCV");
    addCategory("video", "vision", "sensors");
    String javaCvVersion = "1.5.8";
    // addDependency("org.bytedeco", "javacv", javaCvVersion);
    addDependency("org.bytedeco", "javacv-platform", javaCvVersion);
    addDependency("org.bytedeco", "javacpp", javaCvVersion);
    addDependency("org.bytedeco", "openblas", "0.3.21-" + javaCvVersion);
    // FIXME - finish with cmdLine flag -gpu vs cudaEnabled for DL4J ?
    boolean gpu = false;
    if (gpu) {
      // TODO: integrate in the following dependencies for GPU support in
      // OpenCV.
      // add additional metadata dependencies.
      // <dependency>
      // <groupId>org.bytedeco.javacpp-presets</groupId>
      // <artifactId>opencv</artifactId>
      // <version>3.4.1-1.4.1</version>
      // <classifier>linux-x86_64-gpu</classifier>
      // </dependency>
      // <dependency>
      // <groupId>org.bytedeco.javacpp-presets</groupId>
      // <artifactId>opencv</artifactId>
      // <version>3.4.1-1.4.1</version>
      // <classifier>macosx-x86_64-gpu</classifier>
      // </dependency>
      // <dependency>
      // <groupId>org.bytedeco.javacpp-presets</groupId>
      // <artifactId>opencv</artifactId>
      // <version>3.4.1-1.4.1</version>
      // <classifier>windows-x86_64-gpu</classifier>
      // </dependency>
    }
    // sarxos webcam
    // addDependency("com.github.sarxos", "webcam-capture", "0.3.10");
    addDependency("com.github.sarxos", "webcam-capture-driver-v4l4j", "0.3.13-SNAPSHOT");

    // FaceRecognizer no worky if missing it
    addDependency("org.apache.commons", "commons-lang3", "3.3.2");
    // for the mjpeg streamer frame grabber
    addDependency("net.sf.jipcam", "jipcam", "0.9.1");
    exclude("javax.servlet", "servlet-api");
    exclude("log4j", "log4j");
    exclude("org.apache.logging.log4j", "log4j-slf4j-impl");

    // jipcam use commons-lang-1.0 it break marySpeech
    exclude("commons-lang", "commons-lang");
    addDependency("commons-lang", "commons-lang", "2.6");
    // the haar / hog / lp classifier xml files for opencv from the MRL repo
    addDependency("opencv", "opencv_classifiers", "0.0.2", "zip");
    // the DNN Face Detection module
    addDependency("opencv", "opencv_facedetectdnn", "1.0.1", "zip");
    // text detection using EAST classifier
    addDependency("opencv", "opencv_east_text_detection", "0.0.1", "zip");
    // youtube downloader
    // addDependency("com.github.axet", "vget", "1.1.34"); NO LONGER WORKS
    // yolo models
    addDependency("yolo", "yolov2", "0.0.2", "zip");

  }

}
