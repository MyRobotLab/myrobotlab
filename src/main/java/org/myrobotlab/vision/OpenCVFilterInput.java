package org.myrobotlab.vision;


import java.lang.reflect.Constructor;
import java.util.Queue;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.Vision;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

/**
 * used as a filter which can 'produce' images via camera, file, url reference,
 * jpg stream or other ...
 * 
 * @author GroG
 *
 */
public class OpenCVFilterInput extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterInput.class);

	String inputFile = "http://localhost/videostream.cgi";

	// input
	String inputSource = Vision.INPUT_SOURCE_CAMERA;
	int minDelay = 0;
	String grabberType = getDefaultFrameGrabberType();
	transient FrameGrabber grabber = null;
	String pipelineSelected = "";

	int cameraIndex = 0;

	boolean grabberStarted = false;
	String format = null;

	transient Frame frame;
	transient OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

	public static String getDefaultFrameGrabberType() {
		Platform platform = Runtime.getInstance().getPlatform();
		if (platform.isWindows()) {
			return "org.bytedeco.javacv.VideoInputFrameGrabber";
		} else {
			return "org.bytedeco.javacv.OpenCVFrameGrabber";
		}
	}

	public OpenCVFilterInput() {
		super();
	}

	public OpenCVFilterInput(String name) {
		super(name);
	}

	@Override
	public void imageChanged(IplImage image) {
	}

	@Override // null is expected for the input of an input filter ..
	public IplImage process(IplImage image, VisionData data) {
		try {
			// FIXME - how should a non-null image be processed ?

			// FIXME - init once... unless retry
			if (!grabberStarted) {
				startGrabber();
			}

			++frameIndex;
			if (Logging.performanceTiming)
				Logging.logTime("start");

			frame = grabber.grab();

			if (Logging.performanceTiming)
				Logging.logTime(String.format("post-grab %d", frameIndex));

			// log.info(String.format("frame %d", frameIndex));

			if (minDelay > 0) {
				Service.sleep(minDelay);
			}

			if (frame == null) {
				warn("frame is null");
				return null;
			}

			// set the source key of the big map of all sources to
			// reference our new frame - the key is {serviceName}.input
			// data.put(INPUT_KEY, converter.convert(frame)); - already done

			/*
			 * if (getDepth && grabber.getClass() ==
			 * OpenKinectFrameGrabber.class) { sources.put(boundServiceName,
			 * SOURCE_KINECT_DEPTH, ((OpenKinectFrameGrabber)
			 * grabber).grabDepth()); }
			 */

			image = converter.convert(frame);
			return image;
		} catch (Exception e) {
			error("could not initialize grabber");
			log.error("initInput threw", e);
		}
		return null;
	}

	public FrameGrabber getGrabber() {
		return grabber;
	}

	public String getGrabberType() {
		return grabberType;
	}

	public String getInputFile() {
		return inputFile;
	}

	/**
	 * blocking safe exchange of data between different threads external thread
	 * adds image data which can be retrieved from the blockingData queue
	 * 
	 * @param image - a null value, but could support adding an image
	 */
	public VisionData add(Frame image) {
		FrameGrabber grabber = getGrabber();
		if (grabber == null || grabber.getClass() != BlockingQueueGrabber.class) {
			processor.error(
					"can't add an image to the video processor - grabber must be not null and BlockingQueueGrabber");
			return null;
		}

		BlockingQueueGrabber bqgrabber = (BlockingQueueGrabber) grabber;
		bqgrabber.add(image);

		/*
		try {
			OpenCVData ret = (OpenCVData) blockingData.take();
			return ret;
		} catch (InterruptedException e) {
			return null;
		}
		*/
		return null;
	}

	/**
	 * sleep without the throw
	 * 
	 * @param millis
	 */
	public static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}

	public VisionData add(SerializableImage image) {
		Frame src = OpenCVUtils.BufferedImageToFrame(image.getImage());
		// IplImage src = IplImage.createFrom(image.getImage());
		// return new SerializableImage(dst.getBufferedImage(),
		// image.getSource());
		return add(src);
	}

	public void startGrabber() {
		/*
		 * TODO - check out opengl stuff if (useCanvasFrame) { cf = new
		 * CanvasFrame("CanvasFrame"); }
		 */

		try {

			// inputSource = INPUT_SOURCE_IMAGE_FILE;
			log.info(String.format("video source is %s", inputSource));

			Class<?>[] paramTypes = new Class[1];
			Object[] params = new Object[1];

			// TODO - determine by file type - what input it is

			if (Vision.INPUT_SOURCE_CAMERA.equals(inputSource)) {
				paramTypes[0] = Integer.TYPE;
				params[0] = cameraIndex;
			} else if (Vision.INPUT_SOURCE_MOVIE_FILE.equals(inputSource)) {
				paramTypes[0] = String.class;
				params[0] = inputFile;
			} else if (Vision.INPUT_SOURCE_IMAGE_FILE.equals(inputSource)) {
				paramTypes[0] = String.class;
				params[0] = inputFile;
			} else if (Vision.INPUT_SOURCE_IMAGE_DIRECTORY.equals(inputSource)) {
				paramTypes[0] = String.class;
				params[0] = inputFile;
			} else if (Vision.INPUT_SOURCE_PIPELINE.equals(inputSource)) {
				paramTypes[0] = String.class;
				params[0] = pipelineSelected;
			} else if (Vision.INPUT_SOURCE_NETWORK.equals(inputSource)) {
				paramTypes[0] = String.class;
				params[0] = inputFile;
			}

			log.info(String.format("attempting to get frame grabber %s format %s", grabberType, format));
			Class<?> nfg = Class.forName(grabberType);
			// TODO - get correct constructor for Capture Configuration..
			Constructor<?> c = nfg.getConstructor(paramTypes);

			grabber = (FrameGrabber) c.newInstance(params);

			if (format != null) {
				grabber.setFormat(format);
			}

			log.info(String.format("using %s", grabber.getClass().getCanonicalName()));

			if (grabber == null) {
				log.error(String.format("no viable capture or frame grabber with input %s", grabberType));
				processor.stopVideoProcessing();
			}

			if (grabber != null) {
				grabber.start();
			}

			grabberStarted = true;
			
			log.info("wating 300 ms for camera to warm up");
			sleep(300);

		} catch (Exception e) {
			processor.error("problem creating frame grabber");
			log.error("frame grabber threw", e);
			grabberStarted = false;
			processor.stopVideoProcessing();
		}
		// TODO - utilize the size changing capabilites of the different
		// grabbers
		// grabbler.setImageWidth()
		// grabber.setImageHeight(320);
		// grabber.setImageHeight(240);

		log.info("..goodtimes... beginning capture");

		// keys
		// String inputKey = String.format("%s.%s", boundServiceName,
		// INPUT_KEY);
		// String displayKey = String.format("%s.%s.%s", boundServiceName,
		// INPUT_KEY, OpenCVData.KEY_DISPLAY);

		// String inputFilterName = INPUT_KEY;

	}

	public void stopGrabber() {
		try {
			if (grabber != null) {
				grabber.stop();
				grabber.release();
			}
		} catch (Exception e) {
			log.error("stopGrabber threw", e);
			error("could not stop frame grabber");
		}
	}

	public int getCameraIndex() {
		return cameraIndex;
	}
	
	public void setFrameGrabberType(String grabberType) {
		this.grabberType = grabberType;
	}

	public void setInputFileName(String inputFile) {
		this.inputFile = inputFile;
	}

	public void setInputSource(String inputSource) {
		this.inputSource = inputSource;
	}

	public String getInputSource() {
		return inputSource;
	}

	public String getPipelineSelected() {
		return pipelineSelected;
	}

	public void setMinDelay(int time) {
		this.minDelay = time;
	}

	public void setPipeline(String pipeline) {
		this.pipelineSelected = pipeline;
		this.inputSource = "pipeline";
		this.grabberType = "org.myrobotlab.opencv.PipelineFrameGrabber";
	}

	public void setCameraIndex(Integer index) {
		this.cameraIndex = index;
	}

}
