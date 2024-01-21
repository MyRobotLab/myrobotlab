package org.myrobotlab.service.config;

public class Py4jConfig extends ServiceConfig {

  /**
   * root of python scripts - if not specified by user it will be
   * /data/Py4j/{serviceName}
   */
  public String scriptRootDir;

  /**
   * Whether to use the bundled Python executable
   * or invoke the system Python.
   */
  public boolean useBundledPython = true;
  /**
   * Whether to start a new Python process with
   * stdin/stdout connected to the JVM or leave the
   * starting of the process to the user.
   */
  public boolean autostartPython = true;

}
