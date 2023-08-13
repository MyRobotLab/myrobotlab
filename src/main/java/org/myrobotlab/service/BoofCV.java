package org.myrobotlab.service;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.boofcv.BoofCVFilter;
import org.myrobotlab.boofcv.BoofCVFilterTrackerObjectQuad;
import org.myrobotlab.cv.ComputerVision;
import org.myrobotlab.cv.CVFilter;
import org.myrobotlab.framework.Instantiator;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.WebImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.BoofCVConfig;
import org.slf4j.Logger;

import com.github.sarxos.webcam.Webcam;

//import boofcv.abst.video.VideoDisplay;
//import boofcv.abst.video.VideoDisplayProcessing;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

public class BoofCV extends Service<BoofCVConfig>
    implements ComputerVision /* Point2DfPublisher, Point2DfListener */ {

  // FIXME - reconcile and make a real serializable enum with OpenCV definitions
  public final String INPUT_SOURCE_CAMERA = "camera";
  public final String INPUT_SOURCE_FILE = "imagefile";
  
  public class VideoProcessor implements Runnable {

    volatile boolean running = false;

    transient Thread worker = null;

    @Override
    public void run() {
      capturing = true;
      running = true;
      try {
        while (running) {
          process();
          frameIndex++;
        }
      } catch (Exception e) {
        error(e);
      }
      capturing = false;
      broadcastState();
      worker = null;
    }

    synchronized void start() {
      if (worker == null) {
        worker = new Thread(this, String.format("%s-VideoProcessor", getName()));
        worker.start();
      } else {
        log.info("{} video processor already running", getName());
      }
    }

    synchronized void stop() {
      running = false;
    }

  }

  public final static Logger log = LoggerFactory.getLogger(BoofCV.class);

  private static final long serialVersionUID = 1L;

  /**
   * capturing state
   */
  protected boolean capturing = false;

  /**
   * last buffered image to be processed
   */
  transient BufferedImage bimage = null;

  /**
   * list of cameras
   */
  List<String> cameras = new ArrayList<>();

  // FIXME - put in config
  String cameraDevice = null;

  /**
   * list of named filters to process the video stream
   */
  transient Map<String, BoofCVFilter> filters = new LinkedHashMap<>();

  /**
   * video stream frame index
   */
  protected int frameIndex = 0;

  /**
   * native swing display
   */
  transient private ImagePanel gui = null;

  protected String inputSource = "camera";

  protected boolean loop = true;

  transient MediaManager media = DefaultMediaManager.INSTANCE;

  boolean nativeViewer = true;

  protected long ts = 0;

  transient SimpleImageSequence<?> video = null;

  transient protected com.github.sarxos.webcam.Webcam webcam = null;

  boolean webViewer = true;

  final VideoProcessor worker = new VideoProcessor();
  private ImageBase<?> lastFrame;
  private BufferedImage lastImage;

  public BoofCV(String n, String id) {
    super(n, id);
  }

  @Override
  public CVFilter addFilter(String name, String filterType) {
    String type = String.format("org.myrobotlab.boofcv.BoofCVFilter%s", filterType);
    BoofCVFilter filter = (BoofCVFilter) Instantiator.getNewInstance(type, name);
    if (filter == null) {
      error("cannot create filter %s of type %s", name, type);
      return null;
    }
    addFilter(filter);
    return filter;
  }

  public BoofCVFilter addFilter(BoofCVFilter filter) {
    filter.setBoofCV(this);

    // guard against putting same name filter in
    if (filters.containsKey(filter.getName())) {
      warn("trying to add same named filter - %s - choose a different name", filter);
      return filters.get(filter.getName());
    }

    // heh - protecting against concurrency the way Scala does it ;
    Map<String, BoofCVFilter> newFilters = new LinkedHashMap<>();
    newFilters.putAll(filters);
    // add new filter
    newFilters.put(filter.getName(), filter);
    // switch to new references
    filters = newFilters;
    setDisplayFilter(filter.getName());
    broadcastState();
    return filter;

  }

  @Override
  public void capture() {
    worker.start();
    capturing = true;
    broadcastState();
  }

  @Override
  public void disableAll() {
    for (BoofCVFilter filter : filters.values()) {
      filter.disable();
    }
    broadcastState();
  }

  @Override
  public void disableFilter(String name) {
    BoofCVFilter f = filters.get(name);
    if (f != null && f.isEnabled()) {
      f.disable();
      broadcastState();
    }
  }

  @Override
  public void enableFilter(String name) {
    BoofCVFilter f = filters.get(name);
    if (f != null && !f.isEnabled()) {
      f.enable();
      broadcastState();
    }
  }

  public List<String> getCameras() {
    // Get a list of available camera names
    List<Webcam> webcams = Webcam.getWebcams();
    cameras = new ArrayList<>();
    for (Webcam cam : webcams) {
      cameras.add(cam.getName());
    }
    return cameras;
  }

  private ImageBase<?> getFrame() {

    ImageBase<?> frame = null;

    // shutdown video source if loop and has no next
    // FIXME - second param image type info
    if (video != null && !video.hasNext() && loop) {
      video.close();
      video = null;
    }

    // create a video source if one is specified and null reference
    
      if (config.inputSource == INPUT_SOURCE_FILE) {
        if (video == null) {
          // file sequence needs a video
        ImageType<?> imageType = ImageType.pl(3, GrayU8.class);
        imageType = ImageType.single(GrayF32.class);
        // video = media.openVideo(config.inputFile, imageType);
        video = media.openVideo(config.inputFile, ImageType.single(GrayU8.class));
        }
      } else if (config.inputSource == INPUT_SOURCE_CAMERA) {
        // camera needs a webcam
        // Configure webcam capture
//        UtilWebcamCapture.CaptureInfo info = UtilWebcamCapture.selectSize(null,640,480);
//        UtilWebcamCapture capture = UtilWebcamCapture.create(info);
        if (webcam == null) {
        // webcam = UtilWebcamCapture.openDevice(cameraDevice, 640,480);
          webcam = UtilWebcamCapture.openDefault(640,480);
        }
      }
    
      // return a frame based on video source
      if (config.inputSource == INPUT_SOURCE_FILE) {
        frame = video.next();
        if (frame != null) {
          lastImage = video.getGuiImage();
        }
      } else if (config.inputSource == INPUT_SOURCE_CAMERA) {
        BufferedImage image = webcam.getImage();
        if (image != null) {
          lastImage = image;
        }
        frame = ConvertBufferedImage.convertFrom(image, (GrayU8) null);
      }
      
      if (frame != null) {
        lastFrame = frame;
      }

    return frame;
  }

  private void process() {
    // process the video stream

    // get timestamp
    ts = System.currentTimeMillis();

    // get an image boofcv? frame
    ImageBase<?> frame = getFrame();

    try {
      // iterate through filters
      for (BoofCVFilter filter : filters.values()) {
        frame = filter.process(frame);
      }
    } catch (Exception e) {
      error(e);
    }

    // convert to Buffered Image ?
    bimage = ConvertBufferedImage.convertTo(frame, bimage, true);

    // display the image natively
    if (nativeViewer) {
      if (gui == null) {
        gui = new ImagePanel();
        // gui.setPreferredSize(webcam.getViewSize());
        gui.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));
        // gui.setPreferredSize(bimage.getViewSize());
        ShowImages.showWindow(gui, getName(), true);
      }

      gui.setImageRepaint(bimage);
      sleep(100);

    } else {
      if (gui != null) {
        gui.setVisible(false);
        gui = null;
      }
    }

    // display the image via web
    if (webViewer) {
      WebImage webImage = new WebImage(bimage, getName(), frameIndex);
      webImage.ts = ts;
      // broadcast does not queue the image and operates on the same thread
      broadcast("publishWebDisplay", webImage);
    }

    // publish results
    // publishCvData

  }

  // FIXME put in an interface
  public WebImage publishWebDisplay(WebImage data) {
    return data;
  }

  @Override
  public void removeFilter(String name) {
    if (filters.containsKey(name)) {
      Map<String, BoofCVFilter> newFilters = new LinkedHashMap<>();
      newFilters.putAll(filters);
      BoofCVFilter removed = newFilters.remove(name);
      removed.release();
      filters = newFilters;
      broadcastState();
    }
  }

  @Override
  public void removeFilters() {
    for (BoofCVFilter filter : filters.values()) {
      filter.release();
    }
    filters = new LinkedHashMap<>();
    broadcastState();
  }

  @Override
  public Integer setCameraIndex(Integer index) {
    getCameras();
    if (cameras.size() > index) {
      // Webcam.
      // cameraDevice = UtilWebcamCapture.openDevice(cameraDevice, frameIndex,
      // frameIndex)

    }
    return null;
  }

  @Override
  public void setDisplayFilter(String name) {
    // TODO Auto-generated method stub

  }

  @Override
  public void stopCapture() {
    worker.stop();
    capturing = false;
    broadcastState();
  }
  
  public void capture(String filename) {
    config.inputFile = filename;
    // FIXME make a communal enum shared with opencv (and serializable)
    config.inputSource = INPUT_SOURCE_FILE;        
    capture();
  }
  
  public void capture(int cameraIndex) {
    config.cameraIndex = cameraIndex;
    // FIXME make a communal enum shared with opencv (and serializable)
    config.inputSource = INPUT_SOURCE_CAMERA;
    capture();
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("webgui", "WebGui");

      BoofCV boofcv = (BoofCV) Runtime.start("boofcv", "BoofCV");

      List<String> cameras = boofcv.getCameras();

      BoofCVFilterTrackerObjectQuad tracker = new BoofCVFilterTrackerObjectQuad("tracker");
      tracker.setLocation(211.0, 162.0, 326.0, 153.0, 335.0, 258.0, 215.0, 249.0);
      boofcv.addFilter(tracker);
      // boofcv.addFilter("tracker", "TrackerObjectQuad");

      boofcv.capture(0);
      
      // boofcv.capture("wildcat_robot.mjpeg");
      // boofcv.capture("zoom.mjpeg");

      // boofcv.capture();
      log.info("here");
      // Runtime.start("gui", "SwingGui");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public BufferedImage getGuiImage() {  
    if (lastImage != null) {
      return lastImage;  
    }
    return null;
  }

}
