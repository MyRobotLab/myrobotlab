package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class OakDConfig extends ServiceConfig {

  /**
   * Install DepthAI 3.x into the shared Py4j venv ({@code data/Py4j/venv}) on
   * start. First run is slow (pip). Set false afterward if packages are
   * already installed. Requires a PATH Python that has a depthai 3.x wheel
   * (3.9–3.14). Raspberry Pi OS Trixie 64-bit (aarch64, Python 3.13) is
   * supported. 32-bit ARM has no DepthAI 3 wheel. Do not use depthai 2.29.0
   * on Python 3.13+.
   */
  public boolean py4jInstall = true;

  /**
   * Publish a live RGB image and colorized depth map on
   * {@code publishWebDisplay} while a WebGui panel is subscribed.
   */
  public boolean displayWeb = true;

  /**
   * Skip USB and publish a synthetic wall+box cloud (simulator development).
   */
  public boolean forceSynthetic = false;

  /**
   * If the OAK-D is missing or DepthAI fails, keep publishing synthetic depth.
   */
  public boolean syntheticFallback = true;

  /**
   * Download a YOLO blob and run SpatialDetectionNetwork (needs internet once).
   */
  public boolean enableSpatialDetections = false;

  /** Optional local OpenVINO .blob path; empty uses blobconverter when enabled. */
  public String blobPath = "";

  public int fps = 12;
  public int cloudStride = 8;
  public int minDepthMm = 300;
  public int maxDepthMm = 4000;
  public float confidence = 0.5f;

  /**
   * When true, JMonkeyEngine draws an RGB-textured triangle mesh instead of
   * depth-colored voxels. Requires the color camera JPEG on {@code DepthFrame}.
   */
  public boolean rgbMesh = false;

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "py4j", "Py4j");
    listeners.add(new Listener("publishProcessMessage", getPeerName("py4j"), "onPythonMessage"));
    return plan;
  }

}
