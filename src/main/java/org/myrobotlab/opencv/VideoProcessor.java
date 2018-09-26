package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvScalar;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.cvInitFont;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameRecorder;
import org.bytedeco.javacv.OpenKinectFrameGrabber;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

/**
 * This is the main video processing pipeline for the OpenCV service.
 * It grabs a frame from the frame grabber and passes it though all opencv filters
 */
public class VideoProcessor implements Runnable, Serializable {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(VideoProcessor.class);

	public int frameIndex = 0;
	private final transient OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();	
	public HashMap<String, Overlay> overlays = new HashMap<String, Overlay>();
	public boolean publishOpenCVData = true;
	private transient CvFont font = new CvFont();
	public StringBuffer frameTitle = new StringBuffer();
	public OpenCVData data;
  private final transient OpenCV opencv;
	// FIXME - more than 1 type is being used on this in more than one context
	// BEWARE !!!!
	// FIXME - use for RECORDING & another one for Blocking for data !!!
	// TODO: understand what these comments above mean...

	transient public BlockingQueue<Object> blockingData = new LinkedBlockingQueue<Object>();
	private transient FrameGrabber grabber = null;
	public transient Thread videoThread = null;
	private transient Map<String, OpenCVFilter> filters = new LinkedHashMap<String, OpenCVFilter>();
	private transient List<OpenCVFilter> addFilterQueue = new ArrayList<OpenCVFilter>();
	private transient List<String> removeFilterQueue = new ArrayList<String>();
	private transient SimpleDateFormat sdf = new SimpleDateFormat();
	private transient HashMap<String, FrameRecorder> outputFileStreams = new HashMap<String, FrameRecorder>();
	public static final String INPUT_KEY = "input";
	public String boundServiceName;

	/**
	 * selected display filter unselected defaults to input
	 */
	public String displayFilterName = INPUT_KEY;
	public transient Frame frame;
	private int minDelay = 0;
	private boolean recordOutput = false;
	private boolean closeOutputs = false;
	public String recordingSource = INPUT_KEY;
	private boolean showFrameNumbers = true;
	private boolean showTimestamp = true;

	/**
	 * Although OpenCVData might be publishing, this determines if a display is
	 * to be published. In addition to this a specific filter name is needed, if
	 * the filter name does not exist - input will be displayed
	 */
	public boolean publishDisplay = true;
	
	/**
	 * the last source key - used to set the next filter's
	 * default source
	 */
	public String lastSourceKey;

	public VideoProcessor(OpenCV opencv) {
	  this.opencv = opencv;
	  this.boundServiceName = opencv.getName();
	  cvInitFont(font, CV_FONT_HERSHEY_PLAIN, 1, 1);
		OpenCVData data = new OpenCVData(boundServiceName, 0);
		lastSourceKey = INPUT_KEY;
		data.put(INPUT_KEY);
	}
	
	/**
	 * to reply to the request of getting all
	 * source keys - pending or current
	 */
	public Set<String> getKeys(){
		Set<String> ret = new LinkedHashSet<String>();
		// get filters
		ret.addAll(filters.keySet());
		// get to be added
		for (int i = 0; i < addFilterQueue.size(); ++i){
			String name = addFilterQueue.get(i).name;
			ret.add(String.format("%s.%s", boundServiceName, name));
		}

		// remove to be removed
		for(int i = 0; i < removeFilterQueue.size(); ++i){
			ret.remove(removeFilterQueue.get(i));
		}
		// send 
		return ret;
	}

	/**
	 * add filter to the addFilterQueue so the video processor thread will pick
	 * it up - this is always doen by an 'external' thread
	 */
	public OpenCVFilter addFilter(OpenCVFilter filter) {
	  filter.setVideoProcessor(this);
		addFilterQueue.add(filter);
		return filter;
	}

	/**
	 * add filter with a string name and a filter type
	 */
	public OpenCVFilter addFilter(String name, String filterType) {
		if (!filters.containsKey(name)) {
			String type = String.format("org.myrobotlab.opencv.OpenCVFilter%s", filterType);
			OpenCVFilter filter = (OpenCVFilter) Instantiator.getNewInstance(type, name);
			addFilterQueue.add(filter);
			filter.setVideoProcessor(this);
			return filter;
		} else {
			return filters.get(name);
		}
	}

	/**
	 * return a particular filter that is in the video processor by it's name
	 * @param name
	 * @return
	 */
	public OpenCVFilter getFilter(String name) {
		if (filters.containsKey(name)) {
			return filters.get(name);
		}
		return null;
	}

	
	public List<OpenCVFilter> getFiltersCopy() {
		return new ArrayList<OpenCVFilter>(filters.values());
	}

	public FrameGrabber getGrabber() {
		return grabber;
	}

	public OpenCV getOpencv() {
		return opencv;
	}

	/*
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
				FrameRecorder recorder = new OpenCVFrameRecorder(String.format("%s.avi", recordingSource),
						frame.imageWidth, frame.imageHeight);
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

	public void recordOutput(Boolean b) {
		if (b) {
			recordOutput = b;
		} else {
			closeOutputs = true;
		}
	}

	public void removeFilter(String name) {
		removeFilterQueue.add(name);
	}

	/**
	 * you can't call clear() on filters from this function, because it will be
	 * on a different thread and your likely to mess up the video processing
	 * thread - so we load the list of names to remove - and let the video
	 * processing thread remove them all
	 */
	public void removeFilters() {
		List<OpenCVFilter> copy = getFiltersCopy();
		for (int i = 0; i < copy.size(); ++i) {
			OpenCVFilter f = copy.get(i);
			removeFilterQueue.add(f.name);
		}
	}

