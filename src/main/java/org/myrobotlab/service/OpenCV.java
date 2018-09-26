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

/*
 TODO : 
 new filters - http://idouglasamoore-javacv.googlecode.com/git-history/02385ce192fb82f1668386e55ff71ed8d6f88ae3/src/main/java/com/googlecode/javacv/ObjectFinder.java

 static wild card imports for quickly finding static functions in eclipse
 */
//import static org.bytedeco.javacpp.opencv_calib3d.*;
//import static org.bytedeco.javacpp.opencv_contrib.*;
//import static org.bytedeco.javacpp.opencv_core.*;
import java.awt.Dimension;
import java.awt.Rectangle;

//import static org.bytedeco.javacpp.opencv_gpu.*;
//import static org.bytedeco.javacpp.opencv_superres.*;
//import static org.bytedeco.javacpp.opencv_ts.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvPoint2D32f;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.image.ColoredPoint;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.BlockingQueueGrabber;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.MJpegFrameGrabber;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.opencv.OpenCVFilterDL4J;
import org.myrobotlab.opencv.OpenCVFilterFaceDetect;
import org.myrobotlab.opencv.OpenCVFilterTracker;
import org.myrobotlab.opencv.OpenCVFilterUndistort;
import org.myrobotlab.opencv.VideoProcessor;
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

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCV.class);

  // FIXME - make more simple
  transient public final static String INPUT_SOURCE_CAMERA = "camera";
  transient public final static String INPUT_SOURCE_MOVIE_FILE = "file";
  transient public final static String INPUT_SOURCE_NETWORK = "network";
  transient public final static String INPUT_SOURCE_PIPELINE = "pipeline";
  transient public final static String INPUT_SOURCE_IMAGE_FILE = "imagefile";
  transient public final static String INPUT_SOURCE_IMAGE_DIRECTORY = "slideshow";

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
      "Dilate", "DL4J", "DL4JTransfer", "Erode", "FaceDetect", "FaceRecognizer", "Fauvist", "Ffmpeg", "FindContours", "Flip", "FloodFill", "FloorFinder", "GoodFeaturesToTrack", "Gray",
      "HoughLines2", "Hsv", "Input", "InRange", "KinectDepth", "KinectDepthMask", "KinectInterleave", "LKOpticalTrack", "Mask", "MatchTemplate", "MotionTemplate", "Mouse", "Not",
      "Output", "PyramidDown", "PyramidUp", "RepetitiveAnd", "RepetitiveOr", "ResetImageRoi", "Resize", "SampleArray", "SampleImage", "SetImageROI", "SimpleBlobDetector", "Smooth",
      "Solr", "Split", "State", "Surf", "Tesseract", "Threshold", "Tracker", "Transpose", "Undistort", "Yolo" };

  // yep its public - cause a whole lotta data
  // will get set on it before a setState

  transient public VideoProcessor videoProcessor;

  // mask for each named filter
  transient public HashMap<String, IplImage> masks = new HashMap<String, IplImage>();

  public boolean undockDisplay = false;

  // track the state of opencv. capturing true/false?
  // FIXME - there should be a bool isCapturing() - part of VideoCapture
  // interface !
  // additionally this should not be public but package scope protected (ie no
  // declaration)
  public volatile boolean capturing = false;

  // TODO: a peer, but in the future , we should use WebGui and it's http
  // container for this if possible.
  // GROG : .. perhaps just a filter in the pipeline could stream it via http
  transient public VideoStreamer streamer;

  // Changed default to false. Otherwise multiple opencv instances will get a
  // port in use bind exception.
  // TODO: fix how the opencv service can stream video to the webgui.
  public boolean streamerEnabled = false;
  public String inputSource = OpenCV.INPUT_SOURCE_CAMERA;
  public Integer cameraIndex = 0;
  public String inputFile = "http://localhost/videostream.cgi";
  public String pipelineSelected = "";
  public String grabberType = getDefaultFrameGrabberType();
  public String format = null;

  // use these to specify the resolution for the frame grabber
  public Integer height = null;
  public Integer width = null;

  public OpenCV(String n) {
    super(n);
    // initialize the video processor with a handle to this opencv service
    videoProcessor = new VideoProcessor(this);
  }

  @Override
  public void stopService() {
    if (videoProcessor != null) {
      videoProcessor.stop();
      capturing = false;
    }
    super.stopService();
  }

  public final boolean publishDisplay(Boolean b) {
    videoProcessor.publishDisplay = b;
    return b;
  }

  /*
   * new way of converting IplImages to BufferedImages
   */
  public static BufferedImage IplImageToBufferedImage(IplImage src) {
    OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
    Java2DFrameConverter converter = new Java2DFrameConverter();
    Frame frame = grabberConverter.convert(src);
    return converter.getBufferedImage(frame, 1);
  }

  /*
   * new way of converting BufferedImages to IplImages
   */
  public static IplImage BufferedImageToIplImage(BufferedImage src) {
    OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
    Java2DFrameConverter jconverter = new Java2DFrameConverter();
    return grabberConverter.convert(jconverter.convert(src));
  }

  /*
   * new way of converting BufferedImages to IplImages
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
    // lastDisplay = new SerializableImage(img, source);
    // return lastDisplay;
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

  // the big switch <input>
  public void publishOpenCVData(boolean b) {
    videoProcessor.publishOpenCVData = b;
  }

  public Integer setCameraIndex(Integer index) {
    this.cameraIndex = index;
    return index;
  }

  public String setInputFileName(String inputFile) {
    this.inputFile = inputFile;
    return inputFile;
  }

  public String setInputSource(String inputSource) {
    this.inputSource = inputSource;
    return inputSource;
  }

  public String setFrameGrabberType(String grabberType) {
    if (grabberType != null && !grabberType.contains(".")) {
      
      String prefixPath;
      if ("IPCamera".equals(grabberType) || "Pipeline".equals(grabberType) || "ImageFile".equals(grabberType)
          || "SlideShow".equals(grabberType) || "Sarxos".equals(grabberType) || "MJpeg".equals(grabberType)) {
        prefixPath = "org.myrobotlab.opencv.";
      } else {
        prefixPath = "org.bytedeco.javacv.";
      }
      
      grabberType = prefixPath + grabberType;
      if (!grabberType.endsWith("FrameGrabber")) {
        grabberType += "FrameGrabber";
      }
    }
    this.grabberType = grabberType;
    return grabberType;
  }

  public void setDisplayFilter(String name) {
    log.info("pre setDisplayFilter displayFilter{}", videoProcessor.displayFilterName);
    videoProcessor.displayFilterName = name;
    log.info("post setDisplayFilter displayFilter{}", videoProcessor.displayFilterName);
  }

  public OpenCVData add(SerializableImage image) {
    Frame src = BufferedImageToFrame(image.getImage());
    return add(src);
  }

  /**
   * blocking safe exchange of data between different threads external thread
   * adds image data which can be retrieved from the blockingData queue
   * 
   */
  public OpenCVData add(Frame image) {
    FrameGrabber grabber = videoProcessor.getGrabber();
    if (grabber == null || grabber.getClass() != BlockingQueueGrabber.class) {
      error("can't add an image to the video processor - grabber must be not null and BlockingQueueGrabber");
      return null;
    }

    BlockingQueueGrabber bqgrabber = (BlockingQueueGrabber) grabber;
    bqgrabber.add(image);

    try {
      OpenCVData ret = (OpenCVData) videoProcessor.blockingData.take();
      return ret;
    } catch (InterruptedException e) {
      return null;
    }
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

  // publish functions end ---------------------------

  public void stopCapture() {
    log.info("opencv - stop capture");
    // videoProcessor.stop();

    log.debug("stopping capture");
    capturing = false;
    videoProcessor.videoThread = null;

    broadcastState(); // let everyone know
    // TODO: do we need this?
    sleep(500);
    capturing = false;
  }

  public void capture(FrameGrabber grabber) {
    try {
      save();
      if (streamerEnabled) {
        streamer = (VideoStreamer) startPeer("streamer");
        streamer.attach(this);
      }
      // TODO: The elusive API preference for the VideoCapture api! where you
      // can specify plugins!
      videoProcessor.start(grabber);
      // there's a nasty race condition,
      // so we sleep here for 500 milliseconds to make sure
      // the video stream is up and running before we publish our state.
      // sleep(500);
      broadcastState(); // let everyone know
    } catch (Exception e) {
      error(e);
    }
  }

  public void capture() {
    FrameGrabber grabber = null;
    try {
      grabber = createFrameGrabber(inputSource, cameraIndex, inputFile, pipelineSelected, grabberType, format);

      // set the height / width
      if (height != null)
        grabber.setImageHeight(height);
      if (width != null)
        grabber.setImageWidth(width);
    } catch (Exception e) {
      error(e);
      this.capturing = false;
      return;
    }
    capture(grabber);
  }

  public static String getDefaultFrameGrabberType() {
    Platform platform = Runtime.getInstance().getPlatform();
    if (platform.isWindows()) {
      return "org.bytedeco.javacv.VideoInputFrameGrabber";
    } else {
      return "org.bytedeco.javacv.OpenCVFrameGrabber";
    }
  }

  private FrameGrabber createFrameGrabber(String inputSource, Integer cameraIndex, String inputFile, String pipelineSelected, String grabberType, String format)
      throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
    // inputSource = INPUT_SOURCE_IMAGE_FILE;
    log.info(String.format("video source is %s", inputSource));
    Class<?>[] paramTypes = new Class[1];
    Object[] params = new Object[1];
    // TODO - determine by file type - what input it is
    if (OpenCV.INPUT_SOURCE_CAMERA.equals(inputSource)) {
      paramTypes[0] = Integer.TYPE;
      params[0] = cameraIndex;
    } else if (OpenCV.INPUT_SOURCE_MOVIE_FILE.equals(inputSource)) {
      paramTypes[0] = String.class;
      params[0] = inputFile;
    } else if (OpenCV.INPUT_SOURCE_IMAGE_FILE.equals(inputSource)) {
      paramTypes[0] = String.class;
      params[0] = inputFile;
    } else if (OpenCV.INPUT_SOURCE_IMAGE_DIRECTORY.equals(inputSource)) {
      paramTypes[0] = String.class;
      params[0] = inputFile;
    } else if (OpenCV.INPUT_SOURCE_PIPELINE.equals(inputSource)) {
      paramTypes[0] = String.class;
      params[0] = pipelineSelected;
    } else if (OpenCV.INPUT_SOURCE_NETWORK.equals(inputSource)) {
      paramTypes[0] = String.class;
      params[0] = inputFile;
    }

    log.info(String.format("attempting to get frame grabber %s format %s", grabberType, format));
    Class<?> nfg = Class.forName(grabberType);
    // TODO - get correct constructor for Capture Configuration..
    Constructor<?> c = nfg.getConstructor(paramTypes);

    FrameGrabber grabber = (FrameGrabber) c.newInstance(params);

    if (format != null) {
      grabber.setFormat(format);
    }

    log.info(String.format("using %s", grabber.getClass().getCanonicalName()));

    if (grabber == null) {
      log.error("no viable capture or frame grabber with input {}", grabberType);
      videoProcessor.stop();
      capturing = false;
    }

    return grabber;
  }

  public void stopRecording(String filename) {
    // cvReleaseVideoWriter(outputFileStreams.get(filename).pointerByReference());
  }

  public void setMask(String name, IplImage mask) {
    masks.put(name, mask);
  }

  public OpenCVFilter addFilter(OpenCVFilter filter) {
    videoProcessor.addFilter(filter);
    // broadcastState(); not needed as videoProcessor will send
    // the broadcast state
    return filter;
  }

  public OpenCVFilter addFilter(String filterName) {

    OpenCVFilter filter = videoProcessor.addFilter(filterName, filterName);
    // broadcastState(); // let everyone know - not needed as videoProcessor
    // will
    // filter.setVideoProcessor(videoProcessor);
    // send the broadcast state
    return filter;
  }

  public OpenCVFilter addFilter(String name, String filterType) {

    OpenCVFilter filter = videoProcessor.addFilter(name, filterType);
    // we need to pass a handle to our video processor down so filters can
    // invoke things on opencv i guess.
    // TODO: KW: maybe we want to change it so that filters invoke on
    // themselves.. (but, filters aren't services, so that's a bit odd.)
    // filter.setVideoProcessor(videoProcessor);
    // broadcastState(); // let everyone know
    // not needed as broadcast
    return filter;
  }

  // FIXME - rename removeFilters
  public void removeFilters() {
    videoProcessor.removeFilters();
    // broadcastState(); not needed
  }

  public void removeFilter(String name) {
    videoProcessor.removeFilter(name);
    // broadcastState(); not needed
  }

  public List<OpenCVFilter> getFiltersCopy() {
    return videoProcessor.getFiltersCopy();
  }

  public OpenCVFilter getFilter(String name) {
    return videoProcessor.getFilter(name);
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
    videoProcessor.recordOutput(b);
  }

  public String recordSingleFrame() {
    // WOOHOO Changed threads & thread safe !
    // OpenCVData d = videoProcessor.getLastData();
    OpenCVData d = getOpenCVData();
    /*
     * if (d == null) { log.error(
     * "could not record frame last OpenCVData is null"); return null; }
     */
    return d.writeDisplay();
    // return d.writeInput();
  }

  // filter dynamic data exchange end ------------------
  public static Rectangle cvToAWT(CvRect rect) {
    Rectangle boundingBox = new Rectangle();
    boundingBox.x = rect.x();
    boundingBox.y = rect.y();
    boundingBox.width = rect.width();
    boundingBox.height = rect.height();
    return boundingBox;

  }

  public OpenCVData getOpenCVData() {
    return getOpenCVData(500);
  }

  // FIXME - don't try catch - expose the Exceptions - performance enhancement
  public OpenCVData getOpenCVData(Integer timeout) {
    OpenCVData data = null;
    try {

      // making fresh when blocking with a queue
      videoProcessor.blockingData.clear();

      // DEPRECATE always "publish"
      boolean oldPublishOpenCVData = videoProcessor.publishOpenCVData;
      videoProcessor.publishOpenCVData = true;
      // videoProcessor.useBlockingData = true;
      // timeout ? - change to polling

      if (timeout == null || timeout < 1) {
        data = (OpenCVData) videoProcessor.blockingData.take();
      } else {
        data = (OpenCVData) videoProcessor.blockingData.poll(timeout, TimeUnit.MILLISECONDS);
      }
      // value parameter
      videoProcessor.publishOpenCVData = oldPublishOpenCVData;
      // videoProcessor.useBlockingData = false;
      return data;

    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return null;
  }

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

  public static Point2Df findPoint(ArrayList<Point2Df> data, String direction, Double minValue) {

    double distance = 0;
    int index = 0;
    double targetDistance = 0.0f;

    if (data == null || data.size() == 0) {
      log.error("no data");
      return null;
    }

    if (minValue == null) {
      minValue = 0.0;
    }

    if (DIRECTION_CLOSEST_TO_CENTER.equals(direction)) {
      targetDistance = 1;
    } else {
      targetDistance = 0;
    }

    for (int i = 0; i < data.size(); ++i) {
      Point2Df point = data.get(i);

      if (DIRECTION_FARTHEST_FROM_CENTER.equals(direction)) {
        distance = (float) Math.sqrt(Math.pow((0.5 - point.x), 2) + Math.pow((0.5 - point.y), 2));
        if (distance > targetDistance && point.value >= minValue) {
          targetDistance = distance;
          index = i;
        }
      } else if (DIRECTION_CLOSEST_TO_CENTER.equals(direction)) {
        distance = (float) Math.sqrt(Math.pow((0.5 - point.x), 2) + Math.pow((0.5 - point.y), 2));
        if (distance < targetDistance && point.value >= minValue) {
          targetDistance = distance;
          index = i;
        }
      } else if (DIRECTION_FARTHEST_LEFT.equals(direction)) {
        distance = point.x;
        if (distance < targetDistance && point.value >= minValue) {
          targetDistance = distance;
          index = i;
        }
      } else if (DIRECTION_FARTHEST_RIGHT.equals(direction)) {
        distance = point.x;
        if (distance > targetDistance && point.value >= minValue) {
          targetDistance = distance;
          index = i;
        }
      } else if (DIRECTION_FARTHEST_TOP.equals(direction)) {
        distance = point.y;
        if (distance < targetDistance && point.value >= minValue) {
          targetDistance = distance;
          index = i;
        }
      } else if (DIRECTION_FARTHEST_BOTTOM.equals(direction)) {
        distance = point.y;
        if (distance > targetDistance && point.value >= minValue) {
          targetDistance = distance;
          index = i;
        }
      }

    }

    Point2Df p = data.get(index);
    log.info(String.format("findPointFarthestFromCenter %s", p));
    return p;
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

  /*
   * minimum time between processing frames - time unit is in milliseconds
   * 
   */
  public void setMinDelay(int time) {
    videoProcessor.setMinDelay(time);
  }

  public String setRecordingSource(String source) {
    videoProcessor.recordingSource = source;
    return source;
  }

  public void showFrameNumbers(boolean b) {
    videoProcessor.showFrameNumbers(b);
  }

  public void showTimestamp(boolean b) {
    videoProcessor.showTimestamp(b);
  }

  public void captureFromResourceFile(String filename) throws IOException {
    FileIO.extractResource(filename, filename);
    captureFromImageFile(filename);
  }

  public void captureFromImageFile(String filename) {
    stopCapture();
    setFrameGrabberType("org.myrobotlab.opencv.ImageFileFrameGrabber");
    setInputSource(INPUT_SOURCE_IMAGE_FILE);
    setInputFileName(filename);
    capture();
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
    // meta.exclude("commons-codec", "commons-codec");
    // meta.addDependency("commons-codec", "commons-codec", "1.10");

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

    // OpenCV opencvLeft = (OpenCV) Runtime.start("left", "OpenCV");
    // Runtime.start("right", "OpenCV");
    // opencvLeft.setFrameGrabberType("org.myrobotlab.opencv.SlideShowFrameGrabber");
    // opencvLeft.setInputSource(INPUT_SOURCE_IMAGE_DIRECTORY);
    // training images in this example must be same resolution as camera
    // video
    // stream.
    // OpenCVFilterTranspose tr = new OpenCVFilterTranspose("tr");
    // opencv.addFilter(tr);

    // System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    // System.loadLibrary("opencv_java");
    OpenCV opencv = (OpenCV) Runtime.start("opencv", "OpenCV");
    
    
    
    //String url  = "http://192.168.4.104:8081/?action=stream";
    String url = "http://192.168.4.104:8080/?action=stream";

    //public String leftEyeURL = "http://192.168.4.102:8001/camera/mjpeg";
    //public String rightEyeURL = "http://192.168.4.102:8000/camera/mjpeg";

    //String frameGrabberType = "org.myrobotlab.opencv.MJpegFrameGrabber";
    MJpegFrameGrabber grabber =  new MJpegFrameGrabber(url);
    // grabber.start();
    opencv.capture(grabber);

    
    OpenCVFilterUndistort ud = new OpenCVFilterUndistort("ud");
    opencv.addFilter(ud);
    

//    OpenCVFilterTracker tld = new OpenCVFilterTracker("tld");
//    opencv.addFilter(tld);

//    opencv.height = 480;
//    opencv.width = 640;
//    opencv.height = 1080;
//    opencv.width= 1920;
    
  // opencv.capture();
   // OpenCVFrameGrabber grabber = new OpenCVFrameGrabber("foo",1);
   // opencv.capture(grabber);
    
    
    // Runtime.start("right", "OpenCV");
    // opencv.setFrameGrabberType("org.myrobotlab.opencv.SarxosFrameGrabber");
    // opencv.setFrameGrabberType("org.myrobotlab.opencv.MJpegFrameGrabber");

    // opencv.setInputSource(INPUT_SOURCE_IMAGE_DIRECTORY);
    // opencv.setInputSource(INPUT_SOURCE_CAMERA);
    // opencv.setInputSource(INPUT_SOURCE_NETWORK);
    // opencv.setInputFileName("http://192.168.4.125:8080/?action=stream");
    // opencv.setInputFileName("http://192.168.4.112:8081/?action=stream");

    // OpenCVFilterYolo yolo = new OpenCVFilterYolo("yolo");
    // opencv.addFilter(yolo);

    // OpenCVFilterDL4J dl4j = new OpenCVFilterDL4J("dl4j");
    // opencv.addFilter(dl4j);

    // opencv.setStreamerEnabled(false);
    // opencv.addFilter("facerec", "FaceRecognizer");

    // OpenCVFilterPyramidDown pyramid = new OpenCVFilterPyramidDown("pyramid");
    // opencv.addFilter(pyramid);

    // OpenCVFilterDilate dilate = new OpenCVFilterDilate();
    // opencv.addFilter(dilate);
    // OpenCVFilterTesseract tess = new OpenCVFilterTesseract("tess");

    // OpenCVFilterFaceDetect facedetect2 = new
    // OpenCVFilterFaceDetect("facedetect");
    // opencv.addFilter(facedetect2);

    // OpenCVFilterOverlay filter = new OpenCVFilterOverlay("overlay");
    // filter.addImage("overlay1.png", 0.3);
    // filter.addImage("red.png", 0.4);
    // filter.addImage("overlay_640x480.png", 1.0);

    // filter.addText("scanmode", 20, 40, 0.6, "SCAN MODE NONE");
    // filter.addText("assessment", 20, 50, 0.6, "ASSESSMENT COMPLETED");

    // opencv.addFilter(filter);

    // OpenCVFilterFaceDetect2 facedetect2 = new
    // OpenCVFilterFaceDetect2("facedetect2");
    // opencv.addFilter(facedetect2);
    // OpenCVFilterFaceRecognizer("facerec");

    // String trainingDir = "C:\\training";
    // facerec.setTrainingDir(trainingDir);
    // facerec.train();

    // opencvLeft.addFilter(facerec);

    // VideoStreamer vs = (VideoStreamer)Runtime.start("vs",
    // "VideoStreamer");
    // vs.attach(opencv);
    // opencv.capture();
    // opencvLeft.capture();
    // opencvRight.capture();

    /*
     * OpenCVFilterFFmpeg ffmpeg = new OpenCVFilterFFmpeg("ffmpeg");
     * opencv.addFilter(ffmpeg);
     */

    // opencv.capture();
    // Thread.sleep(5000);
    // opencv.stopCapture();
    // Thread.sleep(5000);
    //
    //
    // opencv.capture();
    // Thread.sleep(5000);
    // opencv.stopCapture();
    // Thread.sleep(5000);

    boolean leave = true;
    if (leave) {
      return;
    }

    // opencv.removeFilters();
    // ffmpeg.stopRecording();
    // opencv.setCameraIndex(0);

    // opencv.setInputSource("file");
    // opencv.setInputFileName("c:/test.avi");
    // opencv.capture();
    // OpenCVFilterSURF surf = new OpenCVFilterSURF("surf");
    // String filename = "c:/dev/workspace.kmw/myrobotlab/lloyd.png";
    // String filename = "c:/dev/workspace.kmw/myrobotlab/kw.jpg";
    // surf.settings.setHessianThreshold(400);
    // surf.loadObjectImageFilename(filename);
    // opencv.addFilter(surf);

    // OpenCVFilterTranspose tr = new OpenCVFilterTranspose("tr");
    // opencv.addFilter(tr);
    /*
     * OpenCVFilterLKOpticalTrack lktrack = new
     * OpenCVFilterLKOpticalTrack("lktrack"); opencv.addFilter(lktrack);
     */

    // OpenCVFilterAffine affine = new OpenCVFilterAffine("left");
    // affine.setAngle(45);
    // opencv.addFilter(affine);
    // opencv.test();

    // opencv.capture();
    // opencv.captureFromImageFile("C:\\mrl\\myrobotlab\\image0.png");

    // Runtime.createAndStart("gui", "SwingGui");
    // opencv.test();
    /*
     * Runtime.createAndStart("gui", "SwingGui"); RemoteAdapter remote =
     * (RemoteAdapter) Runtime.start("ocvremote", "RemoteAdapter");
     * remote.connect("tcp://localhost:6767");
     * 
     * opencv.capture(); boolean leaveNow = true; if (leaveNow) return;
     */

    // final CvMat image1 = cvLoadImageM("C:/blob.jpg" , 0);
    //
    // SimpleBlobDetector o = new SimpleBlobDetector();
    // KeyPoint point = new KeyPoint();
    // o.detect(image1, point, null);
    //
    // System.out.println(point.toString());

  }

  public void saveFrame(String string) {
    OpenCVData data = getOpenCVData();
    data.getInputBufferedImage();
  }

}