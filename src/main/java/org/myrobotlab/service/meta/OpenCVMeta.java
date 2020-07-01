package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class OpenCVMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(OpenCVMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.OpenCV");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("OpenCV (computer vision) service wrapping many of the functions and filters of OpenCV");
    meta.addCategory("video", "vision", "sensors");
    // meta.addPeer("streamer", "VideoStreamer", "video streaming service
    meta.addPeer("streamer", "streamer", "VideoStreamer", "Shared Video Streamer");
    String javaCvVersion = "1.5.3";
    meta.addDependency("org.bytedeco", "javacv", javaCvVersion);
    meta.addDependency("org.bytedeco", "javacv-platform", javaCvVersion);
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
    meta.addDependency("com.github.sarxos", "webcam-capture", "0.3.10");
    // FaceRecognizer no worky if missing it
    meta.addDependency("org.apache.commons", "commons-lang3", "3.3.2");
    // for the mjpeg streamer frame grabber
    meta.addDependency("net.sf.jipcam", "jipcam", "0.9.1");
    meta.exclude("javax.servlet", "servlet-api");
    // jipcam use commons-lang-1.0 it break marySpeech
    meta.exclude("commons-lang", "commons-lang");
    meta.addDependency("commons-lang", "commons-lang", "2.6");
    // the haar / hog / lp classifier xml files for opencv from the MRL repo
    meta.addDependency("opencv", "opencv_classifiers", "0.0.2", "zip");
    // the DNN Face Detection module
    meta.addDependency("opencv", "opencv_facedetectdnn", "1.0.1", "zip");
    // text detection using EAST classifier
    meta.addDependency("opencv", "opencv_east_text_detection", "0.0.1", "zip");
    // youtube downloader
    meta.addDependency("com.github.axet", "vget", "1.1.34");
    // yolo models
    meta.addDependency("yolo", "yolov2", "0.0.2", "zip");

    return meta;
  }
  
  
}

