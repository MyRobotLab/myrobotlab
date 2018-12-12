package org.myrobotlab.inmoov;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.service.InMoov;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

/**
 * InMoov extra methods for OpenCV service
 */
public class Vision {
  public Boolean openCVenabled;
  transient private boolean ready = false;
  transient public InMoov instance;
  public final static Logger log = LoggerFactory.getLogger(Vision.class);
  public Set<String> preFilters = new TreeSet<String>();

  /**
   * Init default config parameters
   */
  public void init() {
    openCVenabled = false;
  }

  public void setOpenCVenabled(boolean enable) {
    openCVenabled = enable;
  }

  /**
   * pre filters are "always on" filters, set by user, like Flip or PyramidDown
   */
  public void addPreFilter(String filter) {
    if (!Arrays.asList(OpenCV.getPossibleFilters()).contains(filter)) {
      log.error("Sorry, {} is an unknown filter.", filter);
      return;
    }
    preFilters.add(filter);
    if (instance.opencv != null && (instance.opencv.getFilter(filter) == null)) {
      instance.opencv.addFilter(filter).enable();
    }
  }

  public void removePreFilter(String filter) {
    if (preFilters.contains(filter)) {
      preFilters.remove(filter);
      if (instance.opencv != null && !(instance.opencv.getFilter(filter) == null)) {
        instance.opencv.removeFilter(filter);
      }
    }
  }

  public void enablePreFilters() {
    preFilters.forEach(name -> instance.opencv.addFilter(name).enable());
  }

  public OpenCVFilter setActiveFilter(String filterName) {
    if (!Arrays.asList(OpenCV.getPossibleFilters()).contains(filterName)) {
      log.error("Sorry, {} is an unknown filter.", filterName);
      return null;
    }

    if (instance.opencv != null && (instance.opencv.getFilter(filterName) == null)) {
      instance.opencv.addFilter(filterName);
    }
    instance.opencv.setActiveFilter(filterName);
    return (OpenCVFilter) instance.opencv.getFilter(filterName);
  }

  public boolean test() {
    if (instance.opencv != null) {
      instance.opencv.capture();
      if (instance.opencv.isCapturing()) {
        enablePreFilters();
        instance.opencv.stopCapture();
        ready = true;
        return true;
      }
    }
    log.error("Please check OpenCV configuration, test() is NoWorky, change camera index or grabber...");
    return false;
  }

  public boolean isCameraOn() {
    if (instance.opencv != null) {
      if (instance.opencv.isCapturing()) {
        return true;
      }
    }
    return false;
  }

  public boolean isTracking() {
    if (instance.eyesTracking != null && !instance.eyesTracking.isIdle()) {
      return true;
    }
    if (instance.headTracking != null && !instance.headTracking.isIdle()) {
      return true;
    }
    return false;
  }

  /**
   * used by gestures, to not block if using openCV...
   */
  public boolean isReady() {
    return ready;
  }
}