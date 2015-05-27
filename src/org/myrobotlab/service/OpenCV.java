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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvPoint2D32f;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.FrameGrabber;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.MRLError;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.image.ColoredPoint;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.BlockingQueueGrabber;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.opencv.OpenCVFilterAffine;
import org.myrobotlab.opencv.OpenCVFilterAnd;
import org.myrobotlab.opencv.OpenCVFilterFFMEG;
import org.myrobotlab.opencv.OpenCVFilterFaceDetect;
import org.myrobotlab.opencv.OpenCVFilterFaceRecognition;
import org.myrobotlab.opencv.VideoProcessor;
import org.myrobotlab.reflection.Reflector;
import org.myrobotlab.service.data.Point2Df;
import org.myrobotlab.service.interfaces.VideoSource;
import org.slf4j.Logger;

/*import static org.bytedeco.javacpp.opencv_flann.*;
 import static org.bytedeco.javacpp.opencv_highgui.*;
 import static org.bytedeco.javacpp.opencv_imgproc.*;
 import static org.bytedeco.javacpp.opencv_legacy.*;
 import static org.bytedeco.javacpp.opencv_ml.*;
 import static org.bytedeco.javacpp.opencv_nonfree.*;
 import static org.bytedeco.javacpp.opencv_objdetect.*;
 import static org.bytedeco.javacpp.opencv_photo.*;
 import static org.bytedeco.javacpp.opencv_stitching.*;
 import static org.bytedeco.javacpp.opencv_video.*;
 import static org.bytedeco.javacpp.opencv_videostab.*; */

public class OpenCV extends VideoSource {

