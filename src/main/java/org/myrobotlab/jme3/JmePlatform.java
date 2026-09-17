package org.myrobotlab.jme3;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Map;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.JMonkeyEngineConfig;
import org.slf4j.Logger;

import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.system.NativeLibraryLoader;

/**
 * Display and GPU choices for JMonkeyEngine / LWJGL3. Raspberry Pi OS (64-bit
 * Trixie) can run the simulator: LWJGL ships {@code natives-linux-arm64}, and
 * Mesa V3D provides OpenGL 3.1 — not the OpenGL 3.2 core profile JME 3.6
 * requests by default.
 * <p>
 * JME 3.6 always loads {@code bulletjme} at context init when
 * {@code jme3-bullet} is on the classpath. {@code jme3-bullet-native} 3.3.2
 * ships the ARM64 library as {@code native/linux/aarch64/libbulletjme.so};
 * JME looks for {@code native/linux/arm64/}.
 */
public final class JmePlatform {

  public final static Logger log = LoggerFactory.getLogger(JmePlatform.class);

  private JmePlatform() {
  }

  /**
   * LWJGL3 opens its own GLFW window. AWT {@code GraphicsEnvironment.isHeadless()}
   * is the wrong gate: Debian's {@code openjdk-*-jre-headless} (common on
   * Raspberry Pi) reports headless even when XWayland is up.
   * <p>
   * LWJGL 3.3 GLFW on linux-arm64 is X11-based. {@code WAYLAND_DISPLAY} alone is
   * not enough and can hang {@code glfwInit}. Require {@code DISPLAY} or an X11
   * socket we can bind to {@code :0}.
   */
  public static boolean canCreateWindow() {
    return prepareNativeDisplay();
  }

  /**
   * Point GLFW at a usable X11 display and extract LWJGL natives to a filesystem
   * that allows {@code exec} ({@code /tmp} is often {@code noexec} on Raspberry
   * Pi OS).
   *
   * @return true if GLFW should be attempted
   */
  public static boolean prepareNativeDisplay() {
    configureLwjglExtractPath();
    prepareBulletNatives();
    String os = System.getProperty("os.name", "").toLowerCase();
    if (!os.contains("linux")) {
      return true;
    }
    if (hasText(env("DISPLAY"))) {
      log.info("JME display DISPLAY={} WAYLAND_DISPLAY={}", env("DISPLAY"), env("WAYLAND_DISPLAY"));
      return true;
    }
    String display = firstX11Display();
    if (display != null) {
      boolean javaOk = putJavaEnv("DISPLAY", display);
      boolean nativeOk = putNativeEnv("DISPLAY", display);
      log.info("DISPLAY was unset; using {} (X11 socket) javaEnv={} nativeEnv={} WAYLAND_DISPLAY={}", display, javaOk,
          nativeOk, env("WAYLAND_DISPLAY"));
      return hasText(env("DISPLAY")) || nativeOk || javaOk;
    }
    log.warn("No DISPLAY and no /tmp/.X11-unix/X* — GLFW/X11 cannot open a window (WAYLAND_DISPLAY={})",
        env("WAYLAND_DISPLAY"));
    return false;
  }

  /**
   * True when a Linux box has an X11 socket but the JVM has no {@code DISPLAY}
   * (typical SSH/systemd on a Pi desktop).
   */
  public static boolean hasX11SocketWithoutDisplay() {
    if (hasText(env("DISPLAY"))) {
      return false;
    }
    return firstX11Display() != null;
  }

  public static String windowUnavailableMessage() {
    if (hasX11SocketWithoutDisplay()) {
      return "JMonkeyEngine needs DISPLAY for GLFW/X11. Raspberry Pi OS Trixie is Wayland; XWayland is usually :0. From SSH or systemd: export DISPLAY=:0";
    }
    return "JMonkeyEngine needs an X11 display (DISPLAY or /tmp/.X11-unix/X0). A headless JDK is fine — GLFW does not use AWT — but Wayland alone is not enough for LWJGL 3.3.";
  }

