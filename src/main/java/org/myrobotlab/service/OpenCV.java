/**
 *                    
 * @author grog, kwatters, moz4r, MaVo, and many others...
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_calib3d.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_features2d.*;
import static org.bytedeco.javacpp.opencv_flann.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_ml.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;
import static org.bytedeco.javacpp.opencv_photo.*;
import static org.bytedeco.javacpp.opencv_shape.*;
import static org.bytedeco.javacpp.opencv_stitching.*;
import static org.bytedeco.javacpp.opencv_video.*;
import static org.bytedeco.javacpp.opencv_videostab.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvPoint2D32f;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameGrabber.ImageMode;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameRecorder;
import org.bytedeco.javacv.OpenKinectFrameGrabber;
import org.myrobotlab.document.Classification;
import org.myrobotlab.document.Classifications;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.image.ColoredPoint;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.geometry.Point2df;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.opencv.OpenCVFilterFaceDetect;
import org.myrobotlab.opencv.OpenCVFilterKinectDepth;
import org.myrobotlab.opencv.OpenCVFilterYolo;
import org.myrobotlab.opencv.Overlay;
import org.myrobotlab.opencv.YoloDetectedObject;
import org.myrobotlab.reflection.Reflector;
import org.myrobotlab.service.abstracts.AbstractVideoSource;
import org.slf4j.Logger;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoFileInfo;
import com.github.axet.vget.info.VideoInfo;
/*
<pre>
// extremely useful list of static imports - since auto-complete won't work with statics
 
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_calib3d.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_features2d.*;
import static org.bytedeco.javacpp.opencv_flann.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_ml.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;
import static org.bytedeco.javacpp.opencv_photo.*;
import static org.bytedeco.javacpp.opencv_shape.*;
import static org.bytedeco.javacpp.opencv_stitching.*;
import static org.bytedeco.javacpp.opencv_video.*;
import static org.bytedeco.javacpp.opencv_videostab.*;

</pre>
*/

/**
 * 
 * OpenCV - This service provides webcam support and video image processing It
 * uses the JavaCV binding to the OpenCV library. OpenCV is a computer vision
 * library. You can create an OpenCV service and then add a pipeline of
 * OpenCVFilters to it to provide things like facial recognition, and
 * KLOpticalTracking
 * 
 * More Info about OpenCV : http://opencv.org/ JavaCV is maintained by Samuel
 * Audet : https://github.com/bytedeco/javacv
 * 
 */
public class OpenCV extends AbstractVideoSource {

  class VideoProcessor implements Runnable {

    @Override
    public void run() {
      try {
        synchronized (lock) {

          log.info("beginning capture");
          capturing = true;
          getGrabber();

          lengthInFrames = grabber.getLengthInFrames();
          lengthInTime = grabber.getLengthInTime();

          // Wait for the Kinect to heat up.
          int loops = 0;
          while (grabber.getClass() == OpenKinectFrameGrabber.class && lengthInFrames == 0 && loops < 200) {
            lengthInFrames = grabber.getLengthInFrames();
            lengthInTime = grabber.getLengthInTime();
            sleep(40);
            loops++;
          }
          lock.notifyAll();
        }
        
        while (capturing) {
          Frame newFrame = null;

          // if the 
          if (lengthInFrames < 0 || lengthInFrames > 1 || (lengthInFrames == 1 && frameIndex < 1) || inputSource.equals(INPUT_SOURCE_CAMERA)) {            
            newFrame = grabber.grab();
          }

          if (newFrame != null) {
            lastFrame = newFrame;
          } else if (newFrame == null && lastFrame != null) {
            newFrame = lastFrame.clone();
          } else {
            error("could not get valid frame");
            stopCapture();
          }
          
          frameStartTs = System.currentTimeMillis();
          ++frameIndex;
          
          data = new OpenCVData(getName(), frameStartTs, frameIndex, newFrame);
          

          if (grabber.getClass().equals(OpenKinectFrameGrabber.class)) {
            // by default this framegrabber returns video 
            // getGrabber will set the format to "depth" - which will grab depth by default
            // here we need ot add the video
            
            IplImage video = ((OpenKinectFrameGrabber)grabber).grabVideo();
            data.putKinect(toImage(newFrame), video);
          }
          
          processVideo(data);

          if (lengthInFrames > 1 && loop && frameIndex > lengthInFrames - 2) {
            grabber.setFrameNumber(0);
            frameIndex = 0;
          }
        } // end of while - no longer capturing

      } catch (Exception e) {
        log.error("getting grabber failed", e);
      }

      synchronized (lock) {
        capturing = false;
        videoThread = null;
        frameIndex = 0;
        if (grabber != null) {
          try {
            grabber.close();
          } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
          }
          grabber = null;
        }
        lock.notifyAll();
      }

