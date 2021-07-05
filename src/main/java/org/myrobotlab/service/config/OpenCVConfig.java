package org.myrobotlab.service.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class OpenCVConfig extends ServiceConfig {

  public Integer cameraIndex;
  public String grabberType;
  public String inputSource;
  public String inputFile;
  public Boolean nativeViewer;
  public Boolean webViewer;  
  public Map<String, OpenCVFilterConfig> filters = new LinkedHashMap<>();

}
