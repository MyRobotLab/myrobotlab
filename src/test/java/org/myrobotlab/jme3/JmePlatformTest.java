package org.myrobotlab.jme3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.myrobotlab.service.config.JMonkeyEngineConfig;

import com.jme3.system.AppSettings;
import com.jme3.system.Platform;

public class JmePlatformTest {

  @Test
  public void armUsesOpenGl2Not32Core() {
    assertEquals(AppSettings.LWJGL_OPENGL2, JmePlatform.selectRenderer(true));
    assertEquals(AppSettings.LWJGL_OPENGL32, JmePlatform.selectRenderer(false));
  }

  @Test
  public void configRendererOverridesArmDefault() {
    AppSettings settings = new AppSettings(true);
    JMonkeyEngineConfig config = new JMonkeyEngineConfig();
    config.renderer = AppSettings.LWJGL_OPENGL31;
    JmePlatform.apply(settings, config, null);
    assertEquals(AppSettings.LWJGL_OPENGL31, settings.getRenderer());
  }

  @Test
  public void armHintMentionsRaspberryPiAndNatives() {
    String hint = JmePlatform.glFailureHint(true);
    assertTrue(hint.contains("Raspberry Pi"));
    assertTrue(hint.contains("natives-linux-arm64"));
    assertFalse(JmePlatform.glFailureHint(false).contains("Raspberry Pi"));
  }

  @Test
  public void x11SocketNameMapsToDisplay() {
    assertEquals(":0", JmePlatform.displayFromX11SocketName("X0"));
    assertEquals(":1", JmePlatform.displayFromX11SocketName("X1"));
    assertEquals(":10", JmePlatform.displayFromX11SocketName("X10"));
    assertEquals(null, JmePlatform.displayFromX11SocketName("wayland-0"));
    assertEquals(null, JmePlatform.displayFromX11SocketName("X"));
    assertEquals(null, JmePlatform.displayFromX11SocketName(null));
  }

  @Test
  public void waylandOnlyMessageMentionsX11() {
    String msg = JmePlatform.windowUnavailableMessage();
    assertTrue(msg.contains("DISPLAY") || msg.contains("X11") || msg.contains("Wayland"));
  }

  @Test
  public void bullet332ShipsAarch64NotJme36Arm64() {
    assertFalse("JME 3.6 looks here and the 3.3.2 jar does not use this folder",
        JmePlatform.classpathHas("native/linux/arm64/libbulletjme.so"));
    assertTrue("jme3-bullet-native 3.3.2 stores the Pi 64-bit library here",
        JmePlatform.classpathHas("native/linux/aarch64/libbulletjme.so"));
  }

  @Test
  public void remapsJme36LinuxArm64ToBullet332Aarch64() {
    assertEquals("native/linux/aarch64/libbulletjme.so", JmePlatform.firstBulletNativeOnClasspath(Platform.Linux_ARM64));
    assertEquals("native/linux/armhf/libbulletjme.so", JmePlatform.firstBulletNativeOnClasspath(Platform.Linux_ARM32));
    JmePlatform.remapBulletNativePaths();
  }

  @Test
  public void enqueueDoneIsNonNullSoAppTaskGetDoesNotFakeTimeout() {
    assertNotNull(JmePlatform.ENQUEUE_DONE);
    assertTrue(JmePlatform.ENQUEUE_DONE);
  }

  @Test
  public void launcherThreadExitIsNotJmeInitFailure() {
    assertFalse("Application.start() returns after spawning jME3 Main",
        JmePlatform.jmeInitThreadFailed(null, null));
    Thread alive = new Thread(() -> {
      try {
        Thread.sleep(10_000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }, "alive-jme");
    alive.setDaemon(true);
    alive.start();
    try {
      assertFalse(JmePlatform.jmeInitThreadFailed(null, alive));
      assertTrue(JmePlatform.jmeInitThreadFailed(new RuntimeException("glfw"), null));
      alive.interrupt();
      try {
        alive.join(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      assertTrue(JmePlatform.jmeInitThreadFailed(null, alive));
    } finally {
      alive.interrupt();
    }
  }

  @Test
  public void arm64SearchPrefersJme36PathThenAarch64() {
    String[] paths = JmePlatform.bulletNativeSearchPaths(Platform.Linux_ARM64);
    assertEquals("native/linux/arm64/libbulletjme.so", paths[0]);
    assertEquals("native/linux/aarch64/libbulletjme.so", paths[1]);
    assertNotNull(JmePlatform.firstClasspathResource(paths));
  }

}
