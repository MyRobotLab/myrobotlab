/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvPoint2D32f;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameRecorder;
import org.bytedeco.javacv.OpenKinectFrameGrabber;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.image.ColoredPoint;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.opencv.OpenCVFilterFaceDetect;
import org.myrobotlab.opencv.Overlay;
import org.myrobotlab.opencv.YoloDetectedObject;
import org.myrobotlab.reflection.Reflector;
import org.myrobotlab.service.abstracts.AbstractVideoSource;
import org.myrobotlab.service.data.Point2Df;
import org.slf4j.Logger;

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

  // FIXME - don't return BufferedImage return SerializableImage always !

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCV.class);

  // FIXME - make more simple
  transient public final static String INPUT_SOURCE_CAMERA = "camera";
  transient public final static String INPUT_SOURCE_PIPELINE = "pipeline";
  transient public final static String INPUT_SOURCE_IMAGE_FILE = "imagefile";

  // TODO - OpenCV constants / enums ? ... hmm not a big fan ...
  transient public static final String FILTER_LK_OPTICAL_TRACK = "LKOpticalTrack";
  transient public static final String FILTER_PYRAMID_DOWN = "PyramidDown";
  transient public static final String FILTER_GOOD_FEATURES_TO_TRACK = "GoodFeaturesToTrack";
  transient public static final String FILTER_DETECTOR = "Detector";
  transient public static final String FILTER_ERODE = "Erode";
  transient public static final String FILTER_DILATE = "Dilate";
  transient public static final String FILTER_FIND_CONTOURS = "FindContours";
  transient public static final String FILTER_FACE_DETECT = "FaceDetect";
  transient public static final String FILTER_FACE_RECOGNIZER = "FaceRecognizer";
  transient public static final String FILTER_GRAY = "Gray";

  // directional constants
  transient final static public String DIRECTION_FARTHEST_FROM_CENTER = "DIRECTION_FARTHEST_FROM_CENTER";
  transient final static public String DIRECTION_CLOSEST_TO_CENTER = "DIRECTION_CLOSEST_TO_CENTER";
  transient final static public String DIRECTION_FARTHEST_LEFT = "DIRECTION_FARTHEST_LEFT";
  transient final static public String DIRECTION_FARTHEST_RIGHT = "DIRECTION_FARTHEST_RIGHT";
  transient final static public String DIRECTION_FARTHEST_TOP = "DIRECTION_FARTHEST_TOP";
  transient final static public String DIRECTION_FARTHEST_BOTTOM = "DIRECTION_FARTHEST_BOTTOM";

  transient final static public String FOREGROUND = "foreground";
  transient final static public String BACKGROUND = "background";
  transient final static public String PART = "part";

  transient public final static String SOURCE_KINECT_DEPTH = "SOURCE_KINECT_DEPTH";

  static String POSSIBLE_FILTERS[] = { "AdaptiveThreshold", "AddAlpha", "AddMask", "Affine", "And", "AverageColor", "Canny", "ColorTrack", "Copy", "CreateHistogram", "Detector",
      "Dilate", "DL4J", "DL4JTransfer", "Erode", "FaceDetect", "FaceRecognizer", "Fauvist", "Ffmpeg", "FindContours", "Flip", "FloodFill", "FloorFinder", "GoodFeaturesToTrack",
      "Gray", "HoughLines2", "Hsv", "Input", "InRange", "KinectDepth", "KinectDepthMask", "KinectInterleave", "LKOpticalTrack", "Mask", "MatchTemplate", "MotionTemplate", "Mouse",
      "Not", "Output", "PyramidDown", "PyramidUp", "RepetitiveAnd", "RepetitiveOr", "ResetImageRoi", "Resize", "SampleArray", "SampleImage", "SetImageROI", "SimpleBlobDetector",
      "Smooth", "Solr", "Split", "State", "Surf", "Tesseract", "Threshold", "Tracker", "Transpose", "Undistort", "Yolo" };

  // mask for each named filter
  transient public HashMap<String, IplImage> masks = new HashMap<String, IplImage>();

  boolean undockDisplay = false;

  // track the state of opencv. capturing true/false?
  // FIXME - there should be a bool isCapturing() - part of VideoCapture
  // interface !
  // additionally this should not be but package scope protected (ie no
  // declaration)
  volatile boolean capturing = false;

  // TODO: a peer, but in the future , we should use WebGui and it's http
  // container for this if possible.
  // GROG : .. perhaps just a filter in the pipeline could stream it via http
  transient VideoStreamer streamer;

  // Changed default to false. Otherwise multiple opencv instances will get a
  // port in use bind exception.
  // TODO: fix how the opencv service can stream video to the webgui.
  boolean streamerEnabled = false;
  String inputSource = OpenCV.INPUT_SOURCE_CAMERA;
  Integer cameraIndex = 0;
  String inputFile = null;
  String pipelineSelected = null;
  String grabberType = null;
  String format = null;

  // from vp begin
  transient FrameGrabber grabber = null;
  transient Thread videoThread = null;
  transient Map<String, OpenCVFilter> filters = new LinkedHashMap<String, OpenCVFilter>();
  transient List<OpenCVFilter> addFilterQueue = new ArrayList<OpenCVFilter>();
  transient List<String> removeFilterQueue = new ArrayList<String>();
  transient SimpleDateFormat sdf = new SimpleDateFormat();
  transient HashMap<String, FrameRecorder> outputFileStreams = new HashMap<String, FrameRecorder>();
  public static final String INPUT_KEY = "input";
  boolean publishDisplay = true;
  transient BlockingQueue<Object> blockingData = new LinkedBlockingQueue<Object>();

  /**
   * selected display filter unselected defaults to input
   */
  String displayFilterName = INPUT_KEY;
  transient Frame frame;
  boolean recordOutput = false;
  boolean closeOutputs = false;
  String recordingSource = INPUT_KEY;

  static final Set<String> grabberTypes = new TreeSet<String>();

  static {
    try {

      for (int i = 0; i < FrameGrabber.list.size(); ++i) {
        String ss = FrameGrabber.list.get(i);
        String fg = ss.substring(ss.lastIndexOf(".") + 1);
        grabberTypes.add(fg);
      }

      // Add the MRL Frame Grabbers
      // grabberTypes.add("IPCamera"); - given to bytedeco
      grabberTypes.add("Pipeline"); // to/from another opencv service
      // grabberTypes.add("ImageFile"); - FFmpeg support more types
      // grabberTypes.add("SlideShowFile"); - if file is dir slideshow will be
      // used
      grabberTypes.add("Sarxos");
      grabberTypes.add("MJpeg");
    } catch (Exception e) {
      log.error("initializing frame grabber types threw", e);
    }
  }

  int frameIndex = 0;
  final transient OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
  HashMap<String, Overlay> overlays = new HashMap<String, Overlay>();
  transient CvFont font = new CvFont();
  StringBuffer frameTitle = new StringBuffer();
  OpenCVData data;

  // from vp end

  // frame grabber properties
  Integer height = null;
  Integer width = null;

  long frameStartTs;

  transient Frame lastFrame;
  transient IplImage lastImage;
  String lastFilterName;

  public OpenCV(String n) {
    super(n);
    putText("time:  %d", 20, 20, "black");
    putText("frame: %d", 20, 20, "black");
  }

  @Override
  public void stopService() {
    super.stopService();
    stopCapture();
  }

  public final boolean publishDisplay(Boolean b) {
    publishDisplay = b;
    return b;
  }

  /**
   * new way of converting IplImages to BufferedImages
   */
  public static BufferedImage IplImageToBufferedImage(IplImage src) {
    OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
    Java2DFrameConverter converter = new Java2DFrameConverter();
    Frame frame = grabberConverter.convert(src);
    return converter.getBufferedImage(frame, 1);
  }

  /**
   * convert BufferedImages to IplImages
   */
  public static IplImage BufferedImageToIplImage(BufferedImage src) {
    OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
    Java2DFrameConverter jconverter = new Java2DFrameConverter();
    return grabberConverter.convert(jconverter.convert(src));
  }

  /**
   * convert BufferedImages to IplImages
   */
  public static Frame BufferedImageToFrame(BufferedImage src) {
    Java2DFrameConverter jconverter = new Java2DFrameConverter();
    return jconverter.convert(src);
  }

  /**
   * FIXME - input needs to be OpenCVData THIS IS NOT USED ! VideoProcessor NOW
   * DOES OpenCVData - this will return NULL REMOVE !!
   */
  public final SerializableImage publishDisplay(SerializableImage img) {
    return img;
  }

  /**
   * the publishing point of all OpenCV goodies ! type conversion is held off
   * until asked for - then its cached SMART ! :)
   * 
   */
  public final OpenCVData publishOpenCVData(OpenCVData data) {
    return data;
  }

  public Integer setCameraIndex(Integer index) {
    this.cameraIndex = index;
    return index;
  }

  public String setInputFileName(String inputFile) {
    this.inputFile = inputFile;
    return inputFile;
  }

  // FIXME either file or camera
  // camera or file => capture(int) or capture(String)
  public String setInputSource(String inputSource) {
    this.inputSource = inputSource;
    return inputSource;
  }

  public String setFrameGrabberType(String grabberType) {
    this.grabberType = grabberType;
    return grabberType;
  }

  public void setDisplayFilter(String name) {
    displayFilterName = name;
  }

  /**
   * when the video image changes size this function will be called with the new
   * dimension
   */
  public Dimension sizeChange(Dimension d) {
    return d;
  }

  public String publish(String value) {
    return value;
  }

  // CPP interface does not use array - but hides implementation
  public CvPoint2D32f publish(CvPoint2D32f features) {
    return features;
  }

  public double[] publish(double[] data) {
    return data;
  }

  public CvPoint publish(CvPoint point) {
    return point;
  }

  public Point2Df publish(Point2Df point) {
    return point;
  }

  public Rectangle publish(Rectangle rectangle) {
    return rectangle;
  }

  // when containers are published the <T>ypes are unknown to the publishing
  // function
  public ArrayList<?> publish(ArrayList<?> polygons) {
    return polygons;
  }

  public ColoredPoint[] publish(ColoredPoint[] points) {
    return points;
  }

  public SerializableImage publishTemplate(String source, BufferedImage img, int frameIndex) {
    SerializableImage si = new SerializableImage(img, source, frameIndex);
    return si;
  }

  public IplImage publishIplImageTemplate(IplImage img) {
    return img;
  }

  synchronized public void stopCapture() {
    info("stopping capture");
    try {

      capturing = false;
      videoThread = null;

      if (grabber != null) {
        // calls stop & release
        grabber.close();
      }

    } catch (Exception e) {
      log.error("stopCapture threw", e);
    }

    broadcastState();
  }

  public void capture(FrameGrabber grabber) {
    stopCapture();
    this.grabber = grabber;
    capture();
  }

  synchronized public void capture() {
    if (videoThread != null) {
      info("already capturing");
    } else {
      videoThread = new Thread(new VideoProcessor2(), String.format("%s-video-processor", getName()));
      videoThread.start();
    }
  }

  public FrameGrabber getFrameGrabber()
      throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, org.bytedeco.javacv.FrameGrabber.Exception {

    if (grabber != null) {
      return grabber;
    }

    String newGrabberType = null;

    if (inputSource == null) {
      inputSource = INPUT_SOURCE_CAMERA;
    }

    if ((grabberType == null) && (inputSource.equals(INPUT_SOURCE_CAMERA))) {
      grabberType = "OpenCV";
    } else if ((grabberType == null) && (inputSource.equals(INPUT_SOURCE_IMAGE_FILE))) {
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

    log.info(String.format("video source is %s", inputSource));
    Class<?>[] paramTypes = new Class[1];
    Object[] params = new Object[1];
    // TODO - determine by file type - what input it is
    if (OpenCV.INPUT_SOURCE_IMAGE_FILE.equals(inputSource)) {
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

    if (format != null) {
      grabber.setFormat(format);
    }

    // FIXME FIXME FIXME !!! - hasDepthInfo = true - minimally it has
    // two grabs ??? check how it can work
    if (grabber.getClass() == OpenKinectFrameGrabber.class) {
      // OpenKinectFrameGrabber kinect = (OpenKinectFrameGrabber) grabber;
      // data.put(OpenCV.SOURCE_KINECT_DEPTH, kinect.grabDepth());
    }

    log.info(String.format("using %s", grabber.getClass().getCanonicalName()));

    // TODO other framegrabber parameters for frame grabber

    if (height != null) {
      grabber.setImageHeight(height);
    }

    if (width != null) {
      grabber.setImageWidth(width);
    }

    grabber.start();

    return grabber;
  }

  public void stopRecording(String filename) {
    // cvReleaseVideoWriter(outputFileStreams.get(filename).pointerByReference());
  }

  public void setMask(String name, IplImage mask) {
    masks.put(name, mask);
  }

  public OpenCVFilter addFilter(OpenCVFilter filter) {
    filter.setVideoProcessor(this);
    if (!capturing) {
      filters.put(filter.name, filter);
      broadcastState();
    } else {
      addFilterQueue.add(filter);
    }
    return filter;
  }

  /**
   * add filter by type e.g. addFilter("Canny","Canny")
   * 
   * @param filterType
   * @return
   */
  public OpenCVFilter addFilter(String filterType) {
    return addFilter(filterType, filterType);
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
   * remove all the filters in the pipeline
   */
  public void removeFilters() {
    if (!capturing) {
      filters.clear();
      broadcastState();
    } else {
      removeFilterQueue.addAll(filters.keySet());
    }
  }

  /**
   * remove a filter by name
   * 
   * @param name
   */
  public void removeFilter(String name) {
    if (!capturing) {
      filters.remove(name);
    } else {
      removeFilterQueue.add(name);
    }
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
      Service.copyShallowFrom(filter, otherFilter.filter);
    } else {
      error("setFilterState - could not find %s ", otherFilter.name);
    }

  }

  /*
   * Callback from the SwingGui to the appropriate filter funnel through here
   */
  public void invokeFilterMethod(String filterName, String method, Object... params) {
    OpenCVFilter filter = getFilter(filterName);
    if (filter != null) {
      Reflector.invokeMethod(filter, method, params);
    } else {
      log.error("invokeFilterMethod " + filterName + " does not exist");
    }
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

  public void recordOutput(Boolean b) {
    recordOutput = b;
  }

  // FIXME - does this work ???
  public String recordSingleFrame() {
    OpenCVData d = getOpenCVData();
    return d.writeDisplay();
  }

  public OpenCVData getOpenCVData() {
    return getOpenCVData(500);
  }

  // FIXME - use ?
  // FIXME - don't try catch - expose the Exceptions - performance enhancement
  public OpenCVData getOpenCVData(Integer timeout) {
    return data;
  }

  // FIXME - TODO track(type)

  public OpenCVData getGoodFeatures() {
    addFilter(FILTER_GOOD_FEATURES_TO_TRACK, FILTER_GOOD_FEATURES_TO_TRACK);
    OpenCVData d = getOpenCVData();
    removeFilter(FILTER_GOOD_FEATURES_TO_TRACK);
    return d;
  }

  public OpenCVData getFaceDetect() {
    OpenCVFilterFaceDetect fd = new OpenCVFilterFaceDetect();
    addFilter(fd);
    OpenCVData d = getOpenCVData();
    removeFilter(fd.name);
    return d;
  }

  public SerializableImage getDisplay() {
    OpenCVData d = getOpenCVData();
    SerializableImage ret = new SerializableImage(d.getBufferedImage(), d.getSelectedFilterName());
    return ret;
  }

  public int getCameraIndex() {
    return this.cameraIndex;
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

  public void captureFromResourceFile(String filename) throws IOException {
    FileIO.extractResource(filename, filename);
    capture(filename);
  }

  public boolean undockDisplay(boolean b) {
    undockDisplay = b;
    broadcastState();
    return b;
  }

  static public String[] getPossibleFilters() {
    return POSSIBLE_FILTERS;
  }

  public String publishRecognizedFace(String value) {
    return value;
  }

  // FIXME - I could see how this would be useful
  public void publishNoRecognizedFace() {

  }

  public Map<String, Double> publishClassification(Map<String, Double> classifications) {
    // log.info("Publish Classification in opencv!");
    return classifications;
  }

  public ArrayList<YoloDetectedObject> publishYoloClassification(ArrayList<YoloDetectedObject> classifications) {
    return classifications;
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

    String javaCvVersion = "1.4.2";
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

    // yolo models
    meta.addDependency("yolo", "yolov2", "v2", "zip");

    return meta;
  }

  public boolean isStreamerEnabled() {
    return streamerEnabled;
  }

  public void setStreamerEnabled(boolean streamerEnabled) {
    this.streamerEnabled = streamerEnabled;
  }

  public boolean isCapturing() {
    return capturing;
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
    Runtime.start("gui", "SwingGui");
    LoggingFactory.init("WARN");

    // System.loadLibrary("opencv_java");
    OpenCV opencv = (OpenCV) Runtime.start("opencv", "OpenCV");

//    OpenCVFilterYolo yolo = new OpenCVFilterYolo("yolo");
//     opencv.addFilter(yolo);

    opencv.capture();

  }

  public void saveFrame(String string) {
    OpenCVData data = getOpenCVData();
    data.getInputBufferedImage();
  }

  public void capture(int cameraIndex) {
    // vget - youtube
  }

  public void capture(String filename) {
    stopCapture();
    // setFrameGrabberType("org.myrobotlab.opencv.ImageFileFrameGrabber");
    setInputSource(INPUT_SOURCE_IMAGE_FILE);
    setInputFileName(filename);
    capture();
    // vget - youtube

  }

  class VideoProcessor2 implements Runnable {

    @Override
    public void run() {
      try {

        getFrameGrabber();

        lastFilterName = INPUT_KEY;

        log.info("beginning capture");
        capturing = true;

        while (capturing) {
          processVideo();
        }

      } catch (Exception e) {
        log.error("stopping capture", e);
        stopCapture();
      }
    }
  }

  private void processVideo() throws org.bytedeco.javacv.FrameGrabber.Exception, InterruptedException {
    // process filter changes
    if (addFilterQueue.size() > 0) {
      for (OpenCVFilter filter : addFilterQueue) {
        filters.put(filter.name, filter);
      }
      addFilterQueue.clear();
      broadcastState();
    }

    if (removeFilterQueue.size() > 0) {
      for (String name : removeFilterQueue) {
        filters.remove(name);
      }
      removeFilterQueue.clear();
      broadcastState();
    }

    frameStartTs = System.currentTimeMillis();

    frame = grabber.grab();

    if (frame != null) {
      lastFrame = frame;
    } else if (frame == null && lastFrame != null) {
      frame = lastFrame;
    } else {
      error("could not get valid frame");
      stopCapture();
    }

    ++frameIndex;

    data = new OpenCVData(getName(), frameIndex);

    IplImage img = converter.convert(frame);
    if (img != null) {
      lastImage = img;
    } else if (img == null && lastImage != null) {
      img = lastImage;
    } else {
      error("could not get valid image");
      stopCapture();
    }

    lastFilterName = INPUT_KEY;
    data.put(INPUT_KEY, img);

    // process each filter
    for (String filterName : filters.keySet()) {
      OpenCVFilter filter = filters.get(filterName);

      data.setFilter(filter);

      IplImage image = data.get(filter.sourceKey);
      if (image == null) {
        image = data.get(lastFilterName);
      }

      // pre process handles image size & channel changes
      filter.preProcess(frameIndex, image, data);

      image = filter.process(image, data);

      // process the image - push into source as new output
      // other pipelines will pull it off the from the sources
      data.put(filter.name, image);
      lastFilterName = filter.name;
    } // for each filter

    data.setDisplayFilterName(displayFilterName);
    IplImage displayImg = data.getDisplay();

    putText("frame: %d", frameIndex);
    putText("time:  %d", frameStartTs);

    for (Overlay overlay : overlays.values()) {
      // cvPutText(displayImg, "WTF!", cvPoint(106, 106), font, CvScalar.YELLOW);
      // cvPutText(displayImg, overlay.text, overlay.pos, overlay.font, overlay.color);
    }

    
    // FIXME - to have "display" or to only use OpenCVData - that is the question
    // FIXME - to add overlays in Java land or OpeCV land ?
    if (publishDisplay) {      
      // SerializableImage display = new SerializableImage(data.getBufferedImage(displayFilterName), data.getDisplayFilterName(), frameIndex);
      BufferedImage display = IplImageToBufferedImage(displayImg);
      Graphics2D g2d = display.createGraphics();
      g2d.setPaint(Color.red);
      g2d.setFont(new Font("Serif", Font.BOLD, 20));
      g2d.drawString("HELLO", 20, 20);
      g2d.dispose();
      SerializableImage serializableImg = new SerializableImage(display, data.getDisplayFilterName(), frameIndex);
      invoke("publishDisplay", serializableImg);
    }

    // FIXME - should have had it
    invoke("publishOpenCVData", data);

    // this has to be before record as
    // record uses the queue - this has the "issue" if
    // the consumer does not pickup-it will get stale
    if (blockingData.size() == 0) {
      blockingData.add(data);
    }

    if (recordOutput) {
      // TODO - add input, filter, & display
      record(data);
    }

  }

  public void removeOverlays() {
    overlays = new HashMap<String, Overlay>();
  }

  // putText("time", 20, 20, "time: %d", frameIndex);

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

  /**
   * creates a new overlay of text
   * 
   * @param format
   * @param x
   * @param y
   * @param color
   */
  public void putText(String format, int x, int y, String color) {
    Overlay overlay = new Overlay(x, y, color, format);
    overlays.put(format, overlay);
  }

  public Frame getFrame() {
    return lastFrame;
  }

  public IplImage getImage() {
    return lastImage;
  }

  /**
   * thread safe recording of avi
   * 
   * key- input, filter, or display
   */
  public void record(OpenCVData data) {
    try {

      if (!outputFileStreams.containsKey(recordingSource)) {
        // FFmpegFrameRecorder recorder = new FFmpegFrameRecorder
        // (String.format("%s.avi",filename), frame.width(),
        // frame.height());
        FrameRecorder recorder = new OpenCVFrameRecorder(String.format("%s.avi", recordingSource), frame.imageWidth, frame.imageHeight);
        // recorder.setCodecID(CV_FOURCC('M','J','P','G'));
        // TODO - set frame rate to framerate
        recorder.setFrameRate(15);
        recorder.setPixelFormat(1);
        recorder.start();
        outputFileStreams.put(recordingSource, recorder);
      }
      // TODO - add input, filter & display
      outputFileStreams.get(recordingSource).record(converter.convert(data.getImage(recordingSource)));

      if (closeOutputs) {
        OpenCVFrameRecorder output = (OpenCVFrameRecorder) outputFileStreams.get(recordingSource);
        outputFileStreams.remove(output);
        output.stop();
        output.release();
        recordOutput = false;
        closeOutputs = false;
      }

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public List<OpenCVFilter> getFilters() {
    List<OpenCVFilter> ret = new ArrayList<>();
    ret.addAll(filters.values());
    return ret;
  }

  public String getGrabberType() {
    return grabberType;
  }

  public String getInputFile() {
    return inputFile;
  }

  public String getInputSource() {
    return inputSource;
  }

  public boolean isUndocked() {
    return undockDisplay;
  }

  public int getFrameIndex() {
    return frameIndex;
  }

  static public Set<String> getGrabberTypes() {
    return grabberTypes;
  }

}