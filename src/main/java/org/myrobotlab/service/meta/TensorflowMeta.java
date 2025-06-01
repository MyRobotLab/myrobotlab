package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TensorflowMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(TensorflowMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public TensorflowMeta() {

    /**
     * <pre>
     * tensorflow not ready for primetime
     */
    addDescription("Tensorflow machine learning library from Google");
    addCategory("ai");
    // TODO: what happens when you try to install this on an ARM processor like
    // RasPI or the Jetson TX2 ?
    addDependency("org.tensorflow", "tensorflow", "1.8.0");

    // enable GPU support ?
    boolean gpu = Boolean.valueOf(System.getProperty("gpu.enabled", "false"));
    if (gpu) {
      // Currently only supported on Linux. 64 bit.
      addDependency("org.tensorflow", "libtensorflow", "1.8.0");
      addDependency("org.tensorflow", "libtensorflow_jni_gpu", "1.8.0");
    }
    /* </pre> */

  }

}
