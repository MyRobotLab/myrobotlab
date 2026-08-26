package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.jme3.UserDataConfig;

public class JMonkeyEngineConfig extends ServiceConfig {

  /**
   * Models for JMonkeyEngine to load - can be of format
   */
  public List<String> models = new ArrayList<>();
  
  /**
   * A spatial associated with some part of the scene graph
   */
  public Map<String, UserDataConfig> nodes = new LinkedHashMap<>();
  public Map<String, String[]> multiMapped = new LinkedHashMap<>();
  
  /**
   * The name of the node which the camera should look at
   */
  public String cameraLookAt;

  /**
   * VinMoov node to parent the chest depth camera (e.g. {@code i01.torso.topStom}).
   */
  public String chestCameraParent;

  /** Scene-graph name of the dummy chest depth camera. */
  public String chestCameraNode = "i01.chest.depthCamera";

  /**
   * Place the dummy camera at the visual center of the torso mesh (excluding
   * head/arms), then apply {@link #chestCameraX}/{@link #chestCameraY}/
   * {@link #chestCameraZ} as a local offset. Without this the camera often
   * lands at the world origin (on the floor) when the joint pivot is not the
   * chest center.
   */
  public boolean chestCameraCenterOnTorso = true;

  /**
   * Local translation offset in meters (JME Y-up), added after torso centering.
   * Default is a few centimeters forward of the chest plate.
   */
  public float chestCameraX = 0f;
  public float chestCameraY = 0f;
  public float chestCameraZ = 0.06f;

  /** Local rotation of the chest camera, degrees (pitch, yaw, roll). */
  public float chestCameraPitchDeg = -8f;
  public float chestCameraYawDeg = 0f;
  public float chestCameraRollDeg = 0f;

  /** Draw the colorized depth map on the HUD. */
  public boolean depthHud = true;

  /** Draw the 3D depth point cloud parented to the chest camera. */
  public boolean depthCloud = true;

  /**
   * Extra multiplier on camera-frame meters. {@code 1} matches IK / VinMoov
   * meters. Increase if the cloud still looks tiny after parent-scale
   * compensation.
   */
  public float depthCloudScale = 1f;

  /**
   * Divide out the chest node's world scale so 1 m of depth stays 1 m even
   * when the rig is imported with a non-unit scale.
   */
  public boolean depthCloudMatchWorldMeters = true;

  /**
   * Edge length of each depth voxel in meters (before scale). ~3 cm fills a
   * stride-8 OAK-D cloud at 1–2 m so it reads as a surface, not dust.
   */
  public float depthCloudVoxelM = 0.03f;

  /**
   * Draw an organized RGB-textured mesh instead of voxel cubes. OakD's
   * {@code rgbMesh} checkbox publishes this to the simulator.
   */
  public boolean depthRgbMesh = false;

  /**
   * Drop a mesh quad when any edge's Z jump exceeds this (meters). Stops
   * rubber-sheet triangles across object silhouettes.
   */
  public float depthMeshMaxEdgeM = 0.12f;

}
