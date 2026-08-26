package org.myrobotlab.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.geometry.DepthColorMap;
import org.myrobotlab.math.geometry.DepthToPointCloud;
import org.myrobotlab.math.geometry.PointCloud;
import org.myrobotlab.math.geometry.Rectangle;
import org.myrobotlab.math.geometry.SyntheticDepth;
import org.myrobotlab.service.config.OakDConfig;
import org.myrobotlab.service.data.Classification;
import org.myrobotlab.service.data.DepthFrame;
import org.myrobotlab.service.data.DepthHud;
import org.myrobotlab.service.interfaces.DepthFramePublisher;
import org.myrobotlab.service.interfaces.DepthHudPublisher;
import org.myrobotlab.service.interfaces.PointCloudPublisher;
import org.slf4j.Logger;

/**
 * Luxonis OAK-D / OAK-D Lite stereo depth via DepthAI (Python) and Py4j.
 * <p>
 * Live path: {@link #startDepth()} execs {@code resource/OakD/depth_pipeline.py}
 * which calls {@link #onStridedDepth} each frame (depth plus optional RGB
 * JPEG). Simulator overlay: attach a {@link JMonkeyEngine} and subscribe it to
 * {@link #publishPointCloud}, {@link #publishDepthHud}, and
 * {@link #publishDepthFrame}. {@link OakDConfig#rgbMesh} switches voxels vs an
 * RGB-textured mesh.
 * <p>
 * Python deps are installed into the shared Py4j venv ({@code data/Py4j/venv}).
 * DepthAI <strong>2.29.0 has no wheels for CPython 3.13/3.14</strong>; pip then
 * tries to compile and fails. {@link #DEPTHAI_PIP_SPEC} tracks 3.x binary
 * wheels. See {@code resource/OakD/README.md}.
 */
