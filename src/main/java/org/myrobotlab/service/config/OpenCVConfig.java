package org.myrobotlab.service.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.myrobotlab.opencv.OpenCVFilter;

public class OpenCVConfig extends ServiceConfig {

  public Integer cameraIndex = 0;
  public String grabberType = "OpenCV";
  public String inputSource = "camera";
  public String inputFile = null;
  public boolean nativeViewer = true;
  public boolean webViewer = false;
  public boolean capturing = false;
  public Map<String, OpenCVFilter> filters = new LinkedHashMap<>();
  /**
   * flip the video vertically
   */
  public boolean flip = false;

}
