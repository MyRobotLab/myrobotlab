package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TensorflowMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(TensorflowMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Tensorflow");
    Platform platform = Platform.getLocalInstance();
    /**
     * <pre>
     * tensorflow not ready for primetime
     */
    meta.addDescription("Tensorflow machine learning library from Google");
    meta.addCategory("ai");
    // TODO: what happens when you try to install this on an ARM processor like
    // RasPI or the Jetson TX2 ?
    meta.addDependency("org.tensorflow", "tensorflow", "1.8.0");

    // enable GPU support ?
    boolean gpu = Boolean.valueOf(System.getProperty("gpu.enabled", "false"));
    if (gpu) {
      // Currently only supported on Linux. 64 bit.
      meta.addDependency("org.tensorflow", "libtensorflow", "1.8.0");
      meta.addDependency("org.tensorflow", "libtensorflow_jni_gpu", "1.8.0");
    }
    /* </pre> */
    return meta;
  }
  
}

