package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

public class PythonConfig extends ServiceConfig {
  
  /**
   * root of python scripts - if not specified by user it will be
   *  /data/Python/{serviceName}
   */
  public String scriptRootDir;

  /**
   * scripts to execute when python is started
   */
  public List<String> startScripts = new ArrayList<>();

  /**
   * scripts to execute when python is stopped
   */
  public List<String> stopScripts = new ArrayList<>();

  /**
   * dist or site paths for pure python 2.7 modules
   */
  public List<String> modulePaths = new ArrayList<>();

  
}
