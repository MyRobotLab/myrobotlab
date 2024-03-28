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

import static org.bytedeco.opencv.global.opencv_core.cvCopy;
import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;
import static org.bytedeco.opencv.global.opencv_core.cvSetImageROI;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameGrabber.ImageMode;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.OpenKinectFrameGrabber;
import org.bytedeco.opencv.opencv_core.AbstractCvScalar;
/*
<pre>
// extremely useful list of static imports - since auto-complete won't work with statics
 
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_calib3d.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_features2d.*;
import static org.bytedeco.opencv.global.opencv_flann.*;
import static org.bytedeco.opencv.global.opencv_highgui.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_ml.*;
import static org.bytedeco.opencv.global.opencv_objdetect.*;
import static org.bytedeco.opencv.global.opencv_photo.*;
import static org.bytedeco.opencv.global.opencv_shape.*;
import static org.bytedeco.opencv.global.opencv_stitching.*;
import static org.bytedeco.opencv.global.opencv_video.*;
import static org.bytedeco.opencv.global.opencv_videostab.*;


</pre>
*/
import org.bytedeco.opencv.opencv_core.CvPoint;
import org.bytedeco.opencv.opencv_core.CvPoint2D32f;
import org.bytedeco.opencv.opencv_core.CvRect;
import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.CvSize;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_imgproc.CvFont;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.cv.CVData;
import org.myrobotlab.cv.CVFilter;
import org.myrobotlab.document.Classification;
import org.myrobotlab.document.Classifications;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.image.ColoredPoint;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.image.WebImage;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.geometry.Point2df;
import org.myrobotlab.math.geometry.PointCloud;
import org.myrobotlab.net.Http;
import org.myrobotlab.opencv.CloseableFrameConverter;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.FrameFileRecorder;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.opencv.OpenCVFilterFaceDetectDNN;
import org.myrobotlab.opencv.OpenCVFilterFaceRecognizer;
import org.myrobotlab.opencv.OpenCVFilterKinectDepth;
import org.myrobotlab.opencv.OpenCVFilterYolo;
import org.myrobotlab.opencv.Overlay;
import org.myrobotlab.opencv.YoloDetectedObject;
import org.myrobotlab.reflection.Reflector;
import org.myrobotlab.service.abstracts.AbstractComputerVision;
import org.myrobotlab.service.config.OpenCVConfig;
import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.interfaces.ImageListener;
import org.myrobotlab.service.interfaces.ImagePublisher;
// import org.myrobotlab.swing.VideoWidget2;
import org.slf4j.Logger;

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
public class OpenCV extends AbstractComputerVision<OpenCVConfig> implements ImagePublisher {

  int vpId = 0;

  transient CanvasFrame canvasFrame = null;

  class VideoProcessor implements Runnable {

    transient Thread videoThread = null;

    @Override
    synchronized public void run() {
      // create a closeable frame converter
      CloseableFrameConverter converter = new CloseableFrameConverter();

      try {
        log.info("run - capturing");

        capturing = true;
        getGrabber();

        lengthInFrames = grabber.getLengthInFrames();
        lengthInTime = grabber.getLengthInTime();
        log.info("grabber {} started - length time {} length frames {}", grabberType, lengthInTime, lengthInFrames);

        // Wait for the Kinect to heat up.
        int loops = 0;
        while (grabber.getClass() == OpenKinectFrameGrabber.class && lengthInFrames == 0 && loops < 200) {
          lengthInFrames = grabber.getLengthInFrames();
          lengthInTime = grabber.getLengthInTime();
          sleep(40);
          loops++;
        }

        while (capturing && !stopping) {
          Frame newFrame = null;

          if (!singleFrame || (singleFrame && frameIndex < 1)) {
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
            // getGrabber will set the format to "depth" - which will grab depth
            // by default
            // here we need ot add the video

            IplImage video = ((OpenKinectFrameGrabber) grabber).grabVideo();
            data.putKinect(converter.toImage(newFrame), video);
          }

          processVideo(data);

          if (lengthInFrames > 1 && loop && frameIndex > lengthInFrames - 2) {
            grabber.setFrameNumber(0);
            frameIndex = 0;
          }
        } // end of while - no longer capturing

      } catch (Exception e) {
        log.error("failed getting frame", e);
      }
      // begin capturing ...

      frameIndex = 0;

      // attempt to close the grabber
      if (grabber != null) {
        try {
          // grabber.close();
          grabber.release();
        } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
          log.error("could not close grabber", e);
        }
      }
      grabber = null;
      converter.close();
      // end of stopping

      // stopCapture();
      stopping = false;
      capturing = false;
      // sleep(1000);
      broadcastState();
      log.info("run - stopped capture");
    }

    public void stop() {
      log.info("request to stop");
      if (!capturing) {
        log.info("processor already stopped");
        return;
      }

      // begin stopping
      stopping = true;
      videoThread.interrupt();
      int waitTime = 0;
      while (capturing && waitTime < 1000) {
        ++waitTime;
        sleep(10);
      }
      broadcastState();
      log.info("stopCapture waited {} times - done now", waitTime);
    }

