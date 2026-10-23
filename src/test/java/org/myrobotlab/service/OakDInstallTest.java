package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

/**
 * Guards the DepthAI pin that failed to install on CPython 3.14 (no 2.29.0
 * wheel → pip compiled from source → exit 1).
 */
public class OakDInstallTest {

  @Test
  public void depthaiSpecIsV3BinaryFriendly() {
    assertTrue(OakD.DEPTHAI_PIP_SPEC.startsWith("depthai>=3"));
    assertTrue(OakD.DEPTHAI_PIP_SPEC.contains("<4"));
    assertFalse(OakD.DEPTHAI_PIP_SPEC.contains("2.29"));
    assertFalse(OakD.DEPTHAI_PIP_SPEC.contains("==2."));
    assertEquals(OakD.DEPTHAI_PIP_SPEC, OakD.requiredPipPackages().get(0));
    assertEquals(1, OakD.requiredPipPackages().size());
    assertTrue(OakD.depthaiPipOptions(true).contains("--only-binary=depthai,numpy"));
    assertTrue(OakD.depthaiPipOptions(true).contains(OakD.LUXONIS_PIP_EXTRA_INDEX));
    assertFalse(OakD.depthaiPipOptions(false).contains(OakD.LUXONIS_PIP_EXTRA_INDEX));
    assertFalse(OakD.requiredPipPackages().contains(OakD.OPENCV_PIP_SPEC));
    assertTrue(OakD.depthaiWheelHint("armv7l").contains("32-bit"));
    assertTrue(OakD.depthaiWheelHint("aarch64").isEmpty());
    assertTrue(OakD.depthaiWheelHint("x86_64").isEmpty());
    assertTrue(OakD.usePiwheels("armv7l"));
    assertFalse(OakD.usePiwheels("aarch64"));
    assertTrue(OakD.needsDepthaiInstall("ModuleNotFoundError: No module named 'depthai'"));
    assertTrue(OakD.needsDepthaiInstall("Py4j venv Python was not found under data/Py4j/venv"));
    assertFalse(OakD.needsDepthaiInstall("ImportError: libusb-1.0.so.0: cannot open shared object file"));
    assertFalse(OakD.needsDepthaiInstall(null));
    assertTrue(OakD.MOVIDIUS_UDEV_RULE.contains("03e7"));
    assertTrue(Py4j.venvNeedsDebianPackage("The virtual environment was not created successfully because ensurepip is not available."));
    assertTrue(Py4j.venvNeedsDebianPackage("No module named venv"));
    assertFalse(Py4j.venvNeedsDebianPackage("No matching distribution found for depthai"));
  }

  @Test
  public void pipErrorTailKeepsLastLines() {
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i <= 50; i++) {
      sb.append("line").append(i).append('\n');
    }
    String tail = Py4j.tailLines(sb.toString(), 5);
    assertTrue(tail.contains("line46"));
    assertTrue(tail.endsWith("line50"));
    assertFalse(tail.contains("line45"));
    assertTrue(Py4j.tailLines("", 10).contains("no pip output"));
  }

  @Test
  public void depthPipelineHasV3CameraApi() throws IOException {
    Path script = Paths.get("src/main/resources/resource/OakD/depth_pipeline.py");
    assertTrue("depth_pipeline.py missing at " + script.toAbsolutePath(), Files.isRegularFile(script));
    String src = Files.readString(script, StandardCharsets.UTF_8);
    assertTrue(src.contains("dai.node.Camera"));
    assertTrue(src.contains("CameraBoardSocket.CAM_B"));
    assertTrue(src.contains("stereo.depth"));
    assertTrue(src.contains("_run_v3"));
    assertTrue(src.contains("_run_v2"));
    assertTrue(src.contains("MonoCamera"));
    assertTrue(src.contains("CAM_A"));
    assertTrue(src.contains("imencode"));
    assertTrue(src.contains("onStridedDepth"));
    assertTrue(src.contains("setOutputSize"));
    assertTrue(src.contains("DEPTH_W"));
    assertTrue(src.contains("_annotate_device_error"));
    assertTrue(src.contains("03e7"));
    assertTrue(src.contains("getAllAvailableDevices"));
  }
}