  /**
   * Map an X11 socket file name ({@code X0}) to a DISPLAY value ({@code :0}).
   */
  public static String displayFromX11SocketName(String socketName) {
    if (socketName == null || socketName.length() < 2 || socketName.charAt(0) != 'X') {
      return null;
    }
    String n = socketName.substring(1);
    for (int i = 0; i < n.length(); i++) {
      if (!Character.isDigit(n.charAt(i))) {
        return null;
      }
    }
    return ":" + n;
  }

  static String firstX11Display() {
    File dir = new File("/tmp/.X11-unix");
    if (!dir.isDirectory()) {
      return null;
    }
    File x0 = new File(dir, "X0");
    if (x0.exists()) {
      return ":0";
    }
    File[] socks = dir.listFiles();
    if (socks == null) {
      return null;
    }
    for (File sock : socks) {
      String display = displayFromX11SocketName(sock.getName());
      if (display != null) {
        return display;
      }
    }
    return null;
  }

  public static String selectRenderer(Platform platform) {
    return selectRenderer(platform != null && platform.isArm());
  }

  /**
   * ARM GPUs (Raspberry Pi V3D, etc.) do not create an OpenGL 3.2 core context.
   * OpenGL 2.0 compatibility is what Mesa actually exposes and what JME's GLSL100
   * material techniques run on.
   */
  public static String selectRenderer(boolean arm) {
    return arm ? AppSettings.LWJGL_OPENGL2 : AppSettings.LWJGL_OPENGL32;
  }

  public static void apply(AppSettings settings, JMonkeyEngineConfig config, Platform platform) {
    boolean arm = platform != null && platform.isArm();
    String renderer = null;
    if (config != null && config.renderer != null && !config.renderer.isBlank()) {
      renderer = config.renderer.trim();
    } else {
      renderer = selectRenderer(arm);
    }
    settings.setRenderer(renderer);
    if (arm) {
      // sRGB / MSAA configs often fail GLX framebuffer selection on V3D
      settings.setGammaCorrection(false);
      settings.setSamples(0);
    }
    log.info("JMonkeyEngine renderer {} (arm={})", renderer, arm);
  }

  /**
   * {@link com.jme3.app.LegacyApplication#start()} creates the context, starts
   * the {@code jME3 Main} thread, and returns. The thread that called
   * {@code start()} then exits — that is success, not a failed GLFW init.
   * Treat only a thrown {@code start()} or a dead {@code jME3 Main} as failure.
   */
  public static boolean jmeInitThreadFailed(Throwable startThrew, Thread jmeMain) {
    if (startThrew != null) {
      return true;
    }
    return jmeMain != null && !jmeMain.isAlive();
  }

  public static Thread findJmeMainThread() {
    for (Thread t : Thread.getAllStackTraces().keySet()) {
      if ("jME3 Main".equals(t.getName())) {
        return t;
      }
    }
    return null;
  }

  public static boolean isJmeRenderThread() {
    return "jME3 Main".equals(Thread.currentThread().getName());
  }

  /**
   * JME {@code AppTask.get(timeout)} throws {@code TimeoutException} when the
   * callable returns {@code null}, even if the task finished. Waited
   * {@code enqueue} callables must return this (or any non-null value).
   */
  public static final Boolean ENQUEUE_DONE = Boolean.TRUE;

  public static String glFailureHint(Platform platform) {
    return glFailureHint(platform != null && platform.isArm());
  }

  public static String glFailureHint(boolean arm) {
    if (arm) {
      return "On Raspberry Pi, Mesa V3D is OpenGL 3.1 (not 3.2 core). This service requests OpenGL 2.0 on ARM. Confirm DISPLAY=:0 (XWayland on Trixie) and that libraries/jar contains lwjgl-*-natives-linux-arm64.jar";
    }
    return "Check LWJGL natives for this OS and that a display is available.";
  }

  /**
   * Child-process environment so a respawn on a Pi desktop can see XWayland.
   */
  public static void putLinuxDisplay(java.util.Map<String, String> env) {
    if (env == null) {
      return;
    }
    String display = env.get("DISPLAY");
    if (hasText(display)) {
      return;
    }
    String x11 = firstX11Display();
    if (x11 != null) {
      env.put("DISPLAY", x11);
      log.info("DISPLAY was unset; using {} because an X11 socket exists", x11);
    }
  }