      broadcastState();
      log.info("stopping capture");
      // notify all lock'd threads we are done

    }
  }

  transient final static public String BACKGROUND = "background";

  public static final String CACHE_DIR = OpenCV.class.getSimpleName();
  transient public static final String FILTER_DETECTOR = "Detector";
  transient public static final String FILTER_DILATE = "Dilate";
  transient public static final String FILTER_ERODE = "Erode";
  transient public static final String FILTER_FACE_DETECT = "FaceDetect";
  transient public static final String FILTER_FACE_RECOGNIZER = "FaceRecognizer";
  transient public static final String FILTER_FIND_CONTOURS = "FindContours";

  transient public static final String FILTER_GOOD_FEATURES_TO_TRACK = "GoodFeaturesToTrack";
  transient public static final String FILTER_GRAY = "Gray";
  // TODO - OpenCV constants / enums ? ... hmm not a big fan ...
  transient public static final String FILTER_LK_OPTICAL_TRACK = "LKOpticalTrack";
  transient public static final String FILTER_PYRAMID_DOWN = "PyramidDown";
  transient final static public String FOREGROUND = "foreground";
  static final Set<String> grabberTypes = new TreeSet<String>();
  public static final String INPUT_KEY = "input";

  // FIXME - make more simple
  transient public final static String INPUT_SOURCE_CAMERA = "camera";

  transient public final static String INPUT_SOURCE_FILE = "imagefile";

  transient public final static String INPUT_SOURCE_PIPELINE = "pipeline";

  public final static Logger log = LoggerFactory.getLogger(OpenCV.class);
  public static final String OUTPUT_KEY = "output";
  transient final static public String PART = "part";

  public final static String POSSIBLE_FILTERS[] = { "AdaptiveThreshold", "AddMask", "Affine", "And", "BoundingBoxToFile", "Canny", "ColorTrack", "Copy", "CreateHistogram",
      "Detector", "Dilate", "DL4J", "DL4JTransfer", "Erode", "FaceDetect", "FaceDetect2", "FaceDetectDNN", "FaceRecognizer", "FaceTraining", "Fauvist", "FindContours", "Flip",
      "FloodFill", "FloorFinder", "FloorFinder2", "GoodFeaturesToTrack", "Gray", "HoughLines2", "Hsv", "Input", "InRange", "KinectDepth", "KinectDepthMask", "KinectNavigate",
      "LKOpticalTrack", "Lloyd", "Mask", "MatchTemplate", "Mouse", "Not", "OpticalFlow", "Output", "Overlay", "PyramidDown", "PyramidUp", "ResetImageRoi", "Resize", "SampleArray",
      "SampleImage", "SetImageROI", "SimpleBlobDetector", "Smooth", "Solr", "Split", "SURF", "Tesseract", "Threshold", "Tracker", "Transpose", "Undistort", "Yolo", };

  static final long serialVersionUID = 1L;

  transient public final static String SOURCE_KINECT_DEPTH = "kinect.depth.IplImage";

  static {
    try {

      for (int i = 0; i < FrameGrabber.list.size(); ++i) {
        String ss = FrameGrabber.list.get(i);
        String fg = ss.substring(ss.lastIndexOf(".") + 1);
        grabberTypes.add(fg);
      }

      grabberTypes.add("Pipeline"); // to/from another opencv service
      grabberTypes.add("Sarxos");
      grabberTypes.add("MJpeg");
    } catch (Exception e) {
      log.error("initializing frame grabber types threw", e);
    }
  }

  static public Color getAwtColor(String color) {
    Color newColor = null;
    String c = color.toUpperCase().trim();
    if (c.equals("WHITE")) {
      return Color.WHITE;
    } else if (c.equals("GRAY")) {
      return Color.GRAY;
    } else if (c.equals("BLACK")) {
      return Color.BLACK;
    } else if (c.equals("RED")) {
      return Color.RED;
    } else if (c.equals("GREEN")) {
      return Color.GREEN;
    } else if (c.equals("BLUE")) {
      return Color.BLUE;
    } else if (c.equals("CYAN")) {
      return Color.CYAN;
    } else if (c.equals("MAGENTA")) {
      return Color.MAGENTA;
    } else if (c.equals("YELLOW")) {
      return Color.YELLOW;
    } else if (c.equals("BLACK")) {
      return Color.BLACK;
    } else if (color.length() == 6) {
      newColor = Color.decode("#" + color);
    } else {
      newColor = Color.decode(color);
    }

    if (newColor != null) {
      return newColor;
    } else {
      return Color.BLACK;
    }
  }

  static public CvScalar getColor(String color) {
    String c = color.toUpperCase().trim();
    if (c.equals("WHITE")) {
      return CvScalar.WHITE;
    } else if (c.equals("GRAY")) {
      return CvScalar.GRAY;
    } else if (c.equals("BLACK")) {
      return CvScalar.BLACK;
    } else if (c.equals("RED")) {
      return CvScalar.RED;
    } else if (c.equals("GREEN")) {
      return CvScalar.GREEN;
    } else if (c.equals("BLUE")) {
      return CvScalar.BLUE;
    } else if (c.equals("CYAN")) {
      return CvScalar.CYAN;
    } else if (c.equals("MAGENTA")) {
      return CvScalar.MAGENTA;
    } else if (c.equals("YELLOW")) {
      return CvScalar.YELLOW;
    } else {
      return CvScalar.BLACK;
    }
  }

  static public Set<String> getGrabberTypes() {
    return grabberTypes;
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(OpenCV.class.getCanonicalName());
    meta.addDescription("OpenCV (computer vision) service wrapping many of the functions and filters of OpenCV");
    meta.addCategory("video", "vision", "sensor");
    // meta.addPeer("streamer", "VideoStreamer", "video streaming service
    meta.sharePeer("streamer", "streamer", "VideoStreamer", "Shared Video Streamer");

    String javaCvVersion = "1.4.3";
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
    // meta.exclude("");

    // FaceRecognizer no worky if missing it
    meta.addDependency("org.apache.commons", "commons-lang3", "3.3.2");
    // for the mjpeg streamer frame grabber
    meta.addDependency("net.sf.jipcam", "jipcam", "0.9.1");
    meta.exclude("javax.servlet", "servlet-api");
    // jipcam use commons-lang-1.0 it break marySpeech
    meta.exclude("commons-lang", "commons-lang");
    meta.addDependency("commons-lang", "commons-lang", "2.6");

    // TODO: should be something about yolo here too..
    // maybe make the yolo filter download the model and cache it?
    // or have it as a dependency
    // TODO: the yolo model files are too large for artifactory.. it's limited
    // to 100mb currently

    // the haar / hog / lp classifier xml files for opencv from the MRL repo
    meta.addDependency("opencv", "opencv_classifiers", "0.0.1", "zip");

    // the DNN Face Detection module
    meta.addDependency("opencv", "opencv_facedetectdnn", "1.0.0", "zip");

    // youtube downloader
    meta.addDependency("com.github.axet", "vget", "1.1.34");

    // yolo models
    meta.addDependency("yolo", "yolov2", "v2", "zip");

    return meta;
  }

  /**
   * get the current list of possible filter types
   * 
   * @return
   */
  static public String[] getPossibleFilters() {
    return POSSIBLE_FILTERS;
  }

  public static void main(String[] args) throws Exception {

    // TODO - Avoidance / Navigation Service
    // ground plane
    // http://stackoverflow.com/questions/6641055/obstacle-avoidance-with-stereo-vision
    // radio lab - map cells location cells yatta yatta
    // lkoptical disparity motion Time To Contact
    // https://www.google.com/search?aq=0&oq=opencv+obst&gcx=c&sourceid=chrome&ie=UTF-8&q=opencv+obstacle+avoidance
    //
    // WebGui webgui = (WebGui)Runtime.start("webgui", "WebGui");
    LoggingFactory.init("warn");

    Runtime.start("gui", "SwingGui");
    Runtime.start("python", "Python");
    OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");
    cv.setGrabberType("OpenKinect");
   

    // cv.capture("src/test/resources/OpenCV/multipleFaces.jpg");
    
    OpenCVFilterKinectDepth depth = new OpenCVFilterKinectDepth("depth");
    cv.addFilter(depth);
    cv.capture();
    

    boolean done = true;
    if (done) {
      return;
    }

    OpenCVFilterYolo yolo = (OpenCVFilterYolo) cv.addFilter("yolo");

    // cv.capture("https://www.youtube.com/watch?v=zDO1Q_ox4vk"); // matrix
    cv.capture("src/test/resources/OpenCV/multipleFaces.jpg");
    // cv.capture("https://www.youtube.com/watch?v=rgoYYWCCDkM");
    // cv.capture("https://www.youtube.com/watch?v=rgoYYWCCDkM"); // dublin
    // FIXME - decompose into modular filters
    // cv.capture("https://www.youtube.com/watch?v=JqVWD-3PdZo");//
    // matrix-restaurant
    // cv.capture("https://www.youtube.com/watch?v=lPOXR4dXxDQ"); // matrix 30
    // min movie

    // OpenCVFilterFaceTraining filter = new
    // OpenCVFilterFaceTraining("training");
    // filter.mode = Mode.TRAIN;
    // cv.addFilter(filter);

    cv.addFilter("Yolo");

    // filter.load("C:\\mrl\\myrobotlab.opencv-fixes\\myrobotlab\\BoundingBoxToFile\\neo");
    // filter.mode = Mode.TRAIN;

    boolean leave = true;
    if (leave) {
      return;
    }

    for (String fn : OpenCV.POSSIBLE_FILTERS) {
      if (fn.startsWith("DL4J")) {
        continue;
      }
      log.warn("trying {}", fn);
      cv.addFilter(fn);
      sleep(100);
      cv.removeFilters();
    }

    for (int i = 0; i < 1000; ++i) {

      Map<String, List<Classification>> classifications = cv.getClassifications();

      StringBuilder sb = new StringBuilder("I found ");
      if (classifications != null && classifications.keySet().size() > 0) {

        for (String c : classifications.keySet()) {
          List<Classification> types = classifications.get(c);
          sb.append(types.size());
          sb.append(" ");
          sb.append(c);
          if (types.size() > 1) {
            sb.append("s");
          }
          sb.append(" ");
        }
        sb.append(".");
      } else {
        sb.append("nothing.");
      }

      log.warn(sb.toString());
    }

    cv.addFilter("yolo");
    cv.capture("https://www.youtube.com/watch?v=rgoYYWCCDkM"); // dublin street

    cv.capture("src/test/resources/OpenCV/multipleFaces.jpg");

    cv.capture(0);
    // cv.capture("C:\\mrl\\myrobotlab.opencv-fixes\\d.mp4"); dunno why
    // ffmpeg noWorky

    // check with
    // &timestart=
    cv.setColor("black");
    // cv.capture("src/test/resources/OpenCV/multipleFaces.jpg");
    // cv.capture("testAvi/rassegna2.avi");
    // cv.addFilter("GoodFeaturesToTrack");
    // cv.addFilter("Canny");
    // cv.addFilter("yolo");
    // cv.capture("googleimagesdownload/downloads/cats");
    // cv.capture("http://www.engr.colostate.edu/me/facil/dynamics/files/cbw3.avi");
    OpenCVFilterYolo yolo1 = new OpenCVFilterYolo("yolo");
    cv.addFilter(yolo1);
    log.info("here");

    // cv.capture();

  }

  /**
   * converting IplImages to BufferedImages
   */
  static public BufferedImage toBufferedImage(IplImage src) {
    OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
    Java2DFrameConverter converter = new Java2DFrameConverter();
    Frame frame = grabberConverter.convert(src);
    return converter.getBufferedImage(frame, 1);
  }
  
  public static BufferedImage toBufferedImage(Frame inputFrame) {
    Java2DFrameConverter converter = new Java2DFrameConverter();
    return converter.getBufferedImage(inputFrame);
  }

  static public Frame toFrame(IplImage image) {
    OpenCVFrameConverter.ToIplImage converterToImage = new OpenCVFrameConverter.ToIplImage();
    return converterToImage.convert(image);
  }

  static public Frame toFrame(Mat image) {
    OpenCVFrameConverter.ToIplImage converterToImage = new OpenCVFrameConverter.ToIplImage();
    return converterToImage.convert(image);
  }

  /**
   * convert BufferedImages to IplImages
   */
  static public IplImage toImage(BufferedImage src) {
    OpenCVFrameConverter.ToIplImage converterToImage = new OpenCVFrameConverter.ToIplImage();
    Java2DFrameConverter jconverter = new Java2DFrameConverter();
    return converterToImage.convert(jconverter.convert(src));
  }

  static public IplImage toImage(Frame image) {
    OpenCVFrameConverter.ToIplImage converterToImage = new OpenCVFrameConverter.ToIplImage();
    return converterToImage.convertToIplImage(image);
  }

  static public IplImage toImage(Mat image) {
    OpenCVFrameConverter.ToIplImage converterToImage = new OpenCVFrameConverter.ToIplImage();
    OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
    return converterToImage.convert(converterToMat.convert(image));
  }

  static public Mat toMat(Frame image) {
    OpenCVFrameConverter.ToIplImage converterToImage = new OpenCVFrameConverter.ToIplImage();
    return converterToImage.convertToMat(image);
  }

  static public Mat toMat(IplImage image) {
    OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
    return converterToMat.convert(converterToMat.convert(image));
  }

  transient BlockingQueue<Map<String, List<Classification>>> blockingClassification = new LinkedBlockingQueue<>();

  transient BlockingQueue<OpenCVData> blockingData = new LinkedBlockingQueue<>();
  int cameraIndex = 0;
  volatile boolean capturing = false;
  Classifications classifications = null;
  boolean closeOutputs = false;

  /**
   * color of overlays
   */
  transient Color color = getAwtColor("RED");

  OpenCVData data;
  private String displayFilter = "display";

  /**
   * all the filters in the pipeline
   */
  transient Map<String, OpenCVFilter> filters = new LinkedHashMap<String, OpenCVFilter>();

  transient CvFont font = new CvFont();

  String format = null;
  transient Frame frame;

  long frameEndTs;
  int frameIndex = 0;
  long frameStartTs;

  StringBuffer frameTitle = new StringBuffer();
  transient FrameGrabber grabber = null;

  String grabberType = null;

  Integer height = null;
  String inputFile = null;

  String inputSource = OpenCV.INPUT_SOURCE_CAMERA;

  transient Frame lastFrame;
  transient IplImage lastImage;
  // frame grabber properties
  int lengthInFrames = -1;

  long lengthInTime = -1;

  transient final Object lock = new Object();

  boolean loop = true;

  // mask for each named filter
  transient public HashMap<String, IplImage> masks = new HashMap<String, IplImage>();

  // nice to have something which won't trash the cpu
  // on a still picture
  Integer maxFps = 32;

  transient HashMap<String, FrameRecorder> outputFileStreams = new HashMap<String, FrameRecorder>();

  HashMap<String, Overlay> overlays = new HashMap<String, Overlay>();

  String pipelineSelected = null;

  boolean recording = false;

  String recordingSource = INPUT_KEY;

  transient SimpleDateFormat sdf = new SimpleDateFormat();

  // TODO: a peer, but in the future , we should use WebGui and it's http
  // container for this if possible.
  // GROG : .. perhaps just a filter in the pipeline could stream it via http
  transient VideoStreamer streamer;

  // Changed default to false. Otherwise multiple opencv instances will get a
  // port in use bind exception.
  // TODO: fix how the opencv service can stream video to the webgui.
  boolean streamerEnabled = false;

  // final Object lock = new Object();

  boolean undockDisplay = false;

  transient Thread videoThread = null;

  final private VideoProcessor vp = new VideoProcessor();

  Integer width = null;

  public OpenCV(String n) {
    super(n);
    putText(20, 20, "time:  %d");
    putText(20, 30, "frame: %d");
    File cacheDir = new File(CACHE_DIR);
    cacheDir.mkdirs();
  }

  synchronized public OpenCVFilter addFilter(OpenCVFilter filter) {
    filter.setOpenCV(this);

    // guard against putting same name filter in
    if (filters.containsKey(filter.name)) {
      warn("trying to add same named filter - %s - choose a different name", filter);
      return filters.get(filter.name);
    }

    // heh - protecting against concurrency the way Scala does it ;
    Map<String, OpenCVFilter> newFilters = new LinkedHashMap<>();
    newFilters.putAll(filters);
    // add new filter
    newFilters.put(filter.name, filter);
    // switch to new references
    filters = newFilters;
    setDisplayFilter(filter.name);
    broadcastState();
    return filter;
  }

  /**
   * add filter by type e.g. addFilter("Canny","Canny")
   * 
   * @param filterType
   * @return
   */
  public OpenCVFilter addFilter(String filterName) {
    String filterType = filterName.substring(0, 1).toUpperCase() + filterName.substring(1);
    return addFilter(filterName, filterType);
  }

  /**
   * add filter by name and type e.g. addFilter("c1","Canny")
   * 
   * @param name
   * @param filterType
   * @return
   */
  public OpenCVFilter addFilter(String name, String filterType) {
    String type = String.format("org.myrobotlab.opencv.OpenCVFilter%s", filterType);
    OpenCVFilter filter = (OpenCVFilter) Instantiator.getNewInstance(type, name);
    return addFilter(filter);
  }

  /**
   * capture starts the frame grabber and video processing threads
   */
  public void capture() {
    synchronized (lock) {
      if (capturing) {
        return;
      }
      if (!capturing) {
        videoThread = new Thread(vp, String.format("%s-video-processor", getName()));
        videoThread.start();
        try {
          // wait for capture thread to start
          lock.wait();
        } catch (InterruptedException e) {
        }
        broadcastState();
      }
    }
  }

  public void capture(FrameGrabber grabber) {
    stopCapture();
    this.grabber = grabber;
    capture();
  }

  /**
   * capture from a camera
   * 
   * @param cameraIndex
   */
  public void capture(int cameraIndex) {
    stopCapture();
    setInputSource(INPUT_SOURCE_CAMERA);
    setCameraIndex(cameraIndex);
    capture();
  }

  /**
   * The capture(filename) method is a very general capture function. "filename"
   * can be a local avi, mp4, or remote e.g. http://somewhere/movie.avi, or it
   * can be a jpeg, mjpeg, pdf, tif or many other filetypes. If a frame grabber
   * type is not specifically selected, it will attempt to select the "best"
   * frame grabber for the job. Typically this will the the FFmpeg grabber since
   * its the most capable of decoding different filetypes.
   * 
   * @param filename
   */
  public void capture(String filename) {
    stopCapture();
    setInputSource(INPUT_SOURCE_FILE);
    setInputFileName(filename);
    capture();
  }

  public void captureFromResourceFile(String filename) throws IOException {
    FileIO.extractResource(filename, filename);
    capture(filename);
  }

  public int getCameraIndex() {
    return this.cameraIndex;
  }

  /**
   * default 5 second wait
   * 
   * @return
   */
  public Map<String, List<Classification>> getClassifications() {
    return getClassifications(5000);
  }

  public Map<String, List<Classification>> getClassifications(int timeout) {
    blockingClassification.clear();
    Map<String, List<Classification>> ret = null;
    try {
      ret = blockingClassification.poll(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
    }
    return ret;
  }

  public Color getColor() {
    return color;
  }

  public BufferedImage getDisplay() {
    return data.getDisplay();
  }

  public String getDisplayFilter() {
    return displayFilter;
  }

  // FIXME - getFaces() blocks ..
  public OpenCVData getFaceDetect() {
    OpenCVFilterFaceDetect fd = new OpenCVFilterFaceDetect("face");
    addFilter(fd);
    OpenCVData d = getOpenCVData();
    removeFilter(fd.name);
    return d;
  }

  /**
   * get a filter by name
   * 
   * @param name
   * @return
   */
  public OpenCVFilter getFilter(String name) {
    return filters.get(name);
  }

  public List<OpenCVFilter> getFilters() {
    List<OpenCVFilter> ret = new ArrayList<>();
    ret.addAll(filters.values());
    return ret;
  }

  public Frame getFrame() {
    return lastFrame;
  }

  public int getFrameIndex() {
    return frameIndex;
  }

  public OpenCVData getGoodFeatures() {
    addFilter(FILTER_GOOD_FEATURES_TO_TRACK, FILTER_GOOD_FEATURES_TO_TRACK);
    OpenCVData d = getOpenCVData();
    removeFilter(FILTER_GOOD_FEATURES_TO_TRACK);
    return d;
  }

  public FrameGrabber getGrabber()
      throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, org.bytedeco.javacv.FrameGrabber.Exception {

    if (grabber != null) {
      return grabber;
    }

    String newGrabberType = null;

    if (inputSource == null) {
      inputSource = INPUT_SOURCE_CAMERA;
    }

    // FIXME - need more logic ! other framegrabbers don't make sense with some
    // sources ...
    // FFmpeg can to remote or local files movies or stills
    // the "rest" typically are camera specific

    if (grabberType == null && inputSource.equals(INPUT_SOURCE_FILE) && inputFile != null) {
      File isDir = new File(inputFile);
      if (isDir.isDirectory()) {
        grabberType = "SlideShow";
      } else {
        grabberType = "FFmpeg";
      }
    }

    if ((grabberType == null || grabberType.equals("FFmpeg")) && (inputSource.equals(INPUT_SOURCE_CAMERA))) {
      grabberType = "OpenCV";
    } else if ((grabberType == null || grabberType.equals("OpenCV")) && (inputSource.equals(INPUT_SOURCE_FILE))) {
      grabberType = "FFmpeg";
    }

    String prefixPath;
    if ("IPCamera".equals(grabberType) || "Pipeline".equals(grabberType) || "ImageFile".equals(grabberType) || "SlideShow".equals(grabberType) || "Sarxos".equals(grabberType)
        || "MJpeg".equals(grabberType)) {
      prefixPath = "org.myrobotlab.opencv.";
    } else {
      prefixPath = "org.bytedeco.javacv.";
    }

    newGrabberType = String.format("%s%sFrameGrabber", prefixPath, grabberType);

    // certain files are "not" supported out of the box by certain grabbers
    // ffmpeg is increadibly capable, however it won't do a youtube stream
    // so we have to download/cache it and change the filename
    if (inputFile != null && inputFile.startsWith("http") && inputFile.contains("youtube")) {
      try { // FIXME - put in own Service - along with Google Image Downloader..
        inputFile = getYouTube(inputFile);
      } catch (Exception e) {
        error(e);
      }
    } else if (inputFile != null && inputFile.startsWith("http")) {
      // inspect the mime-type coming back -
    }

    log.info(String.format("video source is %s", inputSource));
    Class<?>[] paramTypes = new Class[1];
    Object[] params = new Object[1];
    // TODO - determine by file type - what input it is
    if (OpenCV.INPUT_SOURCE_FILE.equals(inputSource)) {
      paramTypes[0] = String.class;
      params[0] = inputFile;
    } else if (OpenCV.INPUT_SOURCE_PIPELINE.equals(inputSource)) {
      paramTypes[0] = String.class;
      params[0] = pipelineSelected;
    } else {
      inputSource = OpenCV.INPUT_SOURCE_CAMERA;
      paramTypes[0] = Integer.TYPE;
      params[0] = cameraIndex;
    }

    log.info(String.format("attempting to get frame grabber %s format %s", grabberType, format));
    Class<?> nfg = Class.forName(newGrabberType);

    Constructor<?> c = nfg.getConstructor(paramTypes);
    FrameGrabber newGrabber = (FrameGrabber) c.newInstance(params);

    if (newGrabber == null) {
      log.error("no viable capture or frame grabber with input {}", newGrabberType);
      stopCapture();
    }

    grabber = newGrabber;
    
    if (grabber.getClass().equals(OpenKinectFrameGrabber.class)) {
      OpenKinectFrameGrabber g = (OpenKinectFrameGrabber)grabber;
      //g.setImageMode(ImageMode.COLOR); - this still gives grey - but 3 channels of 16 bit :P
      g.setImageMode(ImageMode.GRAY); // gray is 1 channel 16 bit
      // g.setFormat("depth");
      format = "depth";
    }
    

    if (format != null) {
      grabber.setFormat(format);
    }

    /*
     * if (grabber.getClass() == OpenKinectFrameGrabber.class) {
     * OpenKinectFrameGrabber kinect = (OpenKinectFrameGrabber) grabber;
     * 
     * // kinect.grab(); // kinect.grabVideo(); // kinect.grabDepth(); //
     * kinect.grabIR();
     * 
     * // what is the behavior of (kinect.grabDepth(), kinect.grabVideo(),
     * kinect.grabIR()) all at once - do these calls block ? // or just return
     * null ? how much time do they block ? is it worth getting IR ? //
     * data.putKinect(kinect.grabDepth(), kinect.grabVideo()); }
     */

    log.info(String.format("using %s", grabber.getClass().getCanonicalName()));

    // TODO other framegrabber parameters for frame grabber

    if (height != null) {
      grabber.setImageHeight(height);
    }

    if (width != null) {
      grabber.setImageWidth(width);
    }

    grabber.start();
    broadcastState();
    return grabber;
  }

  public String getGrabberType() {
    return grabberType;
  }

  public IplImage getImage() {
    return lastImage;
  }

  public String getInputFile() {
    return inputFile;
  }

  public String getInputSource() {
    return inputSource;
  }

  public OpenCVData getLastData() {
    return data;
  }

  public Integer getMaxFps() {
    return maxFps;
  }

  public OpenCVData getOpenCVData() {
    return getOpenCVData(500);
  }

  // FIXME - TODO track(type)

  public OpenCVData getOpenCVData(Integer timeout) {
    blockingData.clear();
    OpenCVData newData = null;
    try {
      newData = blockingData.poll(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
    }
    return newData;
  }

  public String getYouTube(String url) throws IOException {

    File cacheDir = new File(String.format("%s/%s-cache", getClass().getSimpleName(), getName()));
    cacheDir.mkdirs();

    // get video key
    int pos0 = url.indexOf("v=");
    int pos1 = url.indexOf("&", pos0);
    if (pos0 < 0) {
      throw new IOException(String.format("could not find youtube v= in url %s", url));
    }

    String key = null;
    if (pos1 > 0) {
      key = url.substring(pos0, pos1 - pos0);
    } else {
      key = url.substring(pos0 + "v=".length());
    }

    String[] files = cacheDir.list();
    for (String cacheFile : files) {
      if (cacheFile.startsWith(key)) {
        File f = new File(String.format("%s/%s", cacheDir.getAbsolutePath(), cacheFile));
        return f.getAbsolutePath();
      }
    }

    URL web = new URL(url);
    VGetParser parser = null;
    parser = VGet.parser(web);
    VideoInfo videoinfo = parser.info(web);

    VGet v = new VGet(videoinfo, cacheDir);
    v.extract();

    log.info("Title: " + videoinfo.getTitle());
    List<VideoFileInfo> list = videoinfo.getInfo();
    String filename = null;

    if (list != null) {
      for (VideoFileInfo d : list) {
        if (d.getContentType().startsWith("video")) {
          String type = d.getContentType();
          String ext = type.substring(type.indexOf("/") + 1);
          filename = String.format("%s/%s_%s.%s", cacheDir, key, videoinfo.getTitle(), ext);
          d.targetFile = new File(filename);

          // [OPTIONAL] setTarget file for each download source video/audio
          // use d.getContentType() to determine which or use
          // v.targetFile(dinfo, ext, conflict) to set name dynamically or
          // d.targetFile = new File("/Downloads/CustomName.mp3");
          // to set file name manually.
          log.info("Download URL: " + d.getSource());
          break;
        }
      }
    }

    v.download();
    return filename;
  }

  /**
   * Callback from the SwingGui (e.g. clicking on the display) routes to the
   * appropriate filter through this method.
   */
  public void invokeFilterMethod(String filterName, String method, Object... params) {
    OpenCVFilter filter = getFilter(filterName);
    if (filter != null) {
      Reflector.invokeMethod(filter, method, params);
    } else {
      log.error("invokeFilterMethod " + filterName + " does not exist");
    }
  }

  public boolean isCapturing() {
    return capturing;
  }

  public boolean isRecording() {
    return recording;
  }

  public boolean isStreamerEnabled() {
    return streamerEnabled;
  }

  public boolean isUndocked() {
    return undockDisplay;
  }

  synchronized public void pauseCapture() {
    synchronized (lock) {
      capturing = false;
    }
  }

  private void processVideo(OpenCVData data) throws org.bytedeco.javacv.FrameGrabber.Exception, InterruptedException {


    // process each filter
    // for (String filterName : filters.keySet()) {
    for (OpenCVFilter filter : filters.values()) {
      // OpenCVFilter filter = filters.get(filterName);
      IplImage input = filter.setData(data);
      if (input == null) {
        log.error("could not get setData image");
        continue;
      }

      // process the previous filter's output
      IplImage processed = filter.process(input);
      filter.postProcess(processed);
      filter.processDisplay();

    } // for each filter

    // get the display filter to process

    putText("frame: %d", frameIndex);
    putText("time:  %d", frameStartTs);

    BufferedImage display = data.getDisplay();
    if (display != null) {
      Graphics2D g2d = display.createGraphics();

      if (g2d != null) {
        // we have a display...
        for (Overlay overlay : overlays.values()) {
          if (overlay.color != null) {
            g2d.setColor(overlay.color);
          }
          // TODO - handle drawImage overlay !
          g2d.drawString(overlay.text, overlay.x, overlay.y);
        }

        /**
         * <pre>
         * // FIXME - publishDisplay is "NOT" used by the OpenCVGui ! -
         * // publishDisplay is more transportable - and potentially could be
         * // "standard"
         * // since BufferedImage is a standard Java Object (contents of
         * // OpenCVData are not)
         * // it has a nice standard name too, but OpenCVData has "way" more
         * // data ... what to do ?
         * </pre>
         */
        BufferedImage b = data.getDisplay();
        invoke("publishDisplay", new SerializableImage(b, displayFilter, frameIndex));
      }
    }

    // useful but chatty debug statement - dumps opencvdata
    // log.debug("data -> {}", data);

    // FIXME - should have had it
    invoke("publishOpenCVData", data);

    // FIXME - TODO
    // data.prepareToSerialize();
    // invoke("publishVideoData", data);

    // this has to be before record as
    // record uses the queue - this has the "issue" if
    // the consumer does not pickup-it will get stale
    if (blockingData.size() == 0) {
      blockingData.add(data);
    }

    if (recording) {
      record(data);
    }

    frameEndTs = System.currentTimeMillis();

    // delay if needed to maxFps
    if (maxFps != null && frameEndTs - frameStartTs < 1000 / maxFps) {
      sleep((1000 / maxFps) - (int) (frameEndTs - frameStartTs));
    }

    data.dispose();

  } // end processVideo

  // when containers are published the <T>ypes are unknown to the publishing
  // function
  public ArrayList<?> publish(ArrayList<?> polygons) {
    return polygons;
  }

  public ColoredPoint[] publish(ColoredPoint[] points) {
    return points;
  }

  public CvPoint publish(CvPoint point) {
    return point;
  }

  // CPP interface does not use array - but hides implementation
  public CvPoint2D32f publish(CvPoint2D32f features) {
    return features;
  }

  public double[] publish(double[] data) {
    return data;
  }

  public Point2df publish(Point2df point) {
    return point;
  }

  public Rectangle publish(Rectangle rectangle) {
    return rectangle;
  }

  public String publish(String value) {
    return value;
  }

  public Map<String, List<Classification>> publishClassification(Map<String, List<Classification>> data) {
    // log.info("Publish Classification in opencv!");
    // aggregate locally for fun - "better" is to send it to a search engine
    // Solr or Elasticsearch

    // template matching ?
    // so potentially frame by frame passes through here
    // and what we are interested in is - during the running of this service
    // we would like a little aggregation and potentially re-training
    // we want to know the general question
    // "what do you see" or "what is new" ? both have some direct or indirect
    // reference to
    // "time"
    if (blockingClassification.size() == 0) {
      blockingClassification.add(data);
    }
    return data;
  }

  /**
   * FIXME - input needs to be OpenCVData THIS IS NOT USED ! VideoProcessor NOW
   * DOES OpenCVData - this will return NULL REMOVE !!
   */
  public final SerializableImage publishDisplay(SerializableImage img) {
    return img;
  }

  /*
   * publishing method for filters - used internally
   * 
   * @return FilterWrapper solves the problem of multiple types being resolved
   * in the setFilterState(FilterWrapper data) method
   */
  public FilterWrapper publishFilterState(FilterWrapper filterWrapper) {
    return filterWrapper;
  }

  /*
   * publishing method for filters - uses string parameter for remote invocation
   * 
   * @return FilterWrapper solves the problem of multiple types being resolved
   * in the setFilterState(FilterWrapper data) method
   */
  public FilterWrapper publishFilterState(String name) {
    OpenCVFilter filter = getFilter(name);
    if (filter != null) {
      return new FilterWrapper(name, filter);
    } else {
      log.error(String.format("publishFilterState %s does not exist ", name));
    }

    return null;
  }

  public IplImage publishIplImageTemplate(IplImage img) {
    return img;
  }

  public Classification publishNewClassification(Classification classification) {
    info("found new %s %f", classification.getLabel(), classification.getConfidence());
    // pauseCapture();
    return classification;
  }

  // FIXME - I could see how this would be useful
  public void publishNoRecognizedFace() {

  }

  /**
   * the publishing point of all OpenCV goodies ! type conversion is held off
   * until asked for - then its cached SMART ! :)
   * 
   */
  public final OpenCVData publishOpenCVData(OpenCVData data) {
    return data;
  }

  public String publishRecognizedFace(String value) {
    return value;
  }

  public SerializableImage publishTemplate(String source, BufferedImage img, int frameIndex) {
    SerializableImage si = new SerializableImage(img, source, frameIndex);
    return si;
  }

  public ArrayList<YoloDetectedObject> publishYoloClassification(ArrayList<YoloDetectedObject> classifications) {
    return classifications;
  }

  public void putText(int x, int y, String format) {
    putText(x, y, format, null);
  }

  /**
   * creates a new overlay of text
   * 
   * @param format
   * @param x
   * @param y
   * @param color
   */
  public void putText(int x, int y, String format, String color) {
    Overlay overlay = new Overlay(x, y, format, color);
    overlays.put(format, overlay);
  }

  /**
   * the "light weight" put - it does not create any new cv objects
   * 
   * @param format
   * @param args
   */
  public void putText(String format, Object... args) {
    if (overlays.containsKey(format)) {
      Overlay overlay = overlays.get(format);
      overlay.text = String.format(format, args);
    } else {
      putText(format, 20, 10 * overlays.size(), "black");
    }
  }

  public void record() {
    recording = true;
  }

  /**
   * thread safe recording of avi
   * 
   * key- input, filter, or display
   */
  public void record(OpenCVData data) {
    try {

      if (!outputFileStreams.containsKey(recordingSource)) {
        String filename = String.format("%s-%d.avi", recordingSource, System.currentTimeMillis());

        FrameRecorder recorder = new OpenCVFrameRecorder(filename, frame.imageWidth, frame.imageHeight);
        recorder.setFrameRate(15);
        recorder.setPixelFormat(1);
        recorder.start();

        /**
         * <pre>
         * FrameRecorder recorder = new FFmpegFrameRecorder(filename, frame.imageWidth, frame.imageHeight, 1);
         * recorder.setFormat("flv");
         * // recorder.setSampleRate(frameRate); for audio
         * recorder.setFrameRate(maxFps);
         * recorder.start();
         * </pre>
         */

        outputFileStreams.put(recordingSource, recorder);
      }
      // TODO - add input, filter & display
      outputFileStreams.get(recordingSource).record(toFrame(data.getImage()));

      if (closeOutputs) {
        FrameRecorder output = (FrameRecorder) outputFileStreams.get(recordingSource);
        outputFileStreams.remove(recordingSource);
        output.stop();
        output.release();
        recording = false;
        closeOutputs = false;
      }
    } catch (Exception e) {
      log.error("record threw", e);
    }
  }

  /**
   * records a single frame to the filesystem from our data
   * 
   * @return
   */
  public String recordFrame() {
    try {
      OpenCVData d = getOpenCVData();
      return d.writeDisplay();
    } catch (Exception e) {
      error(e);
    }
    return null;
  }

  /**
   * remove a filter by name
   * 
   * @param name
   */
  synchronized public void removeFilter(String name) {
    if (filters.containsKey(name)) {
      Map<String, OpenCVFilter> newFilters = new LinkedHashMap<>();
      newFilters.putAll(filters);
      OpenCVFilter removed = newFilters.remove(name);
      removed.release();
      filters = newFilters;
      broadcastState();
    }
  }

  /**
   * remove all the filters in the pipeline
   */
  synchronized public void removeFilters() {
    for (OpenCVFilter filter : filters.values()) {
      filter.release();
    }
    filters = new LinkedHashMap<>();
    broadcastState();
  }

  public void removeOverlays() {
    overlays = new HashMap<String, Overlay>();
  }

  /**
   * a resume is the same as a capture - the only difference is if the capture
   * was "stopped" or "paused" - stop will cause the FrameGrabber to
   * re-initialize with frame 0
   */
  synchronized public void resumeCapture() {
    capture();
  }

  public void saveToFile(String filename, IplImage image) {
    try {
      int i = filename.lastIndexOf(".");
      String ext = "png";
      if (i > 0) {
        ext = filename.substring(i + 1).toLowerCase();
      }
      BufferedImage bi = toBufferedImage(image);
      FileOutputStream fos = new FileOutputStream(filename);
      ImageIO.write(bi, ext, new MemoryCacheImageOutputStream(fos));
      fos.close();
    } catch (IOException e) {
      log.error("saveToFile threw", e);
    }
  }

  public Integer setCameraIndex(Integer index) {
    this.cameraIndex = index;
    return index;
  }

  public void setColor(String colorStr) {
    color = getAwtColor(colorStr);
  }

  public void setDisplayFilter(String name) {
    displayFilter = name;
    OpenCVFilter filter = filters.get(name);
    if (filter == null) {
      return;
    }

    // turn off old filters - turn on new one
    for (OpenCVFilter f : filters.values()) {
      f.disableDisplay();
    }

    if (filter == null || "input".equals(name) || "output".equals(name)) {
      log.info("make select & inverse select");
    } else {
      filter.enableDisplay();
      filter.enable();
    }

  }

  /**
   * @param otherFilter
   *          - data from remote source
   * 
   *          This updates the filter with all the non-transient data in a
   *          remote copy through a reflective field update. If your filter has
   *          JNI members or pointer references it will break, mark all of
   *          these.
   */
  public void setFilterState(FilterWrapper otherFilter) {
    OpenCVFilter filter = getFilter(otherFilter.name);
    if (filter != null) {
      if (filter != otherFilter.filter) {
        Service.copyShallowFrom(filter, otherFilter.filter);
      }
    } else {
      error("setFilterState - could not find %s ", otherFilter.name);
    }
  }

  public String setGrabberType(String grabberType) {
    this.grabberType = grabberType;
    return grabberType;
  }

  public String setInputFileName(String inputFile) {
    this.inputFile = inputFile;
    return inputFile;
  }

  // FIXME either file or camera
  // camera or file => capture(int) or capture(String)
  public String setInputSource(String inputSource) {
    this.inputSource = inputSource;
    broadcastState();
    return inputSource;
  }

  public void setMask(String name, IplImage mask) {
    masks.put(name, mask);
  }

  /**
   * <pre>
   * public void stopCapturex() {
   *   info("stopping capture");
   *   try {
   *     capturing = false;
   * 
   *     synchronized (lock) {
   *       videoThread = null;
   *       if (grabber != null) {
   *         grabber.close();
   *         grabber = null;
   *       }
   *     }
   *   } catch (Exception e) {
   *     log.error("stopCapture threw", e);
   *   }
   *   broadcastState();
   * }
   * </pre>
   */
  public void setMaxFps(Integer fps) {
    if (fps == null || fps < 1 || fps > 1000) {
      maxFps = null;
    }
    maxFps = fps;
  }

  public void setPipeline(String pipeline) {
    this.pipelineSelected = pipeline;
    this.inputSource = "pipeline";
    this.grabberType = "org.myrobotlab.opencv.PipelineFrameGrabber";
  }

  public String setRecordingSource(String source) {
    recordingSource = source;
    return source;
  }

  public void setStreamerEnabled(boolean streamerEnabled) {
    this.streamerEnabled = streamerEnabled;
  }

  public void stopCapture() {
    synchronized (lock) {
      if (!capturing) {
        return;
      }
      capturing = false;
      // sleep(300);
      try {
        lock.wait();
      } catch (InterruptedException e) {
      }
    }
  }

  // FIXME - implement .. duh..
  public void stopRecording() {
    // recording = false;
    closeOutputs = true;
  }

  @Override
  public void stopService() {
    super.stopService();
    stopCapture();
  }
  
  public void setFormat(String format) {
    this.format = format;
    if (grabber != null) {
      grabber.setFormat(format);
    }
  }

  public String getFormat() {
    if (grabber != null) {
      format = grabber.getFormat();
    }
    return format;
  }
  
  public boolean undockDisplay(boolean b) {
    undockDisplay = b;
    broadcastState();
    return b;
  }

 
}