	// FIXME - don't return BufferedImage return SerializableImage always !

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCV.class);

	// FIXME - make more simple
	transient public final static String INPUT_SOURCE_CAMERA = "camera";
	transient public final static String INPUT_SOURCE_MOVIE_FILE = "file";
	transient public final static String INPUT_SOURCE_NETWORK = "network";
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

	public static String VALID_FILTERS[] = { "Affine", "And", "AverageColor", "Canny", "CreateHistogram", "ColorTrack", "Detector", "Dilate", "Erode", "FGBG",
		    "FaceDetect", "FaceRecognition","Fauvist","FindContours", "Flip", "FloodFill", "FloorFinder", "GoodFeaturesToTrack", "Gray", "HoughLines2", "HSV",
		    "InRange", "KinectDepth", "KinectDepthMask", "KinectInterleave", "LKOpticalTrack", "Mask", "MatchTemplate", "MotionTemplate", "Mouse", "Not",
		    "PyramidDown", "PyramidUp", "RepetitiveAnd", "RepetitiveOr", "ResetImageROI", "SampleArray", "SampleImage", "SetImageROI", "SimpleBlobDetector",
		    "Smooth", "Split", "SURF", "Threshold", "Transpose" };

	// yep its public - cause a whole lotta data
	// will get set on it before a setState
	
	transient public VideoProcessor videoProcessor = new VideoProcessor();

	// mask for each named filter
	transient public HashMap<String, IplImage> masks = new HashMap<String, IplImage>();

	// DEPRECATED - use getOpenCVData() instead of lastDisplay
	// public OpenCVData lastDisplay;

	// P - N Learning TODO - remove - implement on "images"
	public ArrayList<SerializableImage> positive = new ArrayList<SerializableImage>();
	public ArrayList<SerializableImage> negative = new ArrayList<SerializableImage>();

	public boolean undockDisplay = false;

	public OpenCV(String n) {
		super(n);
		// load(); // FIXME - go into service frame work .. after construction
		// ..
		// somewhere ...
		videoProcessor.setOpencv(this);
	}

	@Override
	public void stopService() {
		if (videoProcessor != null) {
			videoProcessor.stop();
		}
		super.stopService();
	}

	public final boolean publishDisplay(Boolean b) {
		videoProcessor.publishDisplay = b;
		return b;
	}

	/**
	 * FIXME - input needs to be OpenCVData THIS IS NOT USED ! VideoProcessor
	 * NOW DOES OpenCVData - this will return NULL REMOVE !!
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
	 * @param data
	 * @return
	 */
	public final OpenCVData publishOpenCVData(OpenCVData data) {
		return data;
	}

	// the big switch <input>
	public void publishOpenCVData(boolean b) {
		videoProcessor.publishOpenCVData = b;
	}

	public Integer setCameraIndex(Integer index) {
		videoProcessor.cameraIndex = index;
		return index;
	}

	public String setInputFileName(String inputFile) {
		videoProcessor.inputFile = inputFile;
		return inputFile;
	}

	public String setInputSource(String inputSource) {
		videoProcessor.inputSource = inputSource;
		return inputSource;
	}

	public String setFrameGrabberType(String grabberType) {
		videoProcessor.grabberType = grabberType;
		return grabberType;
	}

	public void setDisplayFilter(String name) {
		log.info("pre setDisplayFilter displayFilter{}", videoProcessor.displayFilterName);
		videoProcessor.displayFilterName = name;
		log.info("post setDisplayFilter displayFilter{}", videoProcessor.displayFilterName);
	}

	public OpenCVData add(SerializableImage image) {
		IplImage src = IplImage.createFrom(image.getImage());
		// return new SerializableImage(dst.getBufferedImage(),
		// image.getSource());
		return add(src);
	}

	/**
	 * blocking safe exchange of data between different threads external thread
	 * adds image data which can be retrieved from the blockingData queue
	 * 
	 * @param image
	 */
	public OpenCVData add(IplImage image) {
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
	 * when the video image changes size this function will be called with the
	 * new dimension
	 * 
	 * @param d
	 * @return
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

	public Boolean isTracking(Boolean b) {
		return b;
	}

	// publish functions end ---------------------------

	public void stopCapture() {
		videoProcessor.stop();
	}

	public void capture() {
		save();
		// stopCapture(); // restart?
		videoProcessor.start();
	}

	public void stopRecording(String filename) {
		// cvReleaseVideoWriter(outputFileStreams.get(filename).pointerByReference());
	}

	public void setMask(String name, IplImage mask) {
		masks.put(name, mask);
	}

	public OpenCVFilter addFilter(OpenCVFilter filter) {
		videoProcessor.addFilter(filter);
		broadcastState();
		return filter;
	}

	public OpenCVFilter addFilter(String filterName) {

		OpenCVFilter filter = videoProcessor.addFilter(filterName, filterName);
		broadcastState(); // let everyone know
		return filter;
	}

	public OpenCVFilter addFilter(String name, String filterType) {

		OpenCVFilter filter = videoProcessor.addFilter(name, filterType);
		broadcastState(); // let everyone know
		return filter;
	}

	// FIXME - rename removeFilters
	public void removeFilters() {
		videoProcessor.removeFilters();
		broadcastState();
	}

	public void removeFilter(String name) {
		OpenCVFilter f = videoProcessor.getFilter(name);
		if (f != null) {
			videoProcessor.removeFilter(f);
		} else {
			log.warn("can not remove filter {} - it does not exits", name);
		}
		broadcastState();
	}

	public void removeFilter(OpenCVFilter filter) {
		videoProcessor.removeFilter(filter);
		broadcastState();
	}

	public ArrayList<OpenCVFilter> getFiltersCopy() {
		return videoProcessor.getFiltersCopy();
	}

	public OpenCVFilter getFilter(String name) {
		return videoProcessor.getFilter(name);
	}

	@Override
	public String getDescription() {
		return "OpenCV (computer vision) service wrapping many of the functions and filters of OpenCV. ";
	}

	// filter dynamic data exchange begin ------------------
	/*
	 * wrong place public void broadcastFilterState() {
	 * invoke("publishFilterState"); }
	 */

	/**
	 * @param otherFilter
	 *            - data from remote source
	 * 
	 *            This updates the filter with all the non-transient data in a
	 *            remote copy through a reflective field update. If your filter
	 *            has JNI members or pointer references it will break, mark all
	 *            of these.
	 */
	public void setFilterState(FilterWrapper otherFilter) {

		OpenCVFilter filter = getFilter(otherFilter.name);
		if (filter != null) {
			Service.copyShallowFrom(filter, otherFilter.filter);
		} else {
			error("setFilterState - could not find %s ", otherFilter.name);
		}

	}

	/**
	 * Callback from the GUIService to the appropriate filter funnel through
	 * here
	 */
	public void invokeFilterMethod(String filterName, String method, Object... params) {
		OpenCVFilter filter = getFilter(filterName);
		if (filter != null) {
			Reflector.invokeMethod(filter, method, params);
		} else {
			log.error("invokeFilterMethod " + filterName + " does not exist");
		}
	}

	/**
	 * publishing method for filters - used internally
	 * 
	 * @return FilterWrapper solves the problem of multiple types being resolved
	 *         in the setFilterState(FilterWrapper data) method
	 */
	public FilterWrapper publishFilterState(FilterWrapper filterWrapper) {
		return filterWrapper;
	}

	/**
	 * publishing method for filters - uses string parameter for remote
	 * invocation
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

	public void recordOutput(Boolean b) {
		videoProcessor.recordOutput(b);
	}

	public String recordSingleFrame() {
		// WOOHOO Changed threads & thread safe !
		// OpenCVData d = videoProcessor.getLastData();
		OpenCVData d = getOpenCVData();
		/*
		 * if (d == null) {
		 * log.error("could not record frame last OpenCVData is null"); return
		 * null; }
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
		removeFilter(fd);
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

	/*
	 * public void useBlockingData(Boolean b) { videoProcessor.useBlockingData =
	 * true; }
	 */

	public int getCameraIndex() {
		return videoProcessor.cameraIndex;
	}

	public void setPipeline(String pipeline) {
		videoProcessor.pipelineSelected = pipeline;
		videoProcessor.inputSource = "pipeline";
		videoProcessor.grabberType = "org.myrobotlab.opencv.PipelineFrameGrabber";
	}

	/**
	 * minimum time between processing frames - time unit is in milliseconds
	 * 
	 * @param time
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

	public Status test() {

		Status status = Status.info("starting %s %s test", getName(), getType());

		try {
			// FIXME - each filter should have its own test !!!!
			//

			// smart testing - determine what environment has
			// do i have a camera ?
			// do i have multiple cameras

			// FIXME FIXME FIXME - this needs to work for directories -
			// recursively - us NIO !!!
			// FileIO.copyResource("OpenCV/testData", "OpenCV/testData");

			// FileIO.copyResource("OpenCV/testData/mask.png",
			// "OpenCV/testData/mask.png");
			OpenCV opencv = (OpenCV) Runtime.start(getName(), "OpenCV");
			// OpenCVFilterCanny canny = new OpenCVFilterCanny();

			OpenCVFilterAffine affine = new OpenCVFilterAffine();
			opencv.addFilter(affine);

			/*
			 * OpenCVFilterCanny canny2 = new OpenCVFilterCanny("canny2");
			 * opencv.addFilter(canny2); opencv.capture();
			 */

			String filename = "faces.jpg";
			String testFilename = String.format("OpenCV/testData/%s", filename);
			Runtime.createAndStart("gui", "GUIService");

			// resource !!! - it better be there !
			// opencv.captureFromResourceFile(testFilename);
			opencv.captureFromImageFile(testFilename);

			OpenCVFilterFaceDetect fd = new OpenCVFilterFaceDetect("fd");
			addFilter(fd);
			String output = recordSingleFrame();
			log.info(String.format("record single frame - %s", output));

			OpenCVData d = getOpenCVData();
			ArrayList<org.myrobotlab.service.data.Rectangle> l = d.getBoundingBoxArray();
			log.info("boundingBox size {}", d.getBoundingBoxArray().size());

			SerializableImage img = getDisplay();
			if (img == null) {
				throw new MRLError("getDisplay is null");
			}

			output = recordSingleFrame();
			log.info(String.format("record single frame - %s", output));
			// TODO verify file exists ...

			log.info("here");

			/*
			 * // base set would be file
			 * FileIO.copyResource(String.format("OpenCV/testData/%s",filename),
			 * filename);
			 * 
			 * // headless section ???
			 * setFrameGrabberType("ImageFileFrameGrabber");
			 * setInputSource(INPUT_SOURCE_IMAGE_FILE);
			 * setInputFileName(filename);
			 * 
			 * capture(); captureFromImageFile("shapes.png");
			 * captureFromResourceFile("");
			 */

			// int videoCameraIndex = 0;
			/*
			 * if (data.length > 0){ if (data[0] instanceof Integer){
			 * videoCameraIndex = (Integer)data[0]; } }
			 */

			// capture();

			// should probably use xml file
			// but currently

			// set frame grabber

			// extract test data
			// FileIO.getPackageContent(packageName)

			// add cumulatively

			// DUMP OUT PRETTY REPORT WITH ALL FILTERS !!
			// POST PRETTY REPORT WITH ALL PICTURES !! (test from Orbous -
			// 12/17/2020 results)

			OpenCVData data = getOpenCVData();
			// no filters - just input - all these should NOT BE NULL
			// and equal
			IplImage image = data.getImage();
			if (image == null) {
				throw new MRLError("image null");
			}
			IplImage display = data.getDisplay();
			if (display == null) {
				throw new MRLError("display null");
			}
			IplImage input = data.getInputImage();
			if (input == null) {
				throw new MRLError("input null");
			}

			if (image != display || display != input) {
				throw new MRLError("not equal");
			}

			// specific filter tests
			OpenCVFilterAnd and = new OpenCVFilterAnd();
			BufferedImage bimage = ImageIO.read(FileIO.class.getResourceAsStream("/resource/OpenCV/testData/mask.png"));
			and.loadMask(bimage);
			addFilter(and);
			OpenCVData o = getOpenCVData();
			String outfile = o.writeDisplay();
			log.info("wrote masked file to {}", outfile);

			// type of test - test which saves result of web page
			// test combination (combinatorics filter)
			for (int i = 0; i < VALID_FILTERS.length; ++i) {
				String filter = VALID_FILTERS[i];

				addFilter(filter);

				data = getOpenCVData();
				// setDisplayFilter(filter);
				// sleep(300);

				// test forcing time delay - regular image is 333 fps on a file

				// recordSingleFrame();

				// check OpenCV DATA !!!! - get points bars lines - bounding
				// Boxes

				// removeFilter(filter);

				image = data.getImage(filter);
				if (image == null) {
					throw new MRLError("image null");
				}
				if (display == null) {
					throw new MRLError("display null");
				}

				if (!data.getSelectedFilterName().equals(filter)) {
					throw new MRLError("filter name != selected name");
				}

				image = data.getImage();
				if (image == null) {
					throw new MRLError("image null");
				}

				display = data.getDisplay();

				input = data.getInputImage();

				int width = data.getWidth();
				int height = data.getHeight();
				BufferedImage bi = data.getBufferedImage();
				String f = data.writeImage();
				log.info("{}", data.keySet());

				removeFilter(filter);

			}

		} catch (Exception e) {
			return status.addError(e);
		}

		status.addInfo("test completed");
		return status;
	}

	public void captureFromResourceFile(String filename) {
		FileIO.copyResource(filename, filename);
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

	@Override
	public String[] getCategories() {
		return new String[] { "video", "sensor" };
	}

	static public String[] getPossibleFilters() {
		return VALID_FILTERS;
	}

	public static void main(String[] args) throws Exception {

		// TODO - Avoidance / Navigation Service
		// ground plane
		// http://stackoverflow.com/questions/6641055/obstacle-avoidance-with-stereo-vision
		// radio lab - map cells location cells yatta yatta
		// lkoptical disparity motion Time To Contact
		// https://www.google.com/search?aq=0&oq=opencv+obst&gcx=c&sourceid=chrome&ie=UTF-8&q=opencv+obstacle+avoidance
		//
		org.apache.log4j.BasicConfigurator.configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Runtime.start("gui", "GUIService");

		OpenCV opencv = (OpenCV) Runtime.start("opencv", "OpenCV");

		// Face recognition test...
		opencv.setCameraIndex(1);
		OpenCVFilterFaceRecognition rec = new OpenCVFilterFaceRecognition("facerec");
		opencv.addFilter(rec);
		// a file that contains the training dataset
		rec.learn("facerec/all10.txt");
		// end face recognition test.
		
		
		
		//		OpenCVFilterFFMEG ffmpeg = new OpenCVFilterFFMEG("ffmpeg");
		//		opencv.addFilter(ffmpeg);
		//		opencv.capture();
		//		opencv.removeFilters();
		//		ffmpeg.stopRecording();

		// opencv.setCameraIndex(0);

		// opencv.setInputSource("file");
		// opencv.setInputFileName("c:/test.avi");
		
		opencv.capture();
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

		// Runtime.createAndStart("gui", "GUIService");
		// opencv.test();
		/*
		 * Runtime.createAndStart("gui", "GUIService"); RemoteAdapter remote =
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
}
