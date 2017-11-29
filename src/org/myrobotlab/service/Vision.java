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

//import static org.bytedeco.javacpp.opencv_gpu.*;
//import static org.bytedeco.javacpp.opencv_superres.*;
//import static org.bytedeco.javacpp.opencv_ts.*;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.cvInitFont;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvPoint2D32f;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.image.ColoredPoint;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.reflection.Reflector;
import org.myrobotlab.service.abstracts.AbstractVideoSource;
import org.myrobotlab.service.data.Point2Df;
import org.myrobotlab.service.interfaces.VideoProcessWorker;
import org.myrobotlab.service.interfaces.VideoProcessor;
import org.myrobotlab.vision.BlockingQueueGrabber;
import org.myrobotlab.vision.FilterWrapper;
import org.myrobotlab.vision.VisionData;
import org.myrobotlab.vision.OpenCVFilter;
import org.myrobotlab.vision.OpenCVFilterFaceDetect;
import org.myrobotlab.vision.OpenCVFilterInput;
import org.myrobotlab.vision.Overlay;
import org.slf4j.Logger;

/*

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

*/

/**
 * 
 * Vision - This service provides webcam support and video image processing It
 * uses the JavaCV binding to the Vision library. Vision is a computer vision
 * library. You can create an Vision service and then add a pipeline of
 * OpenCVFilters to it to provide things like facial recognition, and
 * KLOpticalTracking
 * 
 * More Info about Vision : http://opencv2.org/ JavaCV is maintained by Samuel
 * Audet : https://github.com/bytedeco/javacv
 * 
 */
public class Vision extends AbstractVideoSource implements VideoProcessor {

  transient final static public String BACKGROUND = "background";
  transient final static public String DIRECTION_CLOSEST_TO_CENTER = "DIRECTION_CLOSEST_TO_CENTER";

  transient final static public String DIRECTION_FARTHEST_BOTTOM = "DIRECTION_FARTHEST_BOTTOM";
  // directional constants - FIXME - this is in OpenCVUtils too
  transient final static public String DIRECTION_FARTHEST_FROM_CENTER = "DIRECTION_FARTHEST_FROM_CENTER";
  transient final static public String DIRECTION_FARTHEST_LEFT = "DIRECTION_FARTHEST_LEFT";
  transient final static public String DIRECTION_FARTHEST_RIGHT = "DIRECTION_FARTHEST_RIGHT";
  transient final static public String DIRECTION_FARTHEST_TOP = "DIRECTION_FARTHEST_TOP";
  transient public static final String FILTER_DETECTOR = "Detector";

  transient public static final String FILTER_DILATE = "Dilate";
  transient public static final String FILTER_ERODE = "Erode";
  transient public static final String FILTER_FACE_DETECT = "FaceDetect";
  transient public static final String FILTER_FIND_CONTOURS = "FindContours";
  transient public static final String FILTER_GOOD_FEATURES_TO_TRACK = "GoodFeaturesToTrack";
  // TODO - Vision constants / enums ? ... hmm not a big fan ...
  // FIXME - if these are constants they need to be part of the array of
  // possible filters ..
  transient public static final String FILTER_LK_OPTICAL_TRACK = "LKOpticalTrack";

  static final String FILTER_PKG_PREFIX = "org.myrobotlab.opencv2.OpenCVFilter";
  transient public static final String FILTER_PYRAMID_DOWN = "PyramidDown";
  transient final static public String FOREGROUND = "foreground";
  // FIXME - make more simple
  transient public final static String INPUT_SOURCE_CAMERA = "camera";
  transient public final static String INPUT_SOURCE_IMAGE_DIRECTORY = "slideshow";
  transient public final static String INPUT_SOURCE_IMAGE_FILE = "imagefile";
  transient public final static String INPUT_SOURCE_MOVIE_FILE = "file";
  transient public final static String INPUT_SOURCE_NETWORK = "network";
  transient public final static String INPUT_SOURCE_PIPELINE = "pipeline";

