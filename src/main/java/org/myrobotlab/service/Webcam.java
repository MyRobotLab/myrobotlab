package org.myrobotlab.service;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.image.WebImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.string.StringUtil;
import org.slf4j.Logger;

import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamStreamer;
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;

public class Webcam extends Service implements WebcamListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Webcam.class);

  transient com.github.sarxos.webcam.Webcam webcam;
  transient Dimension dimension = new Dimension(640, 480);

  transient private WebcamStreamer streamer;

  protected String cameraName = null;

  protected List<String> webcams = null;

  protected Integer width = 640;

  protected Integer height = 480;

  protected Integer port = 8080;

  protected int fps = 30;

  protected int frameIndex = 0;

  protected boolean capturing = false;

  protected boolean useWebcamStreamer = false;

  protected double quality = 0.9;

  transient Thread worker = null;

  static {
    com.github.sarxos.webcam.Webcam.setDriver(new V4l4jDriver());
  }

  public WebImage publishWebDisplay(WebImage img) {
    return img;
  }

  public Webcam(String n, String id) {
    super(n, id);
    getWebcams();
  }

  public synchronized void stopCapture() {
    capturing = false;
  }

  public void capture() {
    capture(null, null, null, null, null, null);
  }

  public List<String> getWebcams() {
    List<com.github.sarxos.webcam.Webcam> webcams = com.github.sarxos.webcam.Webcam.getWebcams();
    List<String> names = new ArrayList<>();
    info("found %d webcams", webcams.size());
    for (com.github.sarxos.webcam.Webcam cam : webcams) {
      info("%s - %f fps", cam.getName(), cam.getFPS());
      names.add(cam.getName());
    }
    return names;
  }

  public void capture(String name, Integer port, Integer fps, Integer width, Integer height, Double quality) {
    if (name == null) {
      capture(0, port, fps, width, height, quality);
    } else {
      List<com.github.sarxos.webcam.Webcam> cams = com.github.sarxos.webcam.Webcam.getWebcams();
      for (int i = 0; i < cams.size(); ++i) {
        com.github.sarxos.webcam.Webcam cam = cams.get(i);
        if (name.equals(cam.getName())) {
          capture(i, port, fps, width, height, quality);
        }
      }
      error("could not find camera %s, camera names are", name, StringUtil.toString(getWebcams()));
    }
  }

  public synchronized void capture(int index, Integer port, Integer fps, Integer width, Integer height, Double quality) {
    if (webcam != null) {
      stopCapture();
    }

    if (fps != null) {
      this.fps = 30;
    }

    if (port != null) {
      this.port = port;
    }

    if (width != null) {
      this.width = width;
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
    cameraName = webcam.getName();

    if (webcam == null) {
      webcam = com.github.sarxos.webcam.Webcam.getDefault();
      cameraName = webcam.getName();
      info("starting webcam on port %d", port);
    }

    webcam.setViewSize(new Dimension(this.width, this.height));

    if (useWebcamStreamer && streamer == null) {
      info("starting webstreamer on port %d", port);
      streamer = new WebcamStreamer(this.port, webcam, this.fps, true);
    }

    // webcam.addWebcamListener(this);

    webcam.open();

    worker = new Thread(new VideoProcessor(), String.format("%s-video-processor", getName()));
    worker.start();
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

  protected void processVideo() {
    long start = System.currentTimeMillis();

    BufferedImage bi = webcam.getImage();
    ++frameIndex;
    // TODO change jpg quality !!! B&W option ?
    // SerializableImage image = new SerializableImage(bi, getName());
    WebImage webImage = new WebImage(bi, getName(), frameIndex, "jpg", quality);

    // broadcast - blocking direct call for this thread
    // which is what we want
    broadcast("publishWebDisplay", webImage);

    if (frameIndex % 30 == 0) {
      log.info("process time {} ms", System.currentTimeMillis() - start);
    }
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
      webcamx.capture("/dev/video0", 8080, 30, 640, 480, 0.5);

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
      Logging.logError(e);
    }
  }

}
