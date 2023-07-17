package org.myrobotlab.service;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;

import org.myrobotlab.cv.ComputerVision;
import org.myrobotlab.cv.CvFilter;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.WebImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

//import boofcv.abst.video.VideoDisplay;
//import boofcv.abst.video.VideoDisplayProcessing;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

public class BoofCV extends Service
    implements ComputerVision /* Point2DfPublisher, Point2DfListener */ {

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

  protected boolean capturing = false;

  transient BufferedImage bimage = null;

  // FIXME - put in config
  String cameraDevice = null;

  transient LinkedHashMap<String, CvFilter> filters = new LinkedHashMap<>();

  /**
   * video stream frame index
   */
  protected int frameIndex = 0;

  transient private ImagePanel gui = null;

  protected String inputSource = "camera";

  protected boolean loop = true;

  transient MediaManager media = DefaultMediaManager.INSTANCE;

  boolean nativeViewer = true;

  protected long ts = 0;

  transient SimpleImageSequence<?> video = null;

  // @Override
  // public Point2df publishPoint2Df(Point2df point) {
  // return point;
  // }
  //
  // @Override
  // public Point2df onPoint2Df(Point2df point) {
  // // System.out.println("Receinvig");
  // return point;
  // }

  transient protected com.github.sarxos.webcam.Webcam webcam = null;

  boolean webViewer = true;

  final VideoProcessor worker = new VideoProcessor();

  public BoofCV(String n, String id) {
    super(n, id);
  }

  @Override
  public CvFilter addFilter(String name, String filterType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void capture() {
    worker.start();
    capturing = true;
    broadcastState();
  }

  @Override
  public void disableAll() {
    // TODO Auto-generated method stub

  }

  @Override
  public void disableFilter(String name) {
    // TODO Auto-generated method stub

  }

  @Override
  public void enableFilter(String name) {
    // TODO Auto-generated method stub

  }

  public List<String> getCameras() {
    // Webcam.getList
    return null;
  }

  private ImageBase<?> getFrame() {

    ImageBase<?> image = null;

    // FIXME - second param image type info
    if (video != null && !video.hasNext() && loop) {
      video.close();
      video = null;
    }

    if (video == null) {
      ImageType<?> imageType = ImageType.pl(3, GrayU8.class);
      // video = media.openVideo("wildcat_robot.mjpeg", ImageType.PL_U8);
      // video = media.openVideo("wildcat_robot.mjpeg",
      // ImageType.single(GrayU8.class));
      imageType = ImageType.single(GrayF32.class);
      // video = media.openVideo("wildcat_robot.mjpeg", imageType);
      video = media.openVideo("zoom.mjpeg", imageType);

      // VideoDecoder<GrayU8> videoDecoder = media.openVideo(inputFile,
      // imageType);

    }

    image = video.next();

    if (image == null) {
      log.info("here");
    }

    // if("camera".equals(inputSource )) {
    //
    // if (webcam == null) {
    // if (cameraDevice == null) {
    // webcam = UtilWebcamCapture.openDefault(640, 480);
    // } else {
    // webcam = UtilWebcamCapture.openDevice(cameraDevice, 640, 480);
    // }
    // }
    //
    // image = webcam.getImage();
    // }

    return image;
  }

  private void process() {
    // process the video stream

    // get timestamp
    ts = System.currentTimeMillis();

    // get an image boofcv? frame
    ImageBase<?> frame = getFrame();

    // iterate through filters

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
    // TODO Auto-generated method stub

  }

  @Override
  public void removeFilters() {
    // TODO Auto-generated method stub

  }

  @Override
  public Integer setCameraIndex(Integer index) {
    // TODO Auto-generated method stub
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

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("webgui", "WebGui");

      BoofCV boofcv = (BoofCV) Runtime.start("boofcv", "BoofCV");

      boofcv.capture();
      log.info("here");
      // Runtime.start("gui", "SwingGui");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }
}
