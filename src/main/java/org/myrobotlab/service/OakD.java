package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.geometry.DepthColorMap;
import org.myrobotlab.math.geometry.DepthImageClick;
import org.myrobotlab.math.geometry.DepthToPointCloud;
import org.myrobotlab.math.geometry.PointCloud;
import org.myrobotlab.math.geometry.Rectangle;
import org.myrobotlab.math.geometry.SyntheticDepth;
import org.myrobotlab.service.config.OakDConfig;
import org.myrobotlab.service.data.Classification;
import org.myrobotlab.service.data.DepthClick;
import org.myrobotlab.service.data.DepthFrame;
import org.myrobotlab.service.data.DepthHud;
import org.myrobotlab.service.data.OakDPreview;
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
 * {@link #publishDepthFrame}. {@link #publishWebDisplay} sends a side-by-side
 * RGB JPEG and colorized depth map to the WebGui when
 * {@link OakDConfig#displayWeb} is set and a panel is listening.
 * {@link OakDConfig#rgbMesh} switches voxels vs an
 * RGB-textured mesh. A click on that overlay, or on the WebGui RGB / depth
 * image ({@link #clickImage}), publishes {@code publishClickPoint} to
 * {@link Fabrik#onPoint} and {@link #publishDepthClick} so the panel can mark
 * the pixel. Overlay vertices are
 * camera-frame meters (Z from depth mm/1000) in JME world meters; scale
 * {@code 1} is real-world. {@link #calibrateDepthScale(double)} adjusts
 * {@link org.myrobotlab.service.config.JMonkeyEngineConfig#depthCloudScale}
 * from a tape-measured click.
 * <p>
 * Python deps are installed into the shared Py4j venv ({@code data/Py4j/venv}).
 * DepthAI <strong>2.29.0 has no wheels for CPython 3.13/3.14</strong>; pip then
 * tries to compile and fails. {@link #DEPTHAI_PIP_SPEC} tracks 3.x binary
 * wheels, installed on their own so an OpenCV build cannot roll the install
 * back. On Raspberry Pi OS the Movidius USB rule is installed so the camera
 * is visible to a non-root process. See {@code resource/OakD/README.md}.
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

  /**
   * Luxonis still publishes some wheels only on this index. If the host is
   * unreachable the installer retries against PyPI, which has the aarch64
   * cp313 wheel used by Raspberry Pi OS Trixie 64-bit.
   */
  public static final String LUXONIS_PIP_EXTRA_INDEX = "https://artifacts.luxonis.com/artifactory/luxonis-python-snapshot-local/";

  /** 32-bit Raspberry Pi OS only. aarch64 Trixie uses the PyPI manylinux wheel. */
  public static final String PIWHEELS_INDEX = "https://www.piwheels.org/simple";

  public static final String NUMPY_PIP_SPEC = "numpy";

  public static final String OPENCV_HEADLESS_PIP_SPEC = "opencv-python-headless";

  public static final String OPENCV_PIP_SPEC = "opencv-python";

  /** Makes the OAK USB device (vendor 03e7) readable without running as root. */
  public static final String MOVIDIUS_UDEV_PATH = "/etc/udev/rules.d/80-movidius.rules";

  public static final String MOVIDIUS_UDEV_RULE = "SUBSYSTEM==\"usb\", ATTRS{idVendor}==\"03e7\", MODE=\"0666\"\n";

  /** Optional; used only when spatial YOLO blobs are downloaded. */
  public static final String BLOBCONVERTER_PIP_SPEC = "blobconverter==1.4.3";

  private transient Py4j py4j = null;

  public boolean capturing = false;

  public boolean usingSynthetic = false;

  public String lastError;

  /** Mirror of the simulator overlay scale (1 = real-world meters). */
  public float depthCloudScale = 1f;

  /** Meters from the chest camera to the last overlay click. */
  public float lastClickDistanceM;

  /** Last image or simulator click, including the pixel to mark. */
  public DepthClick lastClick;

  /** Latest frame, for unprojecting a WebGui click. Not part of saved state. */
  private transient volatile DepthFrame lastFrame;

  private long syntheticFrameId = 0;

  public OakD(String n, String id) {
    super(n, id);
  }

  @Override
  public void startService() {
    super.startService();
    if (config.py4jInstall) {
      py4j = (Py4j) startPeer("py4j");
      if (py4j == null) {
        lastError = "Py4j peer missing";
        error(lastError);
      } else {
        String importError = py4j.venvImportError("depthai", "numpy");
        if (importError != null && importError.contains("timed out")) {
          // Reinstalling does not fix a native import that is stuck on USB.
          lastError = importError;
          warn(importError);
        } else if (needsDepthaiInstall(importError)) {
          installDepthAi();
        } else if (importError != null) {
          if (importError.toLowerCase().contains("libusb") && py4j.tryAptInstall("libusb-1.0-0")) {
            importError = py4j.venvImportError("depthai", "numpy");
          }
          if (importError != null) {
            lastError = importError;
            error(importError);
          }
        } else if (!py4j.venvCanImport("cv2")) {
          installOptionalCv();
        }
      }
    }
    ensureMovidiusUdev();
    wireSimulatorCalibration();
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
    if (attachable instanceof JMonkeyEngine) {
      subscribe(attachable.getName(), "publishClickPoint", getName(), "onSimulatorClick");
      subscribe(attachable.getName(), "publishDepthClick", getName(), "onDepthClick");
      depthCloudScale = ((JMonkeyEngine) attachable).getDepthCloudScale();
    }
  }

  /**
   * Last mesh click in the simulator — copies range/scale for the OakD GUI.
   */
  public Point onSimulatorClick(Point point) {
    JMonkeyEngine jme = findSimulator();
    if (jme != null) {
      lastClickDistanceM = jme.lastClickDistanceM;
      depthCloudScale = jme.getDepthCloudScale();
    }
    broadcastState();
    return point;
  }

  public float getDepthCloudScale() {
    JMonkeyEngine jme = findSimulator();
    if (jme != null) {
      depthCloudScale = jme.getDepthCloudScale();
      lastClickDistanceM = jme.lastClickDistanceM;
    }
    return depthCloudScale;
  }

  /**
   * Calibration multiplier on the simulator overlay. {@code 1} is real-world
   * meters (same as IK).
   */
  public float setDepthCloudScale(float scale) {
    JMonkeyEngine jme = findSimulator();
    if (jme == null) {
      error("No JMonkeyEngine running — start the simulator first");
      return depthCloudScale;
    }
    depthCloudScale = jme.setDepthCloudScale(scale);
    lastClickDistanceM = jme.lastClickDistanceM;
    broadcastState();
    return depthCloudScale;
  }

  /**
   * Set overlay scale so the last mesh click is {@code knownDistanceM} from
   * the chest camera (tape measure). Click the overlay first.
   */
  public float calibrateDepthScale(double knownDistanceM) {
    JMonkeyEngine jme = findSimulator();
    if (jme == null) {
      error("No JMonkeyEngine running — start the simulator first");
      return depthCloudScale;
    }
    float applied = jme.calibrateDepthScale(knownDistanceM);
    depthCloudScale = jme.getDepthCloudScale();
    lastClickDistanceM = jme.lastClickDistanceM;
    broadcastState();
    return applied;
  }

  private void wireSimulatorCalibration() {
    JMonkeyEngine jme = findSimulator();
    if (jme == null) {
      return;
    }
    subscribe(jme.getName(), "publishClickPoint", getName(), "onSimulatorClick");
    subscribe(jme.getName(), "publishDepthClick", getName(), "onDepthClick");
    depthCloudScale = jme.getDepthCloudScale();
    lastClickDistanceM = jme.lastClickDistanceM;
  }

  private JMonkeyEngine findSimulator() {
    for (ServiceInterface si : Runtime.getServices()) {
      if (si instanceof JMonkeyEngine) {
        return (JMonkeyEngine) si;
      }
    }
    return null;
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

  /**
   * Turn the WebGui RGB + depth preview on or off. The panel checkbox calls this.
   */
  public void setDisplayWeb(boolean enabled) {
    config.displayWeb = enabled;
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
      if (py4j == null) {
        lastError = "Py4j peer missing";
        error(lastError);
        ensureMovidiusUdev();
        broadcastState();
        return;
      }
      py4j.ensureVenv();
      py4j.upgradePip();
      String machine = py4j.pythonMachine();
      info("Installing %s into the Py4j venv (machine %s, binary wheels only)", DEPTHAI_PIP_SPEC,
          machine == null ? "unknown" : machine);
      if (usePiwheels(machine) && !py4j.venvCanImport("numpy")) {
        try {
          py4j.installPipPackages(List.of(NUMPY_PIP_SPEC),
              List.of("--only-binary=:all:", "--extra-index-url", PIWHEELS_INDEX));
        } catch (IOException numpyFailed) {
          warn("numpy from piwheels failed: %s", numpyFailed.getMessage());
        }
      }
      IOException pipFailed = null;
      try {
        py4j.installPipPackages(requiredPipPackages(), depthaiPipOptions(true));
      } catch (IOException luxonisFailed) {
        warn("DepthAI install via the Luxonis index failed; retrying from PyPI. %s", luxonisFailed.getMessage());
        try {
          py4j.installPipPackages(requiredPipPackages(), depthaiPipOptions(false));
        } catch (IOException pypiFailed) {
          pipFailed = pypiFailed;
        }
      }
      if (pipFailed != null) {
        throw new IOException(pipFailed.getMessage() + depthaiWheelHint(machine), pipFailed);
      }
      if (!py4j.venvCanImport("numpy")) {
        py4j.installPipPackages(List.of(NUMPY_PIP_SPEC), List.of("--only-binary=:all:"));
      }
      installOptionalCv();
      installOptionalBlob();
      if (!py4j.isPythonRunning()) {
        info("Starting the Py4j Python process now that the venv is ready");
        py4j.startPythonProcess();
      }
      String importError = importErrorAfterInstall();
      if (importError != null) {
        lastError = importError;
        error(lastError);
      } else {
        lastError = null;
        info("depthai imports in the Py4j venv");
      }
    } catch (IOException e) {
      lastError = e.getMessage();
      error(e);
    }
    ensureMovidiusUdev();
    broadcastState();
  }

  /** DepthAI only. OpenCV is installed afterwards so a failed cv2 build cannot roll depthai back. */
  public static List<String> requiredPipPackages() {
    List<String> required = new ArrayList<>();
    required.add(DEPTHAI_PIP_SPEC);
    return required;
  }

  public static List<String> depthaiPipOptions(boolean luxonisIndex) {
    List<String> opts = new ArrayList<>();
    opts.add("--upgrade");
    // numpy is a depthai dependency and shares this transaction.
    opts.add("--only-binary=depthai,numpy");
    if (luxonisIndex) {
      opts.add("--extra-index-url");
      opts.add(LUXONIS_PIP_EXTRA_INDEX);
    }
    return opts;
  }

  /**
   * @param machine
   *          {@code platform.machine()}, such as {@code aarch64} or {@code armv7l}
   * @return extra error text when DepthAI 3 has no wheel for that CPU, otherwise empty
   */
  public static String depthaiWheelHint(String machine) {
    if (machine == null) {
      return "";
    }
    String normalized = machine.toLowerCase();
    if (normalized.startsWith("armv6") || normalized.startsWith("armv7") || normalized.contains("armhf")) {
      return " DepthAI 3 has no binary wheel for 32-bit ARM (" + machine
          + ") on Python 3.13. Use 64-bit Raspberry Pi OS (aarch64). This installer will not compile depthai from source.";
    }
    return "";
  }

  /**
   * Pip install when the venv or the module is missing. A native import error
   * (missing libusb, for example) is not fixed by downloading the wheel again.
   */
  static boolean needsDepthaiInstall(String importError) {
    if (importError == null) {
      return false;
    }
    String text = importError.toLowerCase();
    return text.contains("no module named") || text.contains("modulenotfounderror")
        || text.contains("venv python was not found");
  }

  static boolean usePiwheels(String machine) {
    if (machine == null) {
      return false;
    }
    String normalized = machine.toLowerCase();
    return normalized.startsWith("armv6") || normalized.startsWith("armv7");
  }

  private String importErrorAfterInstall() {
    String importError = py4j.venvImportError("depthai", "numpy");
    if (importError != null && importError.toLowerCase().contains("libusb")) {
      info("depthai needs libusb; trying to install libusb-1.0-0");
      if (py4j.tryAptInstall("libusb-1.0-0")) {
        importError = py4j.venvImportError("depthai", "numpy");
      }
    }
    if (importError == null) {
      return null;
    }
    return "depthai pip install finished but import failed: " + importError;
  }

  private void installOptionalCv() {
    if (py4j == null || py4j.venvCanImport("cv2")) {
      return;
    }
    String machine = py4j.pythonMachine();
    for (String spec : new String[] { OPENCV_HEADLESS_PIP_SPEC, OPENCV_PIP_SPEC }) {
      try {
        py4j.installPipPackages(List.of(spec), List.of("--only-binary=:all:"));
        if (py4j.venvCanImport("cv2")) {
          return;
        }
      } catch (IOException e) {
        warn("Optional %s install failed (RGB JPEG needs cv2; depth still runs): %s", spec, e.getMessage());
      }
    }
    if (usePiwheels(machine)) {
      try {
        py4j.installPipPackages(List.of(OPENCV_HEADLESS_PIP_SPEC),
            List.of("--only-binary=:all:", "--extra-index-url", PIWHEELS_INDEX));
      } catch (IOException e) {
        warn("Optional opencv from piwheels failed: %s", e.getMessage());
      }
    }
  }

  private void installOptionalBlob() {
    try {
      py4j.installPipPackages(List.of(BLOBCONVERTER_PIP_SPEC));
    } catch (IOException optionalFailed) {
      warn("Optional OakD pip packages failed (depthai is installed): %s", optionalFailed.getMessage());
    }
  }

  /**
   * Luxonis devices enumerate as USB vendor 03e7. Without this rule a normal
   * user gets an empty device list, which looks like the camera is missing.
   */
  void ensureMovidiusUdev() {
    if (!Platform.getLocalInstance().isLinux()) {
      return;
    }
    try {
      Path dest = Path.of(MOVIDIUS_UDEV_PATH);
      if (Files.isRegularFile(dest) && Files.readString(dest).contains("03e7")) {
        return;
      }
    } catch (Exception e) {
      log.info("Movidius udev rule is not installed yet: {}", e.getMessage());
    }
    Path local = Path.of(getDataDir(), "80-movidius.rules");
    try {
      Files.createDirectories(local.getParent());
      Files.writeString(local, MOVIDIUS_UDEV_RULE);
    } catch (IOException e) {
      warn("Could not write the Movidius udev rule: %s", e.getMessage());
      return;
    }
    boolean copied = copyUdevRule(local);
    if (!copied) {
      warn(
          "The OAK-D stays hidden until this udev rule is installed: echo '%s' | sudo tee %s && sudo udevadm control --reload-rules && sudo udevadm trigger",
          MOVIDIUS_UDEV_RULE.trim(), MOVIDIUS_UDEV_PATH);
      return;
    }
    info("Installed the Movidius udev rule so the OAK-D is visible without root. Replug the camera if it was already connected.");
  }

  private boolean copyUdevRule(Path local) {
    try {
      Files.copy(local, Path.of(MOVIDIUS_UDEV_PATH), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      if (!runOsCommand(sudoPrefix("cp", local.toString(), MOVIDIUS_UDEV_PATH))) {
        return false;
      }
    }
    boolean reloaded = runOsCommand(sudoPrefix("udevadm", "control", "--reload-rules"));
    runOsCommand(sudoPrefix("udevadm", "trigger"));
    if (!reloaded) {
      warn("The udev rule is on disk but was not reloaded. Run: sudo udevadm control --reload-rules && sudo udevadm trigger");
    }
    return true;
  }

  private static List<String> sudoPrefix(String... args) {
    List<String> command = new ArrayList<>();
    if (!"root".equals(System.getProperty("user.name"))) {
      command.add("sudo");
      command.add("-n");
    }
    if (args != null) {
      for (String arg : args) {
        command.add(arg);
      }
    }
    return command;
  }

  private boolean runOsCommand(List<String> command) {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);
    try {
      Process proc = pb.start();
      StringBuilder output = new StringBuilder();
      Thread reader = new Thread(() -> {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
          String line;
          while ((line = in.readLine()) != null) {
            output.append(line).append('\n');
          }
        } catch (IOException ignored) {
        }
      }, getName() + "-udev");
      reader.setDaemon(true);
      reader.start();
      if (!proc.waitFor(60, TimeUnit.SECONDS)) {
        proc.destroyForcibly();
        reader.join(2000);
        return false;
      }
      reader.join(2000);
      if (proc.exitValue() != 0) {
        log.info("command {} exited {}: {}", command, proc.exitValue(), output);
        return false;
      }
      return true;
    } catch (Exception e) {
      log.info("command {} failed: {}", command, e.getMessage());
      return false;
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

  /**
   * WebGui click on the RGB image or the depth map. {@code nx}/{@code ny} are
   * 0–1 across the image, origin top left. The pixel is unprojected and, when
   * a simulator is running, published through the same chest-camera path as a
   * mesh click so FABRIK moves the arm. The marker comes back on
   * {@link #publishDepthClick}.
   */
  public DepthClick clickImage(double nx, double ny) {
    DepthImageClick sample = DepthImageClick.at(lastFrame, (float) nx, (float) ny);
    if (sample == null) {
      lastError = lastFrame == null ? "No depth frame yet" : "No depth at that pixel";
      warn(lastError);
      broadcastState();
      return null;
    }
    lastError = null;
    JMonkeyEngine jme = findSimulator();
    if (jme != null) {
      jme.clickFromCamera(sample);
      return null;
    }
    float scale = depthCloudScale > 0f ? depthCloudScale : 1f;
    Point point = new Point(sample.camX * scale, -sample.camY * scale, sample.camZ * scale);
    DepthClick click = depthClick(sample, point, Math.abs(sample.camZ * scale));
    log.info("OAK-D image click {} — no simulator, arm not moved", point);
    onDepthClick(click);
    return click;
  }

  /**
   * Click picked on the image or reprojected from a simulator mesh hit.
   * WebGui panels listen here for the marker.
   */
  public void onDepthClick(DepthClick click) {
    if (click == null) {
      return;
    }
    lastClick = click;
    lastError = null;
    if (click.distanceM > 0f) {
      lastClickDistanceM = click.distanceM;
    }
    invoke("publishDepthClick", click);
    broadcastState();
  }

  public DepthClick publishDepthClick(DepthClick click) {
    return click;
  }

  private static DepthClick depthClick(DepthImageClick sample, Point point, float distanceM) {
    DepthClick click = new DepthClick();
    click.nx = sample.nx;
    click.ny = sample.ny;
    click.u = sample.u;
    click.v = sample.v;
    click.depthMm = sample.depthMm;
    click.x = point.getX();
    click.y = point.getY();
    click.z = point.getZ();
    click.distanceM = distanceM;
    return click;
  }

  private DepthFrame publishConverted(DepthFrame frame) {
    lastFrame = frame;
    if (frame != null) {
      if (frame.normalizeIntrinsics()) {
        log.info("OAK-D intrinsics rescaled to {}x{}: fx={} fy={} cx={} cy={}", frame.width, frame.height, frame.fx,
            frame.fy, frame.cx, frame.cy);
      }
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
    publishWebPreview(frame);
    return frame;
  }

  /**
   * JPEG pair for the WebGui. Skipped when the live view is off or no panel is
   * subscribed, so the simulator path does not encode frames nobody is watching.
   */
  private void publishWebPreview(DepthFrame frame) {
    if (frame == null || config == null || !config.displayWeb) {
      return;
    }
    List<MRLListener> subs = getNotifyList("publishWebDisplay");
    if (subs == null || subs.isEmpty()) {
      return;
    }
    OakDPreview preview = OakDPreview.from(frame);
    if (preview != null) {
      // Same outbox path as publishDepthHud. broadcast() addresses the
      // WebGui client directly and those frames never arrive.
      invoke("publishWebDisplay", preview);
    }
  }

  public OakDPreview publishWebDisplay(OakDPreview preview) {
    return preview;
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
      ensureMovidiusUdev();
      String depthaiError = py4j.venvImportError("depthai");
      if (depthaiError != null && !depthaiError.contains("timed out")) {
        lastError = "depthai is not importable in the Py4j venv: " + depthaiError;
        error(lastError);
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