public class OakD extends Service<OakDConfig> implements PointCloudPublisher, DepthHudPublisher, DepthFramePublisher {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OakD.class);

  private static final String SYNTHETIC_TASK = "publishSyntheticFrame";

  /**
   * DepthAI 3.x publishes cp39–cp314 wheels (including Windows). Do not pin
   * {@code depthai==2.29.0} — that sdist has no 3.13+ wheel and pip compiles it.
   */
  public static final String DEPTHAI_PIP_SPEC = "depthai>=3.2.0,<4";

  public static final String NUMPY_PIP_SPEC = "numpy";

  public static final String OPENCV_PIP_SPEC = "opencv-python";

  /** Optional; used only when spatial YOLO blobs are downloaded. */
  public static final String BLOBCONVERTER_PIP_SPEC = "blobconverter==1.4.3";

  private transient Py4j py4j = null;

  public boolean capturing = false;

  public boolean usingSynthetic = false;

  public String lastError;

  private long syntheticFrameId = 0;

  public OakD(String n, String id) {
    super(n, id);
  }

  @Override
  public void startService() {
    super.startService();
    if (config.py4jInstall) {
      py4j = (Py4j) startPeer("py4j");
      installDepthAi();
    }
  }

  @Override
  public void stopService() {
    stopDepth();
    super.stopService();
  }

  @Override
  public void attach(Attachable attachable) throws Exception {
    if (attachable == null) {
      return;
    }
    addListener("publishPointCloud", attachable.getName(), "onPointCloud");
    addListener("publishDepthHud", attachable.getName(), "onDepthHud");
    addListener("publishDepthFrame", attachable.getName(), "onDepthFrame");
    addListener("publishRgbMesh", attachable.getName(), "onRgbMesh");
    addListener("publishClassification", attachable.getName(), "onClassification");
  }

  /**
   * Switch the simulator overlay between depth voxels ({@code false}) and an
   * RGB-textured mesh ({@code true}). WebGui checkbox calls this.
   */
  public void setRgbMesh(boolean enabled) {
    config.rgbMesh = enabled;
    invoke("publishRgbMesh", enabled);
    broadcastState();
  }

  public boolean publishRgbMesh(boolean enabled) {
    return enabled;
  }

  public void publishInstallStart() {
  }

  public Status publishInstallFinish() {
    return Status.error("depth ai install was not successful");
  }

  public void installDepthAi() {
    try {
      if (py4j == null) {
        py4j = (Py4j) startPeer("py4j");
      }
      info("Installing %s into the Py4j venv (binary wheels only; 2.29.0 cannot install on Python 3.13+)",
          DEPTHAI_PIP_SPEC);
      List<String> required = new ArrayList<>();
      required.add(DEPTHAI_PIP_SPEC);
      required.add(NUMPY_PIP_SPEC);
      required.add(OPENCV_PIP_SPEC);
      // Refuse source builds — compiling depthai 2.x is what produced
      // "subprocess returned 1" with no useful pip text on Python 3.14.
      py4j.installPipPackages(required, List.of("--upgrade", "--only-binary=depthai"));
      try {
        List<String> optional = new ArrayList<>();
        optional.add(BLOBCONVERTER_PIP_SPEC);
        py4j.installPipPackages(optional);
      } catch (IOException optionalFailed) {
        warn("Optional OakD pip packages failed (depthai is installed): %s", optionalFailed.getMessage());
      }
    } catch (IOException e) {
      lastError = e.getMessage();
      error(e);
    }
  }

  /**
   * Start stereo depth. Tries the USB OAK-D unless {@code forceSynthetic} is
   * set; falls back to a synthetic cloud when configured.
   */
  public boolean startDepth() {
    stopDepth();
    capturing = true;
    lastError = null;
    invoke("publishRgbMesh", config.rgbMesh);
    if (config.forceSynthetic) {
      startSyntheticDepth();
      return true;
    }
    if (tryStartHardware()) {
      return true;
    }
    if (config.syntheticFallback) {
      warn("OAK-D hardware path failed — publishing synthetic depth for the simulator");
      startSyntheticDepth();
      return true;
    }
    capturing = false;
    return false;
  }

  public void stopDepth() {
    capturing = false;
    usingSynthetic = false;
    purgeTask(SYNTHETIC_TASK);
    if (py4j != null && py4j.isReady()) {
      try {
        py4j.exec("OAKD_STOP = True\nOAKD_RUN_ID = 0");
      } catch (Exception e) {
        log.debug("stopDepth python: {}", e.getMessage());
      }
    }
  }

  public void startSyntheticDepth() {
    usingSynthetic = true;
    capturing = true;
    syntheticFrameId = 0;
    info("OakD synthetic depth at %d fps", Math.max(1, config.fps));
    long interval = Math.max(50, 1000L / Math.max(1, config.fps));
    addTask(SYNTHETIC_TASK, interval, 0, "publishSyntheticFrame");
  }

  public DepthFrame publishSyntheticFrame() {
    if (!capturing) {
      return null;
    }
    DepthFrame frame = SyntheticDepth.planeWithBox(syntheticFrameId++);
    frame.src = getName();
    frame.minDepthMm = config.minDepthMm;
    frame.maxDepthMm = config.maxDepthMm;
    return publishConverted(frame);
  }

  /**
   * Called from DepthAI Python (Py4j lists of numbers).
   */
  public DepthFrame onStridedDepth(int width, int height, int stride, List<?> depthMm, Number fx, Number fy, Number cx,
      Number cy) {
    return onStridedDepth(width, height, stride, depthMm, fx, fy, cx, cy, null);
  }

  /**
   * Same as {@link #onStridedDepth(int, int, int, List, Number, Number, Number, Number)}
   * plus a Base64 JPEG from the color camera (may be empty).
   */
  public DepthFrame onStridedDepth(int width, int height, int stride, List<?> depthMm, Number fx, Number fy, Number cx,
      Number cy, String rgbJpegB64) {
    DepthFrame frame = new DepthFrame();
    frame.width = width;
    frame.height = height;
    frame.stride = Math.max(1, stride);
    frame.fx = fx.floatValue();
    frame.fy = fy.floatValue();
    frame.cx = cx.floatValue();
    frame.cy = cy.floatValue();
    frame.minDepthMm = config.minDepthMm;
    frame.maxDepthMm = config.maxDepthMm;
    frame.src = getName();
    frame.depthMm = toIntArray(depthMm);
    if (rgbJpegB64 != null && !rgbJpegB64.isEmpty()) {
      try {
        frame.rgbJpeg = Base64.getDecoder().decode(rgbJpegB64);
      } catch (IllegalArgumentException e) {
        log.debug("rgb jpeg base64: {}", e.getMessage());
      }
    }
    usingSynthetic = false;
    capturing = true;
    return publishConverted(frame);
  }

  public Classification onDetection(String label, Number confidence, Number xmin, Number ymin, Number xmax, Number ymax,
      Number x, Number y, Number z) {
    Classification c = new Classification();
    c.src = getName();
    c.label = label;
    c.confidence = confidence != null ? confidence.doubleValue() : 0.0;
    float x0 = xmin.floatValue();
    float y0 = ymin.floatValue();
    float x1 = xmax.floatValue();
    float y1 = ymax.floatValue();
    c.bbox = new Rectangle(x0, y0, x1 - x0, y1 - y0);
    if (x != null) {
      c.x = x.doubleValue();
    }
    if (y != null) {
      c.y = y.doubleValue();
    }
    if (z != null) {
      c.z = z.doubleValue();
    }
    return (Classification) invoke("publishClassification", c);
  }

  public void onHardwareStarted() {
    usingSynthetic = false;
    capturing = true;
    lastError = null;
    info("OAK-D depth pipeline connected");
    broadcastState();
  }

  public void onHardwareFailed(String message) {
    lastError = message;
    error("OAK-D: %s", message);
    if (config.syntheticFallback && capturing) {
      startSyntheticDepth();
    }
    broadcastState();
  }

  private DepthFrame publishConverted(DepthFrame frame) {
    if (frame != null) {
      frame.decodeRgb();
    }
    invoke("publishDepthFrame", frame);
    PointCloud pc = DepthToPointCloud.convert(frame);
    invoke("publishPointCloud", pc);
    DepthHud hud = DepthColorMap.toHud(frame);
    if (hud != null) {
      hud.src = getName();
      invoke("publishDepthHud", hud);
    }
    return frame;
  }

  @Override
  public DepthFrame publishDepthFrame(DepthFrame frame) {
    return frame;
  }

  @Override
  public PointCloud publishPointCloud(PointCloud pointCloud) {
    return pointCloud;
  }

  @Override
  public DepthHud publishDepthHud(DepthHud hud) {
    return hud;
  }

  public void startRecognition() {
    config.enableSpatialDetections = true;
    startDepth();
  }

  public void stopRecognition() {
    stopDepth();
  }

  public void processMessage(String method, Object data) {
    String processor = getPeerName("py4j");
    Message msg = Message.createMessage(getName(), processor, method, data);
    invoke("publishProcessMessage", msg);
  }

  public Message publishProcessMessage(Message msg) {
    return msg;
  }

  public Classification publishClassification(Classification classification) {
    if (classification != null) {
      classification.src = getName();
    }
    return classification;
  }

  public String imageToWeb(String filePath) {
    try (FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
      byte[] content = new byte[(int) new File(filePath).length()];
      fileInputStream.read(content);
      return String.format("data:image/png;base64,%s", Base64.getEncoder().encodeToString(content));
    } catch (IOException e) {
      error(e);
    }
    return null;
  }

  private boolean tryStartHardware() {
    try {
      if (py4j == null) {
        py4j = (Py4j) startPeer("py4j");
      }
      if (py4j == null) {
        lastError = "Py4j peer missing";
        return false;
      }
      if (!waitForPy4j(8000)) {
        lastError = "Py4j Python process did not connect";
        error(lastError);
        return false;
      }
      String script = FileIO.resourceToString("OakD/depth_pipeline.py");
      if (script == null || script.isEmpty()) {
        script = FileIO.toString(getResourceDir() + fs + "depth_pipeline.py");
      }
      if (script == null || script.isEmpty()) {
        lastError = "depth_pipeline.py not found";
        error(lastError);
        return false;
      }
      String header = String.format(
          "OAKD_SERVICE = %s\nOAKD_STOP = False\nOAKD_RUN_ID = %d\nOAKD_FPS = %d\nOAKD_STRIDE = %d\nOAKD_MIN_MM = %d\nOAKD_MAX_MM = %d\nOAKD_CONFIDENCE = %s\nOAKD_SPATIAL = %s\nOAKD_BLOB = %s\n",
          pyQuote(getName()), System.currentTimeMillis(), Math.max(1, config.fps), Math.max(1, config.cloudStride),
          config.minDepthMm, config.maxDepthMm, Float.toString(config.confidence),
          config.enableSpatialDetections ? "True" : "False", pyQuote(config.blobPath != null ? config.blobPath : ""));
      py4j.exec(header + "\n" + script);
      info("OAK-D depth pipeline requested on %s", getName());
      return true;
    } catch (Exception e) {
      lastError = e.getMessage();
      error(e);
      return false;
    }
  }

  private boolean waitForPy4j(long timeoutMs) {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      if (py4j != null && py4j.isReady()) {
        return true;
      }
      sleep(200);
    }
    return py4j != null && py4j.isReady();
  }

  private static String pyQuote(String s) {
    if (s == null) {
      return "''";
    }
    return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
  }

  static int[] toIntArray(List<?> list) {
    if (list == null) {
      return new int[0];
    }
    int[] a = new int[list.size()];
    for (int i = 0; i < a.length; i++) {
      Object v = list.get(i);
      if (v instanceof Number) {
        a[i] = ((Number) v).intValue();
      }
    }
    return a;
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);
      Runtime.start("oakd", "OakD");
      Runtime.start("webgui", "WebGui");
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