    public void start() {
      log.info("request to start");
      if (!capturing) {
        videoThread = new Thread(vp, String.format("%s-video-processor-%d", getName(), ++vpId));
        videoThread.start();
        broadcastState();
        log.info("capture - started");
      } else {
        log.info("capture - already capturing - leaving");
      }
    }
  }

  transient final static public String BACKGROUND = "background";

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
  protected static final Set<String> globalGrabberTypes = new TreeSet<String>();
  protected static final Set<String> globalVideoFileExt = new TreeSet<String>();
  protected static final Set<String> globalImageFileExt = new TreeSet<String>();
  public static final String INPUT_KEY = "input";

  // FIXME - make more simple
  transient public final static String INPUT_SOURCE_CAMERA = "camera";

  transient public final static String INPUT_SOURCE_FILE = "imagefile";

  transient public final static String INPUT_SOURCE_PIPELINE = "pipeline";

  public final static Logger log = LoggerFactory.getLogger(OpenCV.class);
  public static final String OUTPUT_KEY = "output";
  transient final static public String PART = "part";
  static final String TEST_LOCAL_FACE_FILE_JPEG = "src/test/resources/OpenCV/multipleFaces.jpg";

  public final static String POSSIBLE_FILTERS[] = { "AdaptiveThreshold", "AddMask", "Affine", "And", "BlurDetector", "BoundingBoxToFile", "Canny", "ColorTrack", "Copy",
      "CreateHistogram", "Detector", "Dilate", "DL4J", "DL4JTransfer", "Erode", "FaceDetect", "FaceDetectDNN", "FaceRecognizer", "FaceTraining", "Fauvist", "FindContours", "Flip",
      "FloodFill", "FloorFinder", "FloorFinder2", "GoodFeaturesToTrack", "Gray", "HoughLines2", "Hsv", "ImageSegmenter", "Input", "InRange", "Invert", "KinectDepth",
      "KinectDepthMask", "KinectNavigate", "LKOpticalTrack", "Lloyd", "Mask", "MatchTemplate", "MiniXception", "MotionDetect", "Mouse", "Output", "Overlay", "PyramidDown",
      "PyramidUp", "ResetImageRoi", "Resize", "SampleArray", "SampleImage", "SetImageROI", "SimpleBlobDetector", "Smooth", "Solr", "Split", "SURF", "Tesseract", "TextDetector",
      "Threshold", "Tracker", "Transpose", "Undistort", "Yolo" };

  static final long serialVersionUID = 1L;

  transient public final static String SOURCE_KINECT_DEPTH = "kinect.depth.IplImage";

  static final Set<String> recordKeys = new HashSet<>();

  static {
    try {

      for (int i = 0; i < FrameGrabber.list.size(); ++i) {
        String ss = FrameGrabber.list.get(i);
        String fg = ss.substring(ss.lastIndexOf(".") + 1);
        globalGrabberTypes.add(fg);
      }

      globalGrabberTypes.add("ImageFile");
      globalGrabberTypes.add("Pipeline"); // to/from another opencv service
      globalGrabberTypes.add("Sarxos");
      globalGrabberTypes.add("MJpeg");

      globalVideoFileExt.add("mjpeg");
      globalVideoFileExt.add("mpeg");
      globalVideoFileExt.add("mp4");
      globalVideoFileExt.add("avi");
      globalVideoFileExt.add("mov");
      globalVideoFileExt.add("flv");
      globalVideoFileExt.add("wmv");

      globalImageFileExt.add("jpg");
      globalImageFileExt.add("jpeg");
      globalImageFileExt.add("gif");
      globalImageFileExt.add("tiff");
      globalImageFileExt.add("tif");
      globalImageFileExt.add("png");
      globalImageFileExt.add("pcd");
      globalImageFileExt.add("pdf");

      recordKeys.add("input.video");
      recordKeys.add("input.depth");

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
      return AbstractCvScalar.WHITE;
    } else if (c.equals("GRAY")) {
      return AbstractCvScalar.GRAY;
    } else if (c.equals("BLACK")) {
      return AbstractCvScalar.BLACK;
    } else if (c.equals("RED")) {
      return AbstractCvScalar.RED;
    } else if (c.equals("GREEN")) {
      return AbstractCvScalar.GREEN;
    } else if (c.equals("BLUE")) {
      return AbstractCvScalar.BLUE;
    } else if (c.equals("CYAN")) {
      return AbstractCvScalar.CYAN;
    } else if (c.equals("MAGENTA")) {
      return AbstractCvScalar.MAGENTA;
    } else if (c.equals("YELLOW")) {
      return AbstractCvScalar.YELLOW;
    } else {
      return AbstractCvScalar.BLACK;
    }
  }

  static public Set<String> getGrabberTypes() {
    return globalGrabberTypes;
  }

  /**
   * @return get the current list of possible filter types
   */
  static public String[] getPossibleFilters() {
    return POSSIBLE_FILTERS;
  }

  public void stopStreamer() {
    try {
      if (ffmpegStreamer == null) {
        log.warn("webm already stopped");
        return;
      }
      ffmpegStreamer.close();
      ffmpegStreamer = null;
    } catch (Exception e) {
      log.error("stopStreamer threw", e);
    }
  }

  /**
   * resets all the input specific settings
   */
  public void reset() {
    stopCapture();
    setGrabberType(null);
    setInputSource(null);
    setInputFileName(null);
    singleFrame = false;
    lastFrame = null;
    blockingData.clear();
    removeFilters();
  }

  public static IplImage cropImage(IplImage img, CvRect rect) {
    CvSize sz = new CvSize();
    sz.width(rect.width()).height(rect.height());
    cvSetImageROI(img, rect);
    IplImage cropped = cvCreateImage(sz, img.depth(), img.nChannels());
    // Copy original image (only ROI) to the cropped image
    cvCopy(img, cropped);
    return cropped;
  }

  transient BlockingQueue<Map<String, List<Classification>>> blockingClassification = new LinkedBlockingQueue<>();

  transient BlockingQueue<OpenCVData> blockingData = new LinkedBlockingQueue<>();
  Integer cameraIndex;
  volatile boolean capturing = false;
  Classifications classifications = null;
  boolean closeOutputs = false;

  /**
   * when video process is put in a stopping state
   */
  boolean stopping = false;

  /**
   * color of overlays
   */
  transient Color color = getAwtColor("RED");

  OpenCVData data;
  private String displayFilter = "display";

  /**
   * all the filters in the pipeline
   */
  Map<String, OpenCVFilter> filters = new LinkedHashMap<String, OpenCVFilter>();

  transient CvFont font = new CvFont();

  String format = null;

  long frameEndTs;
  int frameIndex = 0;
  long frameStartTs;

  StringBuffer frameTitle = new StringBuffer();
  transient FrameGrabber grabber = null;

  String grabberType;

  Integer height = null;
  String inputFile = null;

  String inputSource = OpenCV.INPUT_SOURCE_CAMERA;

  transient Frame lastFrame;
  transient IplImage lastImage;
  // frame grabber properties
  int lengthInFrames = -1;

  long lengthInTime = -1;

  String recordingFilename;

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

  boolean undockDisplay = false;

  final transient private VideoProcessor vp = new VideoProcessor();

  Integer width = null;

  boolean recordingFrames = false;

  private boolean singleFrame;

  private PointCloud lastPointCloud;

  /**
   * Used to provide a thread safe way of setting filter states
   */
  private Map<String, OpenCVFilter> newFilterStates = new HashMap<>();

  boolean display = true;

  /**
   * for native canvas frame view of output
   */
  public boolean nativeViewer = true;

  /**
   * local reference of global frame grabber types
   */
  protected Set<String> grabberTypes;

  /**
   * local reference of video file ext
   */
  protected Set<String> videoFileExt;

  /**
   * local reference of image file ext
   */
  protected Set<String> imageFileExt;

  // transient private VideoWidget2 videoWidget = null;

  static String DATA_DIR;

  public OpenCV(String n, String id) {
    super(n, id);
    // pre-loading so filters, functions and tests don't incur a performance hit
    // while
    // processing
    /*
     * Loader.load(opencv_java.class); Loader.load(opencv_objdetect.class);
     * Loader.load(opencv_imgproc.class);
     */
    /*
     * Loader.load(opencv_imgproc.class); Loader.load(opencv_calib3d.class);
     * Loader.load(opencv_core.class); Loader.load(opencv_features2d.class);
     * Loader.load(opencv_flann.class); Loader.load(opencv_highgui.class);
     * Loader.load(opencv_imgcodecs.class); Loader.load(opencv_ml.class);
     * Loader.load(opencv_objdetect.class); Loader.load(opencv_photo.class);
     * Loader.load(opencv_shape.class); Loader.load(opencv_stitching.class);
     * Loader.load(opencv_video.class); Loader.load(opencv_videostab.class);
     */

    putText(20, 20, "time:  %d");
    putText(20, 30, "frame: %d");
    DATA_DIR = getDataDir();

    grabberTypes = globalGrabberTypes;
    videoFileExt = globalVideoFileExt;
    imageFileExt = globalVideoFileExt;

    // Init default config
    if (cameraIndex == null) {
      cameraIndex = 0;
    }
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
   * @param filterName
   *          - name of filter
   * @return the filter
   */
  public CVFilter addFilter(String filterName) {
    String filterType = filterName.substring(0, 1).toUpperCase() + filterName.substring(1);
    return addFilter(filterName, filterType);
  }

  @Override
  public CVFilter addFilter(String name, String filterType) {
    String type = String.format("org.myrobotlab.opencv.OpenCVFilter%s", filterType);
    OpenCVFilter filter = (OpenCVFilter) Instantiator.getNewInstance(type, name);
    if (filter == null) {
      error("cannot create filter %s of type %s", name, type);
      return null;
    }
    addFilter(filter);
    return filter;
  }

  /**
   * capture starts the frame grabber and video processing threads
   */
  @Override
  synchronized public void capture() {
    log.info("capture()");
    vp.start();
  }

  public void capture(FrameGrabber grabber) throws org.bytedeco.javacv.FrameGrabber.Exception {
    stopCapture();
    this.grabber = grabber;
    grabber.restart();
    capture();
  }

  /**
   * capture from a camera
   * 
   * @param cameraIndex
   *          the camera index to capture from
   */
  public void capture(Integer cameraIndex) {
    if (cameraIndex == null) {
      this.cameraIndex = 0;
    }
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
   *          the file to use as the input filename.
   * 
   */
  public void capture(String filename) {
    stopCapture();
    setInputSource(INPUT_SOURCE_FILE);
    setInputFileName(filename);
    capture();
  }

  public void captureFromResourceFile(String filename) throws IOException {
    capture(filename);
  }

  /**
   * Gets valid camera indexes by iterating through 8
   * 
   * @return
   */
  public List<Integer> getCameraIndexes() {
    List<Integer> cameraIndexes = new ArrayList<>();
    if (isCapturing()) {
      error("cannot get indexes when capturing");
      return cameraIndexes;
    }

    // preserving original state
    String previousType = grabberType;
    Integer previousIndex = cameraIndex;

    for (int i = 0; i < 8; i++) {
      try {
        FrameGrabber grabber = new OpenCVFrameGrabber(i);
        grabber.start();
        Frame frame = grabber.grab();
        if (frame != null) {
          cameraIndexes.add(i);
        }
        grabber.stop();
        grabber = null;
      } catch (Exception e) {
        log.info(String.format("not able to camera grab a frame from camera %d", i));
      }
    }

    // resetting to original type
    grabberType = previousType;
    cameraIndex = previousIndex;

    return cameraIndexes;
  }

  public int getCameraIndex() {
    return this.cameraIndex;
  }

  /**
   * default 5 second wait
   * 
   * @return map of classifications
   * 
   */
  public Map<String, List<Classification>> getClassifications() {
    return getClassifications(5000);
  }

  public Map<String, List<Classification>> getClassifications(int timeout) {
    blockingClassification.clear();
    Map<String, List<Classification>> ret = new HashMap<>();
    String name = "yolo";
    try {
      OpenCVFilterYolo fd = new OpenCVFilterYolo(name);
      addFilter(fd);
      long startTs = System.currentTimeMillis();
      while (ret.keySet().size() == 0 && System.currentTimeMillis() - startTs < timeout) {
        ret.putAll(blockingClassification.poll(timeout, TimeUnit.MILLISECONDS));
      }
    } catch (InterruptedException e) {
    }
    removeFilter(name);
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

  @Deprecated /* use getFaces */
  public OpenCVData getFaceDetect() {
    // willing to wait up to 5 seconds
    // but if we find a face before 5s we wont wait
    return getFaceDetect(5000);
  }

  @Deprecated /* use getFaces */
  public OpenCVData getFaceDetect(int timeout) {
    OpenCVFilterFaceDetectDNN fd = new OpenCVFilterFaceDetectDNN("face");
    addFilter(fd);
    OpenCVData d = getOpenCVData(timeout);
    removeFilter(fd.name);
    return d;
  }

  public List<Classification> getFaces() {
    return getFaces(5000); // FIXME - change to MAX_TIMOUT
  }

  public List<Classification> getFaces(int timeout) {
    blockingClassification.clear();
    Map<String, List<Classification>> ret = new HashMap<>();
    String name = "face";
    try {
      OpenCVFilterFaceDetectDNN fd = new OpenCVFilterFaceDetectDNN(name);
      addFilter(fd);
      // sleep(100);
      // fd.enable();
      long startTs = System.currentTimeMillis();
      while (!ret.keySet().contains("face") && System.currentTimeMillis() - startTs < timeout) {
        Map<String, List<Classification>> faces = blockingClassification.poll(timeout, TimeUnit.MILLISECONDS);
        if (faces != null) {
          ret.putAll(faces);
        }
      }
    } catch (InterruptedException e) {
    }
    removeFilter(name);
    return ret.get("face");
  }

  /**
   * get a filter by name
   * 
   * @param name
   *          filter name to lookup
   * @return the filter by name o/w null
   * 
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

    // if grabber type != null && input source == null
    // choose the apprpriate input source (best guess)
    if (grabberType != null && inputSource == null) {
      if (grabberType.equals("FFmpeg") || grabberType.equals("ImageFile")) {
        inputSource = INPUT_SOURCE_FILE;
      } else {
        inputSource = INPUT_SOURCE_CAMERA;
      }
    }

    // certain files are "not" supported out of the box by certain grabbers
    // ffmpeg is increadibly capable, however it won't do a youtube stream
    // so we have to download/cache it and change the filename
    if (inputFile != null && inputFile.startsWith("http")) {
      // get and cache image file
      // FIXME - perhaps "test" stream to try to determine what "type" it is -
      // mjpeg/jpg/gif/ octet-stream :( ???
      if (grabberType == null || (grabberType != null && (!grabberType.equals("MJpeg") && !grabberType.equals("IPCamera")))) {
        inputFile = getImageFromUrl(inputFile);
      }
    }

    String ext = null;
    if (inputFile != null) {
      int pos = inputFile.lastIndexOf(".");
      if (pos > 0) {
        ext = inputFile.substring(pos + 1).toLowerCase();
      }
    }
    if (grabberType != null && (grabberType.equals("FFmpeg") || grabberType.equals("ImageFile")) && inputSource.equals(INPUT_SOURCE_CAMERA)) {
      log.info("invalid state of ffmpeg and input source camera - setting to OpenCV frame grabber");
      grabberType = "OpenCV";
    }

    if (grabberType != null && grabberType.equals("OpenCV") && inputSource.equals(INPUT_SOURCE_FILE)) {
      log.info("invalid state of opencv and input source file - setting to FFmpeg frame grabber");
      if (ext != null && globalVideoFileExt.contains(ext)) {
        grabberType = "FFmpeg";
      } else {
        grabberType = "ImageFile";
      }
    }

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
        grabberType = "ImageFile";
      } else {
        if (ext != null && globalVideoFileExt.contains(ext)) {
          grabberType = "FFmpeg";
        } else {
          grabberType = "ImageFile";
        }
      }
    }

    if ((grabberType == null) && (inputSource.equals(INPUT_SOURCE_CAMERA))) {
      grabberType = "OpenCV";
    } else if ((grabberType == null) && (inputSource.equals(INPUT_SOURCE_FILE))) {
      if (ext != null && globalVideoFileExt.contains(ext)) {
        grabberType = "FFmpeg";
      } else {
        grabberType = "ImageFile";
      }
    }

    if (isVirtual() && inputSource.equals(INPUT_SOURCE_CAMERA)) {
      grabberType = "ImageFile";
      inputSource = INPUT_SOURCE_FILE;
      // FIXME - we should put a single image in src/main/resources/resource/ -
      // to be extracted with resources
      inputFile = "src/test/resources/OpenCV/multipleFaces.jpg";
    }

    String prefixPath;
    if (/* "IPCamera".equals(grabberType) || */ "Pipeline".equals(grabberType) || "ImageFile".equals(grabberType) || "Sarxos".equals(grabberType) || "MJpeg".equals(grabberType)) {
      prefixPath = "org.myrobotlab.opencv.";
    } else {
      prefixPath = "org.bytedeco.javacv.";
    }

    newGrabberType = String.format("%s%sFrameGrabber", prefixPath, grabberType);

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
      OpenKinectFrameGrabber g = (OpenKinectFrameGrabber) grabber;
      // g.setImageMode(ImageMode.COLOR); - this still gives grey - but 3
      // channels of 16 bit :P
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

    singleFrame = isSingleFrame();

    broadcastState(); // restarting/enabled filters ?? wth?
    return grabber;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public String getGrabberType() {
    return grabberType;
  }

  public IplImage getImage() {
    return lastImage;
  }

  /**
   * "Easy" Base64 web image from display last frame
   * 
   * @return
   */
  public String getWebImage() {
    try {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      String imgType = "jpg";
      BufferedImage bi = getDisplay();
      if (bi != null) {
        ImageIO.write(bi, imgType, os);
        os.close();
        return String.format("data:image/%s;base64,%s", imgType, CodecUtils.toBase64(os.toByteArray()));
      }
    } catch (Exception e) {
      error(e);
    }
    return null;
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
    return getOpenCVData(3500);
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

  /**
   * Callback from the SwingGui (e.g. clicking on the display) routes to the
   * appropriate filter through this method.
   * 
   * @param filterName
   *          the name of the fitler
   * @param method
   *          the method to invoke
   * @param params
   *          the params to pass
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

  /**
   * conversion from buffered image to base64 encoded jpg
   * 
   * @param img
   *          the image to convert
   * @return base64jpeg version of buffered image
   */
  public String toBase64Jpg(BufferedImage img) {
    try {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      ImageIO.write(img, "jpg", os);
      os.close();
      String ret = CodecUtils.toBase64(os.toByteArray());
      return ret;
    } catch (Exception e) {
      log.error("toBase64Jpg threw", e);
    }
    return null;
  }

  private void processVideo(OpenCVData data) throws org.bytedeco.javacv.FrameGrabber.Exception, InterruptedException {

    if (stopping) {
      log.warn("stopping processing of image capture is stopping");
      return;
    }

    // process each filter
    // for (String filterName : filters.keySet()) {
    for (OpenCVFilter filter : filters.values()) {
      if (filter.isEnabled() & !stopping) {
        IplImage input = filter.setData(data);
        if (input == null) {
          log.error("could not get setData image");
          continue;
        }

        // process the previous filter's output
        IplImage processed = filter.process(input);
        filter.postProcess(processed);
        filter.processDisplay();
        processFilterStateUpdates(filter);
      }
    } // for each filter

    // get the display filter to process

    putText("frame: %d", frameIndex);
    putText("time:  %d", frameStartTs);

    BufferedImage displayImage = data.getDisplay();
    if (displayImage != null) {
      Graphics2D g2d = displayImage.createGraphics();

      if (g2d != null) {
        // we have a display...
        for (Overlay overlay : overlays.values()) {
          if (overlay.color != null) {
            g2d.setColor(overlay.color);
          }
          g2d.drawString(overlay.text, overlay.x, overlay.y);
        }

        BufferedImage b = data.getDisplay();
        SerializableImage si = new SerializableImage(b, displayFilter, frameIndex);
        invoke("publishDisplay", si);

        if (webViewer) {
          // broadcast(???)
          WebImage webImage = new WebImage(b, getName(), frameIndex);
          // latency use the original ts from before fetch image and the filters
          // !
          webImage.ts = data.getTs();
          broadcast("publishWebDisplay", webImage);
        }

        if (!isHeadless() && nativeViewer) {

          if (canvasFrame == null) {
            canvasFrame = new CanvasFrame(String.format("%s - %s", getName(), displayFilter));
            // canvasFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
          }

          if (b != null) {
            canvasFrame.showImage(b);
          } else {
            Frame frame = data.getFrame();
            if (frame != null) {
              canvasFrame.showImage(frame);
            }
          }
        } else if (canvasFrame != null && !nativeViewer) {
          canvasFrame.dispose();
          canvasFrame = null;
        }

      }
    }

    invoke("publishOpenCVData", data);

    // standard generic CvData publish
    invoke("publishCvData", data);

    if (blockingData.size() == 0) {
      blockingData.add(data);
    }

    if (recording || recordingFrames) {
      record(data);
    }

    if (ffmpegStreamer != null) {
      try {
        ffmpegStreamer.record(data.getInputFrame());
      } catch (Exception e) {
        log.error("webm threw", e);
      }
    }

    frameEndTs = System.currentTimeMillis();

    // delay if needed to maxFps
    if (maxFps != null && frameEndTs - frameStartTs < 1000 / maxFps) {
      sleep((1000 / maxFps) - (int) (frameEndTs - frameStartTs));
    }

    data.dispose();

  } // end processVideo

  /**
   * A new method to protect filters from other threads doing updates possibly
   * creating invalid states from processing "partially" copied filter states
   * 
   * The video processing thread will be the one which does the copy of
   * requested state changes from other threads - which will guarantee a "full"
   * copy of variables before processing begins again
   * 
   * @param filter
   */
  private void processFilterStateUpdates(OpenCVFilter filter) {
    // TODO Auto-generated method stub
    OpenCVFilter newFilterState = newFilterStates.remove(filter.name);
    if (newFilterState != null) {
      // update our filter with a completely updates new filter
      Service.copyShallowFrom(filter, newFilterState);
    }

  }

  /**
   * base 64 jpg frame image
   * 
   * @param data
   *          webimage data
   * @return the web image data
   */
  public WebImage publishWebDisplay(WebImage data) {
    return data;
  }

  // FIXME - this is good in it has a bunch of publish points, but there is no
  // POJO Classification publish point .. yet
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

  // publish the rects where motion was detected
  // TODO: make a better object to publish
  public ArrayList<Rect> publishMotionDetected(ArrayList<Rect> rects) {
    return rects;
  }

  // FIXME - POJO classification :( not doc
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
  @Override
  public final SerializableImage publishDisplay(SerializableImage img) {
    return img;
  }

  /**
   * Publishing method for filters - used internally
   * 
   * @param filterWrapper
   *          wraps a filter
   * 
   * @return FilterWrapper solves the problem of multiple types being resolved
   *         in the setFilterState(FilterWrapper data) method
   */
  public FilterWrapper publishFilterState(FilterWrapper filterWrapper) {
    return filterWrapper;
  }

  /**
   * Publishing method for filters - uses string parameter for remote invocation
   * 
   * @param name
   *          name of filter to publish state for
   * 
   * @return FilterWrapper solves the problem of multiple types being resolved
   *         in the setFilterState(FilterWrapper data) method
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
   * @param data
   *          the opencv data
   * @return cvdata
   * 
   */
  public final OpenCVData publishOpenCVData(OpenCVData data) {
    return data;
  }

  public final CVData publishCvData(CVData data) {
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
   * @param x
   *          coordinate
   * @param y
   *          coordinate
   * @param format
   *          format string
   * @param color
   *          color
   * 
   */
  public void putText(int x, int y, String format, String color) {
    Overlay overlay = new Overlay(x, y, format, color);
    overlays.put(format, overlay);
  }

  /**
   * the "light weight" put - it does not create any new cv objects
   * 
   * @param format
   *          format for the text
   * @param args
   *          args to format into the text
   * 
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

  transient FFmpegFrameRecorder ffmpegStreamer = null;

  /**
   * try native first - this requires base64 frame encoding
   */
  protected boolean webViewer = false;

  public void startStreamer() {
    try {
      if (ffmpegStreamer != null) {
        log.warn("webm already initialized");
        return;
      }

      int imageWidth = (width == null) ? 640 : width;
      int imageHeight = (height == null) ? 480 : height;

      ffmpegStreamer = new FFmpegFrameRecorder("tcp://localhost:9090?listen", imageWidth, imageHeight);
      ffmpegStreamer.setFormat("webm");
      ffmpegStreamer.start();

      // ~~~ https://github.com/bytedeco/javacv/issues/598 ~~~

      /*
       * webm = new FFmpegFrameRecorder("http://localhost:9090", webWidth,
       * webHeight, 0); webm.setVideoOption("preset", "ultrafast");
       * webm.setVideoCodec(avcodec.AV_CODEC_ID_H264); webm.setAudioCodec(0);
       * webm.setPixelFormat(avutil.AV_PIX_FMT_YUV420P); webm.setFormat("webm");
       * webm.setGopSize(10); webm.setFrameRate(32); webm.setVideoBitrate(5000);
       * webm.setOption("content_type", "video/webm"); webm.setOption("listen",
       * "1"); webm.start();
       */
      log.info("started webm");
    } catch (Exception e) {
      log.error("startStreamer threw", e);
    }
  }

  /**
   * Generates either a flv movie file from selected output OR a series of
   * non-lossy pngs from OpenCVData.
   * 
   * key- input, filter, or display
   * 
   * @param data
   *          data
   */
  public void record(OpenCVData data) {
    try {
      Frame frame = data.getFrame();

      if (!outputFileStreams.containsKey(recordingSource)) {

        /**
         * <pre>
         * Records a single output stream into flv format movie file 
         * for which FFmpegFrameGrabber can "probably" ingest ...
         * 
         *  This one freezes on windows :P
         FrameRecorder recorder = new OpenCVFrameRecorder(filename, frame.imageWidth, frame.imageHeight);
         recorder.setFrameRate(15);
         recorder.setPixelFormat(1);
         recorder.start();
         * </pre>
         */
        FrameRecorder recorder = null;
        if (!recordingFrames) {
          recordingFilename = String.format(getDataDir() + File.separator + "%s-%d.flv", recordingSource, System.currentTimeMillis());
          info("recording %s", recordingFilename);
          recorder = new FFmpegFrameRecorder(recordingFilename, frame.imageWidth, frame.imageHeight, 0);
          recorder.setFormat("flv");
        } else {
          recorder = new FrameFileRecorder(getDataDir());
          // recorder.setFormat("png");
        }
        // recorder.setSampleRate(frameRate); for audio
        recorder.setFrameRate(maxFps);
        recorder.start();
        broadcastState();
        outputFileStreams.put(recordingSource, recorder);
      }
      // TODO - add input, filter & display
      outputFileStreams.get(recordingSource).record(frame);

      if (closeOutputs) {
        FrameRecorder output = outputFileStreams.get(recordingSource);
        outputFileStreams.remove(recordingSource);
        output.stop();
        output.release();
        if (!recordingFrames) {
          info("finished recording %s", recordingFilename);
        } else {
          info("finished recording frames to %s", getDataDir());
        }
        recording = false;
        recordingFrames = false;
        closeOutputs = false;
        broadcastState();
      }
    } catch (Exception e) {
      log.error("record threw", e);
    }
  }

  /**
   * records a single frame to the filesystem from our data
   * 
   * @return the filename
   */
  public String recordFrame() {
    try {
      OpenCVData d = getOpenCVData();
      String filename = d.writeDisplay(getDataDir(), "png");
      info("saved frame %s", filename);
      return filename;
    } catch (Exception e) {
      error(e);
    }
    return null;
  }

  public ImageData saveImage() {
    String src = recordFrame();
    ImageData image = new ImageData();
    image.source = getName();
    image.name = src;
    image.src = src;
    invoke("publishImage", image);
    return image;
  }

  /**
   * @param name
   *          remove a filter by name
   */
  @Override
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
  @Override
  synchronized public void removeFilters() {
    for (OpenCVFilter filter : filters.values()) {
      filter.release();
    }
    filters = new LinkedHashMap<>();
    broadcastState();
  }

  /**
   * disable all the filters in the pipeline
   */
  @Override
  synchronized public void disableAll() {
    for (OpenCVFilter filter : filters.values()) {
      filter.disable();
    }
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

  static public void saveToFile(String filename, IplImage image) {
    CloseableFrameConverter converter = new CloseableFrameConverter();
    try {
      int i = filename.lastIndexOf(".");
      String ext = "png";
      if (i > 0) {
        ext = filename.substring(i + 1).toLowerCase();
      }
      BufferedImage bi = converter.toBufferedImage(image);
      FileOutputStream fos = new FileOutputStream(filename);
      ImageIO.write(bi, ext, new MemoryCacheImageOutputStream(fos));
      fos.close();
    } catch (IOException e) {
      log.error("saveToFile threw", e);
    }
    converter.close();
  }

  @Override
  public Integer setCameraIndex(Integer index) {
    this.cameraIndex = index;
    return index;
  }

  public void setColor(String colorStr) {
    color = getAwtColor(colorStr);
  }

  /**
   * enable() and setDisplayFilter() needed filter
   * 
   * @param name
   *          name of the filter to set active
   *
   */
  public void setActiveFilter(String name) {
    OpenCVFilter filter = filters.get(name);
    if (filter == null) {
      return;
    }
    filter.enable();
    setDisplayFilter(name);
  }

  @Override
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

    if (filter != null && !filter.isEnabled()) {
      name = "input";
    }

    if (filter == null || "input".equals(name) || "output".equals(name)) {
      log.info("make select & inverse select");
    } else {
      filter.enableDisplay();
    }
    broadcastState();
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

  /**
   * Json encoded filter to be used to update state information within the named
   * filter
   * 
   * @param name
   *          name of the filter
   * @param data
   *          state date to set.
   */
  public void setFilterState(String name, String data) {
    OpenCVFilter filter = getFilter(name);
    if (filter == null) {
      error("setFilterState - could not find %s ", name);
      return;
    }
    OpenCVFilter otherFilter = CodecUtils.fromJson(data, filter.getClass());
    if (otherFilter == null) {
      error("setFilterState - could not decode %s ", name);
      return;
    }
    // not thread safe
    // Service.copyShallowFrom(filter, otherFilter);
    newFilterStates.put(otherFilter.name, otherFilter);
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

  public void setWebViewer(boolean b) {
    webViewer = b;
  }

  public void setNativeViewer(boolean b) {
    nativeViewer = b;
  }

  @Override
  synchronized public void stopCapture() {
    log.info("stopCapture");
    vp.stop();
  }

  public void stopRecording() {
    closeOutputs = true;
  }

  @Override
  public void stopService() {
    super.stopService();
    stopCapture();
    if (canvasFrame != null) {
      canvasFrame.dispose();
      canvasFrame = null;
    }
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

  public void recordFrames() {
    recordingFrames = true;
    recording = true;
  }

  /**
   * method used to inspect details of grabber configuration and capture request
   * to determine if a single frame is to be returned
   * 
   * @return
   */
  private boolean isSingleFrame() {
    if (inputSource.equals(INPUT_SOURCE_FILE) && inputFile != null) {
      String testExt = inputFile.toLowerCase();
      if (testExt.endsWith(".jpg") || testExt.endsWith(".jpeg") || testExt.endsWith(".png") || testExt.endsWith(".gif") || testExt.endsWith(".tiff") || testExt.endsWith(".tif")) {
        return true;
      }
    }
    return false;
  }

  static public String putCacheFile(String url, byte[] data) {
    try {
      String path = OpenCV.DATA_DIR + File.separator + url.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
      FileIO.toFile(path, data);
      return path;
    } catch (Exception e) {
      log.error("putCacheFile threw", e);
    }
    return null;
  }

  static public String getCacheFile(String url) {
    String path = OpenCV.DATA_DIR + File.separator + url.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    File f = new File(path);
    if (f.exists()) {
      return path;
    }
    return null;
  }

  static public String getImageFromUrl(String url) {
    String ret = getCacheFile(url);
    if (ret != null) {
      return ret;
    }
    byte[] data = Http.get(url);
    if (data == null) {
      log.error("could not get {}", url);
      return null;
    }
    return putCacheFile(url, data);
  }

  public PointCloud publishPointCloud(PointCloud pointCloud) {
    lastPointCloud = pointCloud;
    return pointCloud;
  }

  public PointCloud getPointCloud() {
    return lastPointCloud;
  }

  // Filter enable/disable helper methods.
  @Override
  public void enableFilter(String name) {
    OpenCVFilter f = filters.get(name);
    if (f != null && !f.isEnabled()) {
      f.enable();
      broadcastState();
    }
  }

  /**
   * flip the video display vertically
   * 
   * @param toFlip
   */
  public void flip(boolean toFlip) {
    config.flip = toFlip;
    if (config.flip) {
      addFilter("Flip");
    } else {
      removeFilter("Flip");
    }
  }

  @Override
  public void disableFilter(String name) {
    OpenCVFilter f = filters.get(name);
    if (f != null && f.isEnabled()) {
      f.disable();
      broadcastState();
    }
  }

  public void toggleFilter(String name) {
    OpenCVFilter f = filters.get(name);
    if (f != null) {
      if (f.isEnabled())
        f.disable();
      else
        f.enable();
      broadcastState();
    }
  }

  public void setDisplay(boolean b) {
    display = b;
  }

  public void samplePoint(int x, int y) {
    samplePoint(displayFilter, x, y);
  }

  public void samplePoint(String filter, int x, int y) {
    OpenCVFilter f = getFilter(filter);
    if (f == null) {
      log.warn("cannot sample point on null filter");
      return;
    }
    f.samplePoint(x, y);
  }

  public void saveFile(String filename, String data) {
    FileOutputStream fos = null;
    try {
      String path = FileIO.gluePaths(getDataDir(), filename);
      fos = new FileOutputStream(path);
      byte[] decoded = CodecUtils.fromBase64(data);
      fos.write(decoded);
      fos.close();
      setInputFileName(path);
      // restart grabber if capturing
      if (isCapturing()) {
        stopCapture();
        sleep(300);
        capture();
      }
      broadcastState();
    } catch (Exception e) {
      error(e.getMessage());
    }
  }

  @Override
  public OpenCVConfig getConfig() {
    super.getConfig();
    // FIXME - remove member vars use config only
    config.capturing = capturing;
    config.cameraIndex = cameraIndex;
    // TODO: make the grabber config a nested object to clean this up..
    config.grabberType = grabberType;
    config.inputFile = inputFile;
    config.inputSource = inputSource;
    config.nativeViewer = nativeViewer;
    config.webViewer = webViewer;
    config.filters = new LinkedHashMap<>();
    config.filters.putAll(filters);

    return config;
  }

  @Override
  public OpenCVConfig apply(OpenCVConfig c) {
    super.apply(c);
    setCameraIndex(c.cameraIndex);
    setGrabberType(c.grabberType);
    setInputFileName(c.inputFile);
    setInputSource(c.inputSource);

    setNativeViewer(c.nativeViewer);

    setWebViewer(c.webViewer);

    filters.clear();
    if (c.filters != null) {
      for (OpenCVFilter f : c.filters.values()) {
        addFilter(f);
        // TODO: better configuration of the filter when it's added.
      }
    }

    flip(c.flip);

    if (c.capturing) {
      capture();
    }

    return c;
  }

  public long getFrameStartTs() {
    return frameStartTs;
  }

  public static void main(String[] args) throws Exception {

    try {
      Runtime.main(new String[] { "--id", "admin" });
      LoggingFactory.init("INFO");

      // Runtime.getInstance().load();

      // Runtime.start("python", "Python");
      OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");
      cv.capture();

      cv.addFilter(new OpenCVFilterYolo("yolo"));
      sleep(1000);
      cv.removeFilters();

      OpenCVFilter fr = new OpenCVFilterFaceRecognizer("fr");
      cv.addFilter(fr);
      // OpenCVFilterTracker tracker = new OpenCVFilterTracker("tracker");
      // cv.addFilter(tracker);
      // OpenCVFilterLKOpticalTrack lk = new OpenCVFilterLKOpticalTrack("lk");
      // cv.addFilter(lk);
      // OpenCVFilterFaceDetectDNN faceDnn = new
      // OpenCVFilterFaceDetectDNN("face");
      // cv.addFilter(faceDnn);
      // OpenCVFilterMiniXception mini = new OpenCVFilterMiniXception("mini");
      // cv.addFilter(mini);

      // OpenCVFilterTextDetector td = new OpenCVFilterTextDetector("td");
      // cv.addFilter(td);

      // OpenCVFilterMotionDetect md = new OpenCVFilterMotionDetect("md");
      // cv.addFilter(md);

      // cv.capture(4);
      // FFmpegFrameGrabber grabber = new
      // FFmpegFrameGrabber("tcp://worke-pi:2222");
      // grabber.start();
      // cv.capture(grabber);;

      // Runtime.start("gui", "SwingGui");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      // FFmpegFrameRecorder test = new

      // FFmpegFrameRecorder recorder = new
      // FFmpegFrameRecorder("tcp://localhost:9090?listen", 640, 480);
      // recorder.setFormat("webm");
      // recorder.start();

      /**
       * <pre>
       * 
       * https://stackoverflow.com/questions/43008150/android-javacv-ffmpeg-webstream-to-local-static-website
       *
       * private void initLiveStream() throws FrameRecorder.Exception {
       * 
       * frameRecorder = new FFmpegFrameRecorder("http://localhost:9090",
       * imageWidth, imageHeight, 0); frameRecorder.setVideoOption("preset",
       * "ultrafast"); frameRecorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
       * frameRecorder.setAudioCodec(0);
       * frameRecorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
       * frameRecorder.setFormat("webm"); frameRecorder.setGopSize(10);
       * frameRecorder.setFrameRate(frameRate);
       * frameRecorder.setVideoBitrate(5000);
       * frameRecorder.setOption("content_type","video/webm");
       * frameRecorder.setOption("listen", "1"); frameRecorder.start(); }
       *
       *
       * FrameRecorder recorder = new FFmpegFrameRecorder("out.mp4",
       * grabber.getImageWidth(), grabber.getImageHeight());
       * recorder.setFormat(grabber.getFormat());
       * recorder.setPixelFormat(AV_PIX_FMT_YUV420P);
       * recorder.setFrameRate(grabber.getFrameRate());
       * recorder.setVideoBitrate(grabber.getVideoBitrate());
       * recorder.setVideoCodec(grabber.getVideoCodec());
       * recorder.setVideoOption("preset", "ultrafast");
       * recorder.setVideoCodecName("libx264");
       * recorder.setVideoCodec(AV_CODEC_ID_H264); recorder.start();
       * 
       */

      boolean done = true;
      if (done) {
        return;
      }

      OpenCVFilterKinectDepth depth = new OpenCVFilterKinectDepth("depth");
      cv.addFilter(depth);
      cv.capture();
      cv.addFilter("Yolo");

    } catch (Exception e) {
      log.error("main threw", e);
    }

  }

  @Override
  public ImageData publishImage(ImageData image) {
    return image;
  }

  @Override
  public void attach(Attachable attachable) {
    if (attachable instanceof ImageListener) {
      attachImageListener(attachable.getName());
    } else {
      error("don't know how to attach a %s", attachable.getName());
    }
  }

}