  /**
   * LWJGL extracts {@code .so} files under {@code java.io.tmpdir}. Raspberry Pi
   * OS often mounts {@code /tmp} {@code noexec}, so {@code dlopen} fails and the
   * GLFW window never appears.
   */
  public static void configureLwjglExtractPath() {
    if (System.getProperty("org.lwjgl.system.SharedLibraryExtractPath") != null) {
      return;
    }
    File dir = new File("libraries/native/lwjgl");
    if (!dir.isDirectory() && !dir.mkdirs()) {
      dir = new File(System.getProperty("user.home", "."), ".myrobotlab/lwjgl-natives");
      dir.mkdirs();
    }
    System.setProperty("org.lwjgl.system.SharedLibraryExtractPath", dir.getAbsolutePath());
    log.info("LWJGL native extract path {}", dir.getAbsolutePath());
  }

  /**
   * Point JME's {@code NativeLibraryLoader} at {@code jme3-bullet-native}'s
   * actual ARM layout, extract outside {@code /tmp}, and skip a required
   * {@code bulletjme} load when no native exists (so the InMoov window can
   * still open — {@code usePhysics} is off by default).
   */
  public static void prepareBulletNatives() {
    configureJmeExtractPath();
    remapBulletNativePaths();
    if (!NativeLibraryLoader.isUsingNativeBullet()) {
      return;
    }
    com.jme3.system.Platform jmeOs = JmeSystem.getPlatform();
    String[] search = bulletNativeSearchPaths(jmeOs);
    // x86 / mac: JME 3.6 paths already match jme3-bullet-native 3.3.2
    if (search.length == 0) {
      return;
    }
    if (firstClasspathResource(search) != null || localBulletNativeExists()) {
      return;
    }
    log.warn(
        "jme3-bullet is on the classpath but libbulletjme is not packaged for {}; skipping native load so the simulator can start without physics",
        jmeOs);
    skipRequiredBulletNativeLoad();
  }

  /**
   * JME 3.6 registers {@code native/linux/arm64/libbulletjme.so};
   * {@code jme3-bullet-native} 3.3.2 stores that file under {@code aarch64/}.
   */
  static void remapBulletNativePaths() {
    remapBulletNative(com.jme3.system.Platform.Linux_ARM64);
    remapBulletNative(com.jme3.system.Platform.Linux_ARM32);
  }

  static void remapBulletNative(com.jme3.system.Platform platform) {
    String[] paths = bulletNativeSearchPaths(platform);
    if (paths.length == 0) {
      return;
    }
    String found = firstClasspathResource(paths);
    if (found == null || found.equals(paths[0])) {
      return;
    }
    NativeLibraryLoader.registerNativeLibrary("bulletjme", platform, found);
    log.info("Registered bulletjme {} -> {} (jme3-bullet-native 3.3.2 layout)", platform, found);
  }

  /**
   * JME 3.6 expected path first, then the folders {@code jme3-bullet-native}
   * 3.3.2 actually uses.
   */
  static String[] bulletNativeSearchPaths(com.jme3.system.Platform platform) {
    if (platform == com.jme3.system.Platform.Linux_ARM64) {
      return new String[] { "native/linux/arm64/libbulletjme.so", "native/linux/aarch64/libbulletjme.so" };
    }
    if (platform == com.jme3.system.Platform.Linux_ARM32) {
      return new String[] { "native/linux/arm32/libbulletjme.so", "native/linux/armhf/libbulletjme.so",
          "native/linux/arm_v7/libbulletjme.so" };
    }
    return new String[0];
  }

  static String firstBulletNativeOnClasspath(com.jme3.system.Platform platform) {
    return firstClasspathResource(bulletNativeSearchPaths(platform));
  }

  static String firstClasspathResource(String[] paths) {
    if (paths == null) {
      return null;
    }
    for (String path : paths) {
      if (classpathHas(path)) {
        return path;
      }
    }
    return null;
  }