	private void warn(String msg, Object... params) {
		try {
			opencv.warn(msg, params);
		} catch (Exception e) {
		}
	}

	/**
	 * main video processing loop sources is a globally accessible VideoSources
	 * - but is not threadsafe data is thread safe - at least the references to
	 * the data are threadsafe even if the data might not be (although it
	 * "probably" is :)
	 * 
	 * more importantly the references of data are synced with itself - so that
	 * all references are from the same processing loop
	 */
	@Override
	public void run() {

		try {
		  // start the current grabber
		  grabber.start();
		} catch (Exception e) {
		  opencv.capturing = false;
			Logging.logError(e);
			stop();
		}
		
		log.info("beginning capture");
    opencv.capturing = true;
		
		while (opencv.capturing) {
			try {
				++frameIndex;
				if (Logging.performanceTiming)
					Logging.logTime("start");

				// Grab an actual frame!
				frame = grabber.grab();				

				if (Logging.performanceTiming)
					Logging.logTime(String.format("post-grab %d", frameIndex));

				if (minDelay > 0) {
					Service.sleep(minDelay);
				}

				if (frame == null) {
					warn("frame is null");
					continue;
				}
				
				// TODO - option to accumulate? - e.g. don't new
				data = new OpenCVData(boundServiceName, frameIndex);

				// set the source key of the big map of all sources to
				// reference our new frame - the key is {serviceName}.input
				data.put(INPUT_KEY, converter.convert(frame));
				
				if (grabber.getClass() == OpenKinectFrameGrabber.class) {
          OpenKinectFrameGrabber kinect = (OpenKinectFrameGrabber)grabber;
          data.put(OpenCV.SOURCE_KINECT_DEPTH, kinect.grabDepth());
        }

				if (Logging.performanceTiming)
					Logging.logTime("pre-synchronized-filter");

				if (opencv.capturing) {

					/**
					 * add or remove filters depending on the requests of the
					 * queues
					 */

					// process filter add requests
					if (addFilterQueue.size() > 0) {
						for (int i = 0; i < addFilterQueue.size(); ++i) {
							OpenCVFilter f = addFilterQueue.get(i);
							if (f == null) {
								continue;
							}
							if (f.sourceKey == null) {
								f.sourceKey = lastSourceKey;
								data.put(f.name); 
							}
							filters.put(f.name, f);
							lastSourceKey = f.name;
						}
						addFilterQueue.clear();
						opencv.broadcastState(); // filters have changed
					}

					// process filter remove requests
					if (removeFilterQueue.size() > 0) {
						for (int i = 0; i < removeFilterQueue.size(); ++i) {
							String name = removeFilterQueue.get(i);
							if (name == null) {
								continue;
							}
							if (filters.containsKey(name)) {
								filters.remove(name);
								lastSourceKey=INPUT_KEY;
							}
						}
						removeFilterQueue.clear();
						opencv.broadcastState(); // filters have changed
					}

					// process each filter
					for (String filterName : filters.keySet()) {
						OpenCVFilter filter = filters.get(filterName);
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
						if (image == null) {
							warn(String.format("%s has no image - waiting", filter.sourceKey));
							continue;
						}

						// pre process handles image size & channel changes
						filter.preProcess(frameIndex, image, data);
						if (Logging.performanceTiming)
							Logging.logTime(String.format("preProcess-filter %s", filter.name));

						image = filter.process(image, data);

						if (Logging.performanceTiming)
							Logging.logTime(String.format("process-filter %s", filter.name));

						// process the image - push into source as new output
						// other pipelines will pull it off the from the sources
						data.put(filter.name, image);

						// no display || merge display || fork display
						// currently there is no "display" in sources
						// i've got a user selection to display a particular
						// filter

						if (publishDisplay && displayFilterName != null && displayFilterName.equals(filter.name)) {
							data.setDisplayFilterName(displayFilterName);

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

				} // if (capturing)
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
				if (publishDisplay) {
					SerializableImage display = new SerializableImage(data.getDisplayBufferedImage(),
							data.getDisplayFilterName(), frameIndex);
					opencv.invoke("publishDisplay", display);
				}

				// publish accumulated data
				if (publishOpenCVData) {
					opencv.invoke("publishOpenCVData", data);
				}

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

			} catch (Exception e) {
				Logging.logError(e);
				log.error("stopping capture");
				stop();
			}

			if (Logging.performanceTiming)
				Logging.logTime("finished pass");
		} // while capturing

		try {
			grabber.release();
			grabber = null;
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public void setMinDelay(int minDelay) {
		this.minDelay = minDelay;
	}

	public void putText(int x, int y, String text, int r, int g, int b) {
		CvScalar color = cvScalar(r, g, b, 0);
		CvPoint pos = cvPoint(x, y);
		Overlay overlay = new Overlay(text, pos, color, font);
		overlays.put(String.format("%d.%d", x, y), overlay);
	}

	public void clearText() {
		overlays.clear();
	}

	public void showFrameNumbers(boolean b) {
		showFrameNumbers = b;
	}

	public void showTimestamp(boolean b) {
		showTimestamp = b;
	}

	public void start(FrameGrabber grabber) {
	  this.grabber = grabber;
		log.info("starting capture");
		sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
		sdf.applyPattern("dd MMM yyyy HH:mm:ss z");
		if (videoThread != null) {
			log.info("video processor already started");
			return;
		}
		videoThread = new Thread(this, String.format("%s_videoProcessor", opencv.getName()));
		videoThread.start();
	}

	public void stop() {
		log.debug("stopping capture");
		videoThread = null;
    opencv.capturing = false;
	}
}
