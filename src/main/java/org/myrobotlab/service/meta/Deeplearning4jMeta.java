package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Deeplearning4jMeta {
  public final static Logger log = LoggerFactory.getLogger(Deeplearning4jMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    String dl4jVersion = "1.0.0-beta6";

    boolean cudaEnabled = Boolean.valueOf(System.getProperty("gpu.enabled", "false"));
    boolean supportRasPi = false;

    ServiceType meta = new ServiceType("org.myrobotlab.service.Deeplearning4j");
    meta.addDescription("A wrapper service for the Deeplearning4j framework.");
    meta.addCategory("ai");
    
    // Force javacpp 1.5.3 to resolve conflict between dl4j and javacv
    String javaCppVersion = "1.5.3";
    meta.addDependency("org.bytedeco", "javacpp", javaCppVersion);
    meta.addDependency("org.bytedeco", "openblas", "0.3.9-"+javaCppVersion);
    
    // dl4j deps.
    meta.addDependency("org.deeplearning4j", "deeplearning4j-core", dl4jVersion);
    meta.addDependency("org.deeplearning4j", "deeplearning4j-zoo", dl4jVersion);
    meta.addDependency("org.deeplearning4j", "deeplearning4j-nn", dl4jVersion);
    meta.addDependency("org.deeplearning4j", "deeplearning4j-modelimport", dl4jVersion);
    
    // the miniXCEPTION network / model for emotion detection on detected faces
    meta.addDependency("miniXCEPTION", "miniXCEPTION", "0.0", "zip");
    
    if (!cudaEnabled) {
      // By default support native CPU execution.
      meta.addDependency("org.nd4j", "nd4j-native-platform", dl4jVersion);
    } else {
      log.info("-------------------------------");
      log.info("----- DL4J CUDA!         ------");
      log.info("-------------------------------");
      // Use this if you want cuda 9.1 NVidia GPU support
      // TODO: figure out the cuDNN stuff.
      meta.addDependency("org.nd4j", "nd4j-cuda-10.1-platform", dl4jVersion);
      meta.addDependency("org.nd4j", "deeplearning4j-cuda-10.1", dl4jVersion);
    }
    // The default build of 1.0.0-alpha does not support the raspi, we built &
    // host the following dependencies.
    // to support native cpu execution on the raspi.
    if (supportRasPi) {
      meta.addDependency("org.nd4j", "nd4j-native-pi-mrl", dl4jVersion);
      meta.addDependency("org.nd4j", "nd4j-native-platform-pi-mrl", dl4jVersion);
    }
    // due to this bug https://github.com/haraldk/TwelveMonkeys/issues/167 seems
    // we need to explicitly include imageio-core
    meta.addDependency("com.twelvemonkeys.imageio", "imageio-core", "3.1.1");
    return meta;
  }
  
  
}