  static boolean classpathHas(String path) {
    if (path == null || path.isBlank()) {
      return false;
    }
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl != null && cl.getResource(path) != null) {
      return true;
    }
    return JmePlatform.class.getClassLoader().getResource(path) != null;
  }

  static boolean localBulletNativeExists() {
    return new File("libraries/native", System.mapLibraryName("bulletjme")).isFile();
  }

  /**
   * {@link com.jme3.system.JmeDesktopSystem#initialize} loads {@code bulletjme}
   * unless {@link JmeSystem#isLowPermissions()} is true. Run that initialize
   * once with low permissions, then restore so later file access still works.
   */
  static void skipRequiredBulletNativeLoad() {
    JmeSystem.setLowPermissions(true);
    try {
      JmeSystem.initialize(new AppSettings(true));
    } finally {
      JmeSystem.setLowPermissions(false);
    }
  }

  /**
   * JME extracts {@code libbulletjme.so} under {@code java.io.tmpdir} by
   * default — same {@code noexec} problem as LWJGL on Raspberry Pi OS.
   */
  public static void configureJmeExtractPath() {
    File dir = new File("libraries/native/jme");
    if (!dir.isDirectory() && !dir.mkdirs()) {
      dir = new File(System.getProperty("user.home", "."), ".myrobotlab/jme-natives");
      dir.mkdirs();
    }
    NativeLibraryLoader.setCustomExtractionFolder(dir.getAbsolutePath());
    log.info("JME native extract path {}", dir.getAbsolutePath());
  }

  static boolean putJavaEnv(String key, String value) {
    try {
      Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
      java.lang.reflect.Field theEnvironment = pe.getDeclaredField("theEnvironment");
      theEnvironment.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, String> env = (Map<String, String>) theEnvironment.get(null);
      env.put(key, value);
      try {
        java.lang.reflect.Field cie = pe.getDeclaredField("theCaseInsensitiveEnvironment");
        cie.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> env2 = (Map<String, String>) cie.get(null);
        env2.put(key, value);
      } catch (NoSuchFieldException ignored) {
        // Unix JDKs only expose theEnvironment
      }
      return value.equals(System.getenv(key));
    } catch (Exception e) {
      log.debug("could not update Java env {}: {}", key, e.toString());
      return false;
    }
  }

  /**
   * {@code setenv(3)} so GLFW's C {@code getenv} sees DISPLAY. Java's
   * {@link System#getenv()} map is not what native GLFW reads.
   */
  static boolean putNativeEnv(String key, String value) {
    try {
      Class<?> library = Class.forName("org.lwjgl.system.Library");
      Class<?> sharedLibrary = Class.forName("org.lwjgl.system.SharedLibrary");
      java.lang.reflect.Method loadNative = library.getMethod("loadNative", Class.class, String.class, String.class);
      Object libc = loadNative.invoke(null, JmePlatform.class, "", "c");
      java.lang.reflect.Method getFn = sharedLibrary.getMethod("getFunctionAddress", CharSequence.class);
      long setenv = (Long) getFn.invoke(libc, "setenv");
      if (setenv == 0L) {
        return false;
      }
      Class<?> memUtil = Class.forName("org.lwjgl.system.MemoryUtil");
      Class<?> jni = Class.forName("org.lwjgl.system.JNI");
      java.lang.reflect.Method memUTF8 = memUtil.getMethod("memUTF8", CharSequence.class);
      java.lang.reflect.Method memAddress = memUtil.getMethod("memAddress", ByteBuffer.class);
      ByteBuffer k = (ByteBuffer) memUTF8.invoke(null, key);
      ByteBuffer v = (ByteBuffer) memUTF8.invoke(null, value);
      long pk = (Long) memAddress.invoke(null, k);
      long pv = (Long) memAddress.invoke(null, v);
      java.lang.reflect.Method invokePPI = jni.getMethod("invokePPI", long.class, long.class, int.class, long.class);
      invokePPI.invoke(null, pk, pv, 1, setenv);
      return true;
    } catch (Throwable t) {
      log.debug("native setenv failed: {}", t.toString());
      return false;
    }
  }

  private static String env(String key) {
    try {
      return System.getenv(key);
    } catch (Exception e) {
      return null;
    }
  }

  private static boolean hasText(String s) {
    return s != null && !s.isBlank();
  }

}
