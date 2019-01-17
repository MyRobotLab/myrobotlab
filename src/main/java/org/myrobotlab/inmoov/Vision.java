package org.myrobotlab.inmoov;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JFrame;

import org.myrobotlab.document.Classification;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.service.InMoov;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
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
  transient public LinkedHashMap<String, Double> collectionPositions = new LinkedHashMap<String, Double>();
  transient public HashMap<String, Integer> collectionCount = new HashMap<String, Integer>();
  transient private HashMap<String, Integer> collectionTemp = new HashMap<String, Integer>();
  transient public Set<String> filteredLabel = new TreeSet<String>();

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

  /**
   * setActiveFilter will add filter if no exist, enable and setactive
   */
  public OpenCVFilter setActiveFilter(String filterName) {
    if (!Arrays.asList(OpenCV.getPossibleFilters()).contains(filterName)) {
      log.error("Sorry, {} is an unknown filter.", filterName);
      return null;
    }

    if (instance.opencv != null && (instance.opencv.getFilter(filterName) == null)) {
      instance.opencv.addFilter(filterName);
    }
    instance.opencv.setActiveFilter(filterName);
    // temporary fix overexpand windows
    SwingGui gui = (SwingGui) Runtime.getService("gui");
    if (gui != null) {
      gui.maximize();
    }
    return (OpenCVFilter) instance.opencv.getFilter(filterName);
  }

  public boolean test() {
    if (instance.opencv != null) {
      instance.opencv.capture();
      if (instance.opencv.isCapturing()) {
        enablePreFilters();
        ready = true;
        return true;
      }
    }
    log.error("Please check OpenCV configuration, test() is NoWorky, change camera index or grabber...");
    return false;
  }

  /**
   * check if robot do eyesTracking or headTracking
   */
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

  /**
   * Method to a analyze a yolo filter classifier ( maybe dl4j also ? ) This
   * will count objects on the frame and get labels + positions //TODO
   * individual position for multiple same labels..
   */
  public void yoloInventory(TreeMap<String, List<Classification>> classifications) {

    // reset previous same classified objects ( to count same objetcs on the
    // frame )
    for (Map.Entry<String, List<Classification>> entry : classifications.entrySet()) {
      List<Classification> value = entry.getValue();
      for (Classification document : value) {
        if (collectionCount.containsKey(document.getLabel())) {
          collectionCount.remove(document.getLabel());
        }
      }
    }

    // add now labels and positions to collection
    for (Map.Entry<String, List<Classification>> entry : classifications.entrySet()) {
      List<Classification> value = entry.getValue();
      for (Classification document : value) {
        // sometime we want to filter labels
        if (!filteredLabel.contains(document.getLabel())) {
          Integer existingLabelCount = 1;
          if (collectionCount.containsKey(document.getLabel())) {
            existingLabelCount = collectionCount.get(document.getLabel()) + 1;
          }
          collectionCount.put(document.getLabel(), existingLabelCount);
          collectionPositions.put(document.getLabel(), (double) document.getBoundingBox().x);
        }
      }
    }
    // Sort result based on position
    collectionTemp.clear();
    collectionPositions.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(key -> collectionTemp.put(key.getKey(), collectionCount.get(key.getKey())));
    collectionCount.clear();
    collectionCount.putAll(collectionTemp);
    log.info("onClassification collectionCount : {}", collectionTemp);
  }

  public Integer getPosition(String text) {
    List<String> indexes = new ArrayList<String>(collectionCount.keySet());
    return indexes.indexOf(text) + 1;
  }
}