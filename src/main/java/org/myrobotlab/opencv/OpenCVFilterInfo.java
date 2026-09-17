package org.myrobotlab.opencv;

import java.io.Serializable;

/**
 * UI-facing description of an OpenCV pipeline filter: what it does, how to use
 * it, and extra dependencies beyond the OpenCV service itself.
 */
public class OpenCVFilterInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String DEP_CORE = "None beyond the OpenCV service (JavaCV / OpenCV).";

  public static final String DEP_HAAR = "Haar cascade XML from the opencv_classifiers Ivy zip under resource/OpenCV/haarcascades/.";

  public static final String DEP_KINECT = "A depth camera (Kinect or similar) producing a 16-bit 1-channel depth image. RGB-only webcams will not work.";

  public static final String DEP_NOT_IMPLEMENTED = "Not implemented in this build.";

  public String type;
  public String description;
  public String usage;
  public String dependencies;

  public OpenCVFilterInfo() {
  }

  public OpenCVFilterInfo(String type, String description, String usage, String dependencies) {
    this.type = type;
    this.description = description;
    this.usage = usage;
    this.dependencies = dependencies;
  }

  public static OpenCVFilterInfo of(String type, String description, String usage) {
    return new OpenCVFilterInfo(type, description, usage, DEP_CORE);
  }

  public static OpenCVFilterInfo of(String type, String description, String usage, String dependencies) {
    return new OpenCVFilterInfo(type, description, usage, dependencies);
  }

}
