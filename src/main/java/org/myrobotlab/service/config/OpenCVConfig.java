package org.myrobotlab.service.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.myrobotlab.opencv.OpenCVFilter;

public class OpenCVConfig extends ServiceConfig {

  public Integer cameraIndex;
  public String grabberType;
  public String inputSource;
  public String inputFile;
  public boolean nativeViewer = true;
  public boolean webViewer = false;
  public boolean capturing;
  public Map<String, OpenCVFilter> filters = new LinkedHashMap<>();

}
