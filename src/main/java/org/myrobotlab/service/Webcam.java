package org.myrobotlab.service;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.WebImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.geometry.Point;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.string.StringUtil;
import org.slf4j.Logger;

import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamStreamer;
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;

public class Webcam extends Service<ServiceConfig> implements WebcamListener {

  protected class VideoProcessor implements Runnable {

    @Override
    public void run() {
      try {
        capturing = true;
        broadcastState();
        while (capturing) {
          processVideo();
        }
      } catch (Exception e) {
        error("process video threw %s", e.getMessage());
      }
      webcam.close();
      webcam = null;
      worker = null;
      capturing = false;
      broadcastState();
    }
  }

  public final static Logger log = LoggerFactory.getLogger(Webcam.class);

  private static final long serialVersionUID = 1L;

  protected String selectedCamera = null;

  protected boolean capturing = false;

  transient Dimension dimension = new Dimension(640, 480);

  protected int fps = 0;

  protected int requestedFps = 30;

  protected int frameIndex = 0;

  protected String type = "jpg";

  protected Integer height = 480;

  long startSampleTs = 0;

  int lastFrameIndex = 0;

  protected Integer port = 8080;

  protected double quality = 0.9;

  transient private WebcamStreamer streamer;

  protected boolean useWebcamStreamer = false;

  transient com.github.sarxos.webcam.Webcam webcam;

  protected List<String> webcams = null;

  protected Integer width = 640;

  transient Thread worker = null;

  public Webcam(String n, String id) {
    super(n, id);
    Platform platform = Platform.getLocalInstance();
    if (platform.isArm() && platform.isLinux()) {
      com.github.sarxos.webcam.Webcam.setDriver(new V4l4jDriver());
    }
    getWebcams();
  }

  public void capture() {
    capture(null, null, null, null, null, null);
  }

  public synchronized void capture(int index, String type, Integer fps, Integer width, Integer height, Double quality) {
    if (webcam != null) {
      stopCapture();
    }

    if (fps != null) {
      this.requestedFps = fps;
    }

    if (width != null) {
      this.width = width;
    }

    if (type != null) {
      this.type = type;
    }

    if (height != null) {
      this.height = height;
    }

    if (quality != null) {
      this.quality = quality;
    }

    List<com.github.sarxos.webcam.Webcam> cams = com.github.sarxos.webcam.Webcam.getWebcams();
    if (index >= cams.size()) {
      error("camera index requested %d but only %d cameras", index, cams.size());
      return;
    }

    // webcam = com.github.sarxos.webcam.Webcam.getWebcamByName(name);
    webcam = cams.get(index);

    if (webcam == null) {
      webcam = com.github.sarxos.webcam.Webcam.getDefault();
      info("starting webcam on port %d", port);
    }
    selectedCamera = webcam.getName();

    webcam.setViewSize(new Dimension(this.width, this.height));

    if (useWebcamStreamer && streamer == null) {
      info("starting webstreamer on port %d", port);
      streamer = new WebcamStreamer(this.port, webcam, this.requestedFps, true);
    }

    // webcam.addWebcamListener(this);

    webcam.open();

    worker = new Thread(new VideoProcessor(), String.format("%s-video-processor", getName()));
    worker.start();
  }

  public void capture(String name, String type, Integer fps, Integer width, Integer height, Double quality) {
    if (name == null) {
      capture(0, type, fps, width, height, quality);
    } else {
      List<com.github.sarxos.webcam.Webcam> cams = com.github.sarxos.webcam.Webcam.getWebcams();
      for (int i = 0; i < cams.size(); ++i) {
        com.github.sarxos.webcam.Webcam cam = cams.get(i);
        if (name.equals(cam.getName())) {
          capture(i, type, fps, width, height, quality);
          return;
        }
      }
      error("could not find camera %s, camera names are", name, StringUtil.toString(getWebcams()));
    }
  }

  public List<String> getWebcams() {
    List<com.github.sarxos.webcam.Webcam> cams = com.github.sarxos.webcam.Webcam.getWebcams();
    List<String> names = new ArrayList<>();
    info("found %d webcams", cams.size());
    for (com.github.sarxos.webcam.Webcam cam : cams) {
      info("%s - %f fps", cam.getName(), cam.getFPS());
      names.add(cam.getName());
    }
    webcams = names;
    broadcastState();
    return names;
  }

  protected void processVideo() {

    BufferedImage bi = webcam.getImage();
    ++frameIndex;
    // TODO change jpg quality !!! B&W option ?
    // SerializableImage image = new SerializableImage(bi, getName());
    WebImage webImage = new WebImage(bi, getName(), frameIndex, type, quality);

    // non queue broadcast - blocking direct call for this thread
    // which is what we want
    broadcast("publishWebDisplay", webImage);

    long now = System.currentTimeMillis();
    if (now - startSampleTs > 1000) {
      fps = Math.round((frameIndex - lastFrameIndex) / (now - startSampleTs));
      log.info("process {} frames in {} ms", frameIndex - lastFrameIndex, now - startSampleTs);
      lastFrameIndex = frameIndex;
      startSampleTs = System.currentTimeMillis();
    }
  }

  public Point publishSamplePoint(int x, int y) {
    return new Point(x, y);
  }

  public WebImage publishWebDisplay(WebImage img) {
    return img;
  }

  public void samplePoint(int x, int y) {
    invoke("publishSamplePoint", x, y);
  }

  public synchronized void stopCapture() {
    capturing = false;
  }

  @Override
  public void webcamClosed(WebcamEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void webcamDisposed(WebcamEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void webcamImageObtained(WebcamEvent arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void webcamOpen(WebcamEvent arg0) {
    // TODO Auto-generated method stub

  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {
      /*
       * long t1 = 0; long t2 = 0;
       * 
       * int p = 10; int r = 5;
       * 
       * com.github.sarxos.webcam.Webcam webcam =
       * com.github.sarxos.webcam.Webcam.getDefault();
       * 
       * for (int k = 0; k < p; k++) {
       * 
       * webcam.open(); webcam.getImage();
       * 
       * t1 = System.currentTimeMillis(); for (int i = 0; ++i <= r;
       * webcam.getImage()) { } t2 = System.currentTimeMillis();
       * 
       * System.out.println("FPS " + k + ": " + (1000 * r / (t2 - t1 + 1)));
       * 
       * webcam.close(); }
       */

      Webcam webcamx = (Webcam) Runtime.start("webcam", "Webcam");
      // webcam.start("UVC Camera (046d:0807) /dev/video2", 8080, 640, 480);
      // webcamx.start("Integrated Camera: Integrated C /dev/video0", 8080, 640,
      // 480);
      // default driver load names like "Integrated Camera: Integrated C
      // /dev/video0"
      // V4l4jDriver names drivers like /dev/video
      webcamx.capture("/dev/video0", "jpg", 30, 640, 480, 0.5);

      // webcamx.start();
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();
      //
      // Runtime.start("webgui", "WebGui");
      // webcam.startStreamServer("0.0.0.0", 22222);
      // Runtime.start("webgui", "WebGui");
      // webcam.startStreamClient("127.0.0.1", 22222);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