  public final static Logger log = LoggerFactory.getLogger(Vision.class);
  transient final static public String PART = "part";
  static String POSSIBLE_FILTERS[] = { "AdaptiveThreshold", "AddAlpha", "AddMask", "Affine", "And", "AverageColor", "Canny", "ColorTrack", "Copy", "CreateHistogram", "Detector",
      "Dilate", "Erode", "FaceDetect", "FaceRecognizer", "Fauvist", "Ffmpeg", "FindContours", "Flip", "FloodFill", "FloorFinder", "GoodFeaturesToTrack", "Gray", "HoughLines2",
      "Hsv", "Input", "InRange", "KinectDepth", "KinectDepthMask", "KinectInterleave", "LKOpticalTrack", "Mask", "MatchTemplate", "MotionTemplate", "Mouse", "Not", "Output",
      FILTER_PYRAMID_DOWN, "PyramidUp", "RepetitiveAnd", "RepetitiveOr", "ResetImageRoi", "Resize", "SampleArray", "SampleImage", "SetImageROI", "SimpleBlobDetector", "Smooth",
      "Split", "State", "Surf", "Threshold", "Transpose" };

  private static final long serialVersionUID = 1L;

  transient public final static String SOURCE_KINECT_DEPTH = "SOURCE_KINECT_DEPTH";

  // FIXME - probably be CREATED AS A LOCAL VAR - to be clean & not used a
  // shared instance
  transient public BlockingQueue<Object> blockingData = new LinkedBlockingQueue<Object>();

  /*
   * new way of converting BufferedImages to IplImages
   */
  public static Frame BufferedImageToFrame(BufferedImage src) {
    Java2DFrameConverter jconverter = new Java2DFrameConverter();
    return jconverter.convert(src);
  }

  /*
   * new way of converting BufferedImages to IplImages
   */
  public static IplImage BufferedImageToIplImage(BufferedImage src) {
    OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
    Java2DFrameConverter jconverter = new Java2DFrameConverter();
    return grabberConverter.convert(jconverter.convert(src));
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

  // remove same method from OpenCVUtils
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

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(Vision.class);
    meta.addDescription("Vision (computer vision) service wrapping many of the functions and filters of Vision");
    meta.addCategory("video", "vision", "sensor");
    // meta.addPeer("streamer", "VideoStreamer", "video streaming service
    // for
    // webgui.");

    meta.sharePeer("streamer", "streamer", "VideoStreamer", "Shared Video Streamer");
    // meta.addDependency("org.bytedeco.javacpp","1.1");
    meta.addDependency("org.bytedeco.javacv", "1.3");
    meta.addDependency("pl.sarxos.webcam", "0.3.10");
    // FIXME - new OpenCV of post manticore release
    meta.setAvailable(false);
    return meta;
  }

