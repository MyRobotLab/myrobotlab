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

import org.myrobotlab.document.Classification;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.service.InMoov2;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.SwingGui;
import org.slf4j.Logger;

/**
 * InMoov extra methods for OpenCV service This class provided some helper
 * methods/behaviors ontop of normal opencv. It made tracking yolo inventory a
 * bit easier I guess? It also had helpers for working with the pre filters in
 * the video pipeline TODO: This functionality needs to be rationalized in the
 * new InMoov2 implementation. It was used when starting to TrackHumans,
 * TrackPoint, also it was used to automatically add the preFilters to the video
 * pipeline.
 * 
 */

@Deprecated
public class Vision {
  public Boolean openCVenabled;
  transient private boolean ready = false;
  transient public InMoov2 instance;
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
   * 
   * @param filter
   *          the filter to be added as a pre filter.
   */
  public void addPreFilter(String filter) {
    if (!Arrays.asList(OpenCV.getPossibleFilters()).contains(filter)) {
      log.error("Sorry, {} is an unknown filter.", filter);
      return;
    }
    preFilters.add(filter);
    if (instance.getOpenCV() != null && (instance.getOpenCV().getFilter(filter) == null)) {
      instance.getOpenCV().addFilter(filter).enable();
    }
  }

  public void removePreFilter(String filter) {
    if (preFilters.contains(filter)) {
      preFilters.remove(filter);
      if (instance.getOpenCV() != null && !(instance.getOpenCV().getFilter(filter) == null)) {
        instance.getOpenCV().removeFilter(filter);
      }
    }
  }

  public void enablePreFilters() {
    preFilters.forEach(name -> instance.getOpenCV().addFilter(name).enable());
  }

  /**
   * setActiveFilter will add filter if no exist, enable and setactive
   * 
   * @param filterName
   *          the filter to set active
   * @return the filter that is active. null otherwise
   */
  public OpenCVFilter setActiveFilter(String filterName) {
    if (!Arrays.asList(OpenCV.getPossibleFilters()).contains(filterName)) {
      log.error("Sorry, {} is an unknown filter.", filterName);
      return null;
    }

    if (instance.getOpenCV() != null && (instance.getOpenCV().getFilter(filterName) == null)) {
      instance.getOpenCV().addFilter(filterName);
    }
    instance.getOpenCV().setActiveFilter(filterName);
    // temporary fix overexpand windows
    SwingGui gui = (SwingGui) Runtime.getService("gui");
    if (gui != null) {
      gui.maximize();
    }
    return (OpenCVFilter) instance.getOpenCV().getFilter(filterName);
  }

  public boolean test() {
    if (instance.getOpenCV() != null) {
      instance.getOpenCV().capture();
      if (instance.getOpenCV().isCapturing()) {
        enablePreFilters();
        ready = true;
        return true;
      }
    }
    log.error("Please check OpenCV configuration, test() is NoWorky, change camera index or grabber...");
    return false;
  }

  /**
   * @return check if robot do eyesTracking or headTracking
   */
  public boolean isTracking() {
    if (instance.getEyesTracking() != null && !instance.getEyesTracking().isIdle()) {
      return true;
    }
    if (instance.getHeadTracking() != null && !instance.getHeadTracking().isIdle()) {
      return true;
    }
    return false;
  }

  /**
   * used by gestures, to not block if using openCV...
   * 
   * @return returns if the service is ready
   */
  public boolean isReady() {
    return ready;
  }

  /**
   * Method to a analyze a yolo filter classifier ( maybe dl4j also ? ) This
   * will count objects on the frame and get labels + positions //TODO
   * individual position for multiple same labels..
   * 
   * @param classifications
   *          the list of classifications
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