  static public String[] getPossibleFilters() {
    return POSSIBLE_FILTERS;
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

  public static void main(String[] args) throws Exception {

    LoggingFactory.init("INFO");

    // TODO - Avoidance / Navigation Service
    // ground plane
    // http://stackoverflow.com/questions/6641055/obstacle-avoidance-with-stereo-vision
    // radio lab - map cells location cells yatta yatta
    // lkoptical disparity motion Time To Contact
    // https://www.google.com/search?aq=0&oq=opencv2+obst&gcx=c&sourceid=chrome&ie=UTF-8&q=opencv2+obstacle+avoidance
    //
    // WebGui webgui = (WebGui)Runtime.start("webgui", "WebGui");
    try {

      Runtime.start("gui", "SwingGui");

      // Vision opencvLeft = (Vision) Runtime.start("left", "Vision");
      // Runtime.start("right", "Vision");
      // opencvLeft.setFrameGrabberType("org.myrobotlab.opencv2.SlideShowFrameGrabber");
      // opencvLeft.setInputSource(INPUT_SOURCE_IMAGE_DIRECTORY);
      // training images in this example must be same resolution as camera
      // video
      // stream.
      // OpenCVFilterTranspose tr = new OpenCVFilterTranspose("tr");
      // opencv2.addFilter(tr);

      Vision opencv2 = (Vision) Runtime.start("opencv2", "Vision");
      // opencv2.setInputFileName("inmoov/system/update_available_1024-600.jpg");

      // opencv2.setFrameGrabberType(grabberType);
      opencv2.capture();
      // Runtime.start("right", "Vision");
      // opencv2.setFrameGrabberType("org.myrobotlab.opencv2.SarxosFrameGrabber");
      // opencv2.setFrameGrabberType("org.myrobotlab.opencv2.MJpegFrameGrabber");

      // opencv2.setInputSource(INPUT_SOURCE_IMAGE_DIRECTORY);
      // opencv2.setInputSource(INPUT_SOURCE_CAMERA);
      // opencv2.setInputSource(INPUT_SOURCE_NETWORK);
      // opencv2.setInputFileName("http://192.168.4.112:8080/?action=stream");
      // opencv2.setInputFileName("http://192.168.4.112:8081/?action=stream");

      // opencv2.addFilter("facerec", "FaceRecognizer");

      // OpenCVFilterPyramidDown pyramid = new
      // OpenCVFilterPyramidDown("pyramid");
      // opencv2.addFilter(pyramid);

      // OpenCVFilterDilate dilate = new OpenCVFilterDilate();
      // opencv2.addFilter(dilate);

      // OpenCVFilterFaceDetect2 facedetect2 = new
      // OpenCVFilterFaceDetect2("facedetect2");
      // opencv2.addFilter(facedetect2);
      // OpenCVFilterFaceRecognizer("facerec");

      // String trainingDir = "C:\\training";
      // facerec.setTrainingDir(trainingDir);
      // facerec.train();

      // opencvLeft.addFilter(facerec);

      // VideoStreamer vs = (VideoStreamer)Runtime.start("vs",
      // "VideoStreamer");
      // vs.attach(opencv2);
      // opencv2.capture();
      // opencvLeft.capture();
      // opencvRight.capture();

      /*
       * OpenCVFilterFFmpeg ffmpeg = new OpenCVFilterFFmpeg("ffmpeg");
       * opencv2.addFilter(ffmpeg);
       */

      // opencv2.capture();
      // Thread.sleep(5000);
      // opencv2.stopCapture();
      // Thread.sleep(5000);
      //
      //
      // opencv2.capture();
      // Thread.sleep(5000);
      // opencv2.stopCapture();
      // Thread.sleep(5000);

      boolean leave = true;
      if (leave) {
        return;
      }

      // opencv2.removeFilters();
      // ffmpeg.stopRecording();
      // opencv2.setCameraIndex(0);

      // opencv2.setInputSource("file");
      // opencv2.setInputFileName("c:/test.avi");
      // opencv2.capture();
      // OpenCVFilterSURF surf = new OpenCVFilterSURF("surf");
      // String filename = "c:/dev/workspace.kmw/myrobotlab/lloyd.png";
      // String filename = "c:/dev/workspace.kmw/myrobotlab/kw.jpg";
      // surf.settings.setHessianThreshold(400);
      // surf.loadObjectImageFilename(filename);
      // opencv2.addFilter(surf);

      // OpenCVFilterTranspose tr = new OpenCVFilterTranspose("tr");
      // opencv2.addFilter(tr);
      /*
       * OpenCVFilterLKOpticalTrack lktrack = new
       * OpenCVFilterLKOpticalTrack("lktrack"); opencv2.addFilter(lktrack);
       */

      // OpenCVFilterAffine affine = new OpenCVFilterAffine("left");
      // affine.setAngle(45);
      // opencv2.addFilter(affine);
      // opencv2.test();

      // opencv2.capture();
      // opencv2.captureFromImageFile("C:\\mrl\\myrobotlab\\image0.png");

      // Runtime.createAndStart("gui", "SwingGui");
      // opencv2.test();
      /*
       * Runtime.createAndStart("gui", "SwingGui"); RemoteAdapter remote =
       * (RemoteAdapter) Runtime.start("ocvremote", "RemoteAdapter");
       * remote.connect("tcp://localhost:6767");
       * 
       * opencv2.capture(); boolean leaveNow = true; if (leaveNow) return;
       */

      // final CvMat image1 = cvLoadImageM("C:/blob.jpg" , 0);
      //
      // SimpleBlobDetector o = new SimpleBlobDetector();
      // KeyPoint point = new KeyPoint();
      // o.detect(image1, point, null);
      //
      // System.out.println(point.toString());
    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

  // track the state of opencv2. capturing true/false?
  // FIXME - there should be a bool isCapturing() - part of VideoCapture
  // interface !
  // additionally this should not be public but package scope protected (ie no
  // declaration)
  boolean capturing = false;

  VisionData data;
  // filters
  // transient Map<String, OpenCVFilter> filters = new LinkedHashMap<String,
  // OpenCVFilter>();
  transient List<OpenCVFilter> filters = new CopyOnWriteArrayList<OpenCVFilter>();
  transient CvFont font = new CvFont();
  private int frameIndex;
  StringBuffer frameTitle = new StringBuffer();

  VisionData lastOpenCVData;

  /**
   * the last source key - used to set the next filter's default source
   */
  String lastSourceKey;
  // mask for each named filter
  transient public HashMap<String, IplImage> masks = new HashMap<String, IplImage>();

  HashMap<String, Overlay> overlays = new HashMap<String, Overlay>();

  boolean publishOpenCVData = true;

  /**
   * selections - these are string values which represent keyed filters they are
   * give "default" values
   * 
   * these are "user" references to determine what filter is in focus for a
   * particular reason
   * 
   * selectedKey = {name}.input selectedInputKey = input selectedDisplayKey =
   * {name}.input selectedRecordingKey = input
   */
  String selectedName = null;
  String selectedInputName = null;

  /*
   * String selectedInputName = null; String selectedDisplayName = null; String
   * selectedRecordingName = null;
   */

  boolean showFrameNumbers = false;

  // display
  boolean showTimestamp = true;

  // FIXME: a peer, but in the future , we should use WebGui and it's http
  // container for this
  // if possible.
  // GroG asks, "perhaps just a filter in the pipeline could stream it via http"
  transient public VideoStreamer streamer;

  public boolean streamerEnabled = true;

  public boolean undockDisplay = false;

  // processing
  final transient VideoProcessWorker worker;
  boolean publishDisplay = true;

  public Vision(String n) {
    super(n);
    cvInitFont(font, CV_FONT_HERSHEY_PLAIN, 1, 1);
    // initialize our worker
    worker = new VideoProcessWorker(this);

    // creating default key for all selections
    // String defaultKey = String.format("%s.input", getName());

    // have to start somewhere...
    String defaultName = "input";

    // create a default "input" filter
    OpenCVFilterInput selectedInputFilter = new OpenCVFilterInput(defaultName);
    selectedInputFilter.lock(true);

    // initializing all selections
    selectedName = defaultName;
    selectedInputName = defaultName;
    /*
     * selectedInputName = defaultName; selectedDisplayName = defaultName;
     * selectedRecordingName = defaultName;
     */

    addFilter(selectedInputFilter);
  }

  /**
   * blocking safe exchange of data between different threads external thread
   * adds image data which can be retrieved from the blockingData queue
   * 
   * @param image
   *          - image to be processed through pipeline
   * @return - OpenCVData
   */
  public VisionData add(Frame image) {
    FrameGrabber grabber = getGrabber();
    if (grabber == null || grabber.getClass() != BlockingQueueGrabber.class) {
      error("can't add an image to the video processor - grabber must be not null and BlockingQueueGrabber");
      return null;
    }

    BlockingQueueGrabber bqgrabber = (BlockingQueueGrabber) grabber;
    bqgrabber.add(image);

    try {
      VisionData ret = (VisionData) blockingData.take();
      return ret;
    } catch (InterruptedException e) {
      return null;
    }
  }

  public VisionData add(SerializableImage image) {
    Frame src = BufferedImageToFrame(image.getImage());
    // IplImage src = IplImage.createFrom(image.getImage());
    // return new SerializableImage(dst.getBufferedImage(),
    // image.getSource());
    return add(src);
  }

  /**
   * adds a filter if the video processing thread is not running it adds it
   * directly to the linked hashmap filter. If the video processing thread is
   * running it must add it to a thread safe queue to be processed by the the
   * other thread. This should make the system thread-safe
   * 
   * @param filter
   *          - filter to add to the pipeline
   * @return - returns filter added
   */
  public OpenCVFilter addFilter(OpenCVFilter filter) {
    filter.sourceKey = lastSourceKey;
    lastSourceKey = makeKey(filter.name);
    filter.setVideoProcessor(this);
    filters.add(filter);
    broadcastState();
    return filter;
  }

  /**
   * make a key for this service and some filter's name
   * 
   * @param filterName
   *          - name of the filter
   * @return - key {serviceName}.{filterName}
   */
  public String makeKey(String filterName) {
    return String.format("%s.%s", getName(), filterName);
  }

  /**
   * add filter by name - in this case the name "must be" the same as its type
   * .. eg. "FaceDetect" for the FaceDetect Filter
   * 
   * @param filterName
   *          - name of filter to be added
   * @return - returns the filter which was added
   */
  public OpenCVFilter addFilter(String filterName) {
    return addFilter(filterName, filterName);
  }

  /**
   * add filter by name and type
   * 
   * @param name
   *          - name of filter
   * @param filterType
   *          - type of filters
   * @return - the filter added
   */
  public OpenCVFilter addFilter(String name, String filterType) {
    String type = FILTER_PKG_PREFIX + filterType;
    info("adding {} filter {}", type, name);
    OpenCVFilter filter = (OpenCVFilter) Instantiator.getNewInstance(type, name);
    return addFilter(filter);
  }

  public void capture() {
    save();

    if (streamerEnabled) {
      streamer = (VideoStreamer) startPeer("streamer");
      streamer.attach(this);
    }

    startVideoProcessing();
    capturing = true;
    broadcastState(); // let everyone know
  }

  public void captureFromImageFile(String filename) {
    stopCapture();
    setFrameGrabberType("org.myrobotlab.opencv2.ImageFileFrameGrabber");
    setInputSource(INPUT_SOURCE_IMAGE_FILE);
    setInputFileName(filename);
    capture();
  }

  public void captureFromResourceFile(String filename) throws IOException {
    FileIO.extractResource(filename, filename);
    captureFromImageFile(filename);
  }

  public void clearSources() {
    VisionData.clearSources();
  }

  public IplImage get(String key) {
    if (lastOpenCVData != null && lastOpenCVData.containsKey(key)) {
      return lastOpenCVData.get(key);
    }
    return null;
  }

  public int getCameraIndex() {
    OpenCVFilterInput inputFilter = (OpenCVFilterInput) getFilter(selectedName);
    if (inputFilter != null) {
      return inputFilter.getCameraIndex();
    }
    return -1;
  }

  public SerializableImage getDisplay() {
    VisionData d = getOpenCVData();
    SerializableImage ret = new SerializableImage(d.getBufferedImage(), d.getSelectedFilterName());
    return ret;
  }

  public VisionData getFaceDetect() {
    OpenCVFilterFaceDetect fd = new OpenCVFilterFaceDetect();
    addFilter(fd);
    VisionData d = getOpenCVData();
    removeFilter(makeKey(fd.name));
    return d;
  }

  /**
   * getFilter returns a filter by "name" - this should be the ubiquitous
   * getFilter as the implementation of the actual concrete filter will be CV
   * (OpenCV || BoofCV) dependent
   * 
   * It hides the implementation details and allows for remote control (a good
   * thing)
   * 
   * @param name
   *          - name of filter
   * @return - the filter named or null if not found
   */
  public OpenCVFilter getFilter(String name) {
    for (Iterator<OpenCVFilter> it = filters.iterator(); it.hasNext();) {
      OpenCVFilter filter = it.next();
      if (filter.name.equals(name)) {
        return filter;
      }
    }
    error("could not find filter %s", name);
    return null;
  }

  // utility single shot functions - interesting idea
  // shouldn't this return just the points ?
  public VisionData getGoodFeatures() {
    addFilter(FILTER_GOOD_FEATURES_TO_TRACK, FILTER_GOOD_FEATURES_TO_TRACK);
    VisionData d = getOpenCVData();
    removeFilter(FILTER_GOOD_FEATURES_TO_TRACK);
    return d;
  }

  public FrameGrabber getGrabber() {
    OpenCVFilterInput inputFilter = (OpenCVFilterInput) getFilter(selectedName);
    if (inputFilter != null) {
      return inputFilter.getGrabber();
    }
    return null;
  }

  public String getGrabberType() {
    OpenCVFilterInput inputFilter = (OpenCVFilterInput) getFilter(selectedName);
    if (inputFilter != null) {
      return inputFilter.getGrabberType();
    }
    return null;
  }

  public String getInputFile() {
    OpenCVFilterInput inputFilter = (OpenCVFilterInput) getFilter(selectedName);
    if (inputFilter != null) {
      return inputFilter.getInputFile();
    }
    return null;
  }

  public String getInputSource() {
    OpenCVFilterInput inputFilter = (OpenCVFilterInput) getFilter(selectedName);
    if (inputFilter != null) {
      return inputFilter.getInputSource();
    }
    return null;
  }

  public VisionData getLastOpenCVData() {
    return lastOpenCVData;
  }

  public VisionData getOpenCVData() {
    return getOpenCVData(500);
  }

  // FIXME - don't try catch - expose the Exceptions - performance enhancement
  public VisionData getOpenCVData(Integer timeout) {
    VisionData data = null;
    try {

      // making fresh when blocking with a queue
      blockingData.clear();

      if (timeout == null || timeout < 1) {
        data = (VisionData) blockingData.take();
      } else {
        data = (VisionData) blockingData.poll(timeout, TimeUnit.MILLISECONDS);
      }

      return data;

    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return null;
  }

  public String getPipelineSelected() {
    OpenCVFilterInput inputFilter = (OpenCVFilterInput) getFilter(selectedName);
    if (inputFilter != null) {
      return inputFilter.getPipelineSelected();
    }
    return null;
  }

  @Override
  public VideoProcessWorker getWorker() {
    return worker;
  }

  /**
   * Callback from the SwingGui to the appropriate filter funnel through here
   * 
   * @param filterName
   *          - the name of the filter to invoke our function
   * @param method
   *          - the method on the filter to be invoked
   * @param params
   *          - the params of the method
   */
  public void invokeFilterMethod(String filterName, String method, Object... params) {
    OpenCVFilter filter = getFilter(filterName);
    if (filter != null) {
      Reflector.invokeMethod(filter, method, params);
    } else {
      log.error("invokeFilterMethod " + filterName + " does not exist");
    }
  }

  @Override
  public boolean isCapturing() {
    return capturing;
  }

  public boolean isStreamerEnabled() {
    return streamerEnabled;
  }

  /**
   * main video processing loop sources is a globally accessible VideoSources -
   * but is not threadsafe data is thread safe - at least the references to the
   * data are threadsafe even if the data might not be (although it "probably"
   * is :)
   * 
   * more importantly the references of data are synced with itself - so that
   * all references are from the same processing loop
   */
  @Override
  public void processVideo() {

    capturing = true;

    while (capturing) {
      try {

        /**
         * This is the creation of a new OpenCVData. References for serializable
         * data will be created new and added to in the pipeline. Internally
         * sources are static (non serializable) and continue to be added or
         * overwritten.
         * 
         * OpenCVData.sources is an index into what has previously processed
         */

        data = new VisionData(getName(), frameIndex, selectedInputName);
        ++frameIndex;

        // process each filter
        for (Iterator<OpenCVFilter> it = filters.iterator(); it.hasNext();) {
          OpenCVFilter filter = it.next();

          if (Logging.performanceTiming)
            Logging.logTime(String.format("pre set-filter %s", filter.name));

          // set the selected filter
          data.setFilter(filter);

          // get the source image this filter is chained to
          // should be safe and correct if operating in this
          // service
          // pipeline to another service needs to use data not
          // sources
          IplImage image = data.get(filter.sourceKey);

          // pre process handles image size & channel changes
          filter.preProcess(frameIndex, image, data);
          if (Logging.performanceTiming)
            Logging.logTime(String.format("preProcess-filter %s", filter.name));

          image = filter.process(image, data);

          if (Logging.performanceTiming)
            Logging.logTime(String.format("process-filter %s", filter.name));

          // process the image - push into source as new output
          // other pipelines will pull it off the from the sources

          data.add(image);

          // no display || merge display || fork display
          // currently there is no "display" in sources
          // i've got a user selection to display a particular
          // filter

          // FIXME - display becomes an attribute of the filter itself
          // -
          // FIXME - filters by default "display" when they are
          // selected and "un-display" when they are not
          // FIXME - however - filters could be left in the display
          // state if desired
          // if (publishDisplay && displayFilterName != null &&
          // displayFilterName.equals(filter.name)) {
          // if (publishDisplay && selectedName != null &&
          // selectedFilter.name.equals(filter.name)) {
          if (selectedName != null && selectedName.equals(filter.name)) {

            // data.setDisplayFilterName(displayFilterName);

            // The fact that I'm in a filter loop
            // and there is a display to publish means
            // i've got to process a filter's display
            // TODO - would be to have a set of displays if it's
            // needed
            // if displayFilter == null but we are told to
            // display - then display INPUT

            filter.display(image, data);

            // if display frame
            if (showFrameNumbers || showTimestamp) {

              frameTitle.setLength(0);

              if (showFrameNumbers) {
                frameTitle.append("frame ");
                frameTitle.append(frameIndex);
                frameTitle.append(" ");
              }

              if (showTimestamp) {
                frameTitle.append(System.currentTimeMillis());
              }
              // log.info("Adding text: " +
              // frameTitle.toString());
              cvPutText(image, frameTitle.toString(), cvPoint(20, 20), font, CvScalar.BLACK);
              for (Overlay overlay : overlays.values()) {
                // log.info("Overlay text:" + overlay.text);
                cvPutText(image, overlay.text, overlay.pos, overlay.font, overlay.color);
              }
            }

          } // end of display processing

        } // for each filter

        if (Logging.performanceTiming)
          Logging.logTime("filters done");

        // copy key references from sources to data
        // the references will presist and so will the data
        // for as long as the OpenCVData structure exists
        // Sources will contain new references to new data
        // next iteration
        // data.putAll(sources.getData()); not needed :)

        // has to be 2 tests for publishDisplay
        // one inside the filter loop - to set the display to a new
        // filter
        // and this one to publish - if it is left "unset" then the
        // input becomes the
        // display filter
        log.info("frame " + data.getFrameIndex());

        if (publishDisplay) {
          SerializableImage display = new SerializableImage(data.getDisplayBufferedImage(), data.getDisplayFilterName(), frameIndex);

          if (data.getDisplayBufferedImage() == null) {
            log.info("here");
          }

          invoke("publishDisplay", display);
        }

        // publish accumulated data
        if (publishOpenCVData) {
          invoke("publishOpenCVData", data);
        }

        // this has to be before record as
        // record uses the queue - this has the "issue" if
        // the consumer does not pickup-it will get stale
        // if (blockingData.size() == 0) {
        // blockingData.add(data);
        // }

      } catch (Exception e) {
        log.error("filter processing threw", e);
        stopVideoProcessing();
      }

      if (Logging.performanceTiming)
        Logging.logTime("finished pass");
    } // while capturing

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
   * publishing method for filters - used internally
   * 
   * @param filterWrapper
   *          - filter wrapped for single method common type
   * @return - FilterWrapper solves the problem of multiple types being resolved
   *         in the setFilterState(FilterWrapper data) method
   */
  public FilterWrapper publishFilterState(FilterWrapper filterWrapper) {
    return filterWrapper;
  }

  /**
   * publishing method for filters - uses string parameter for remote invocation
   * 
   * @return FilterWrapper solves the problem of multiple types being resolved
   *         in the onFilterState(FilterWrapper data) callback
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

  public void publishNoRecognizedFace() {

  }

  // the big switch <input>
  // FIXME - test overloaded !!!!
  public void publishOpenCVData(boolean b) {
    publishOpenCVData = b;
  }

  /**
   * the publishing point of all Vision goodies ! type conversion is held off
   * until asked for - then its cached SMART ! :)
   * 
   * @param data
   *          - the data to be published
   * @return - the published data
   */
  public final VisionData publishOpenCVData(VisionData data) {
    /*
     * GroG asks, "Is this necessary  ?" shouldnt the thing that publishes do it
     * through the capture method and update the capturing appropriately ?
     */
    /*
     * REMOVING if (data != null) { capturing = true; } else { capturing =
     * false; }
     */
    return data;
  }

  public String publishRecognizedFace(String value) {
    return value;
  }

  public SerializableImage publishTemplate(String source, BufferedImage img, int frameIndex) {
    SerializableImage si = new SerializableImage(img, source, frameIndex);
    return si;
  }

  // FIXME - need to record on a specific record filter !!!
  // maintain a default selected record filter selectedRecorder
  @Deprecated
  public void recordOutput(Boolean b) {
    // recordOutput = b;
    // FIXME - default name of recorder filter is recorder
    // FIXME - need another method to set selectedRecorder
  }

  public String recordSingleFrame() {
    // WOOHOO Changed threads & thread safe !
    // OpenCVData d = getLastData();
    VisionData d = getOpenCVData();
    /*
     * if (d == null) { log.error(
     * "could not record frame last OpenCVData is null"); return null; }
     */
    return d.writeDisplay();
    // return d.writeInput();
  }

  /**
   * remove a specific filter if not capturing remove it from the video
   * processor thread's filters if the video thread is capturing - must remove
   * it by adding it to a removeFilterQueue to be processed later
   * 
   * @param name - name of the filter to return
   */
  public void removeFilter(String name) {
    log.info("removing filter {} ", name);
    removeFilter(name);
  }

  public void removeFilter(OpenCVFilter filter) {
    if (!filter.isLocked()) {
      filters.remove(filter);
    }
    warn("could not remove %s", filter.name);
  }

  /**
   * remove filter by name if not capturing remove it from the video processor
   * thread's filters if the video thread is capturing - must remove it by
   * adding it to a removeFilterQueue to be processed later
   */
  public void removeFilters() {
    log.info("removeFilters");
    for (Iterator<OpenCVFilter> it = filters.iterator(); it.hasNext();) {
      OpenCVFilter filter = it.next();
      removeFilter(filter);
    }
  }

  public void setCameraIndex(Integer index) {
    OpenCVFilterInput inputFilter = (OpenCVFilterInput) getFilter(selectedName);
    if (inputFilter != null) {
      info("setting camera index to %s", index);
      inputFilter.setCameraIndex(index);
    }
  }

  public void setFilterName(String name) {
    selectedName = name;
  }

  public String getFilterName() {
    return selectedName;
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

  public void setFrameGrabberType(String grabberType) {
    OpenCVFilterInput inputFilter = (OpenCVFilterInput) getFilter(selectedName);
    if (inputFilter != null) {
      info("setting frame grabber type to %s", grabberType);
      inputFilter.setFrameGrabberType(grabberType);
    }
  }

  public void setInputFileName(String inputFile) {
    OpenCVFilterInput inputFilter = (OpenCVFilterInput) getFilter(selectedName);
    if (inputFilter != null) {
      info("setting input file to %s", inputFile);
      inputFilter.setInputFileName(inputFile);
    }
  }

  public void setInputSource(String inputSource) {
    OpenCVFilterInput inputFilter = (OpenCVFilterInput) getFilter(selectedName);
    if (inputFilter != null) {
      inputFilter.setInputSource(inputSource);
    }
  }

  public void setMask(String name, IplImage mask) {
    masks.put(name, mask);
  }

  /**
   * minimum time between processing frames - time unit is in milliseconds
   * 
   * @param time - minimum amount of time in millis
   */
  public void setMinDelay(int time) {
    OpenCVFilterInput inputFilter = (OpenCVFilterInput) getFilter(selectedName);
    if (inputFilter != null) {
      inputFilter.setMinDelay(time);
    }
  }

  public void setPipeline(String pipeline) {
    OpenCVFilterInput inputFilter = (OpenCVFilterInput) getFilter(selectedName);
    if (inputFilter != null) {
      inputFilter.setPipeline(pipeline);
    }
  }

  public void setStreamerEnabled(boolean streamerEnabled) {
    this.streamerEnabled = streamerEnabled;
  }

  public void showFrameNumbers(boolean b) {
    this.showFrameNumbers(b);
  }

  public void showTimestamp(boolean b) {
    this.showTimestamp = b;
  }

  public void stopCapture() {
    stopVideoProcessing();
    // FIXME -
    capturing = false;// change this to an event .. publishCapturing()
    // invoke("publishCapturing", false);
    broadcastState();
  }

  // FIXME - implement
  public void stopRecording(String filename) {
    // cvReleaseVideoWriter(outputFileStreams.get(filename).pointerByReference());
  }

  @Override
  public void stopService() {
    // shut down video
    stopVideoProcessing();
    // shut down service
    super.stopService();
  }

  public boolean undockDisplay(boolean b) {
    undockDisplay = b;
    broadcastState();
    return b;
  }

}
