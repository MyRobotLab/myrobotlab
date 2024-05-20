package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class Deeplearning4jMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Deeplearning4jMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public Deeplearning4jMeta() {

    String dl4jVersion = "1.0.0-M2.1";

    boolean cudaEnabled = Boolean.valueOf(System.getProperty("gpu.enabled", "false"));
    boolean supportRasPi = false;

    addDescription("A wrapper service for the Deeplearning4j framework.");
    addCategory("ai");

    // Force javacpp 1.5.3 to resolve conflict between dl4j and javacv
    addDependency("org.bytedeco", "javacpp", "1.5.8");

    // REMOVED FOR COLLISION
    // addDependency("org.bytedeco", "openblas", "0.3.21-" + "1.5.8");

    // dl4j deps.
    addDependency("org.deeplearning4j", "deeplearning4j-core", dl4jVersion);
    exclude("org.slf4j", "slf4j-api");
    addDependency("org.deeplearning4j", "deeplearning4j-zoo", dl4jVersion);
    addDependency("org.deeplearning4j", "deeplearning4j-nn", dl4jVersion);
    addDependency("org.deeplearning4j", "deeplearning4j-modelimport", dl4jVersion);

    // the miniXCEPTION network / model for emotion detection on detected faces
    addDependency("miniXCEPTION", "miniXCEPTION", "0.0", "zip");

    if (!cudaEnabled) {
      // By default support native CPU execution.
      addDependency("org.nd4j", "nd4j-native-platform", dl4jVersion);
    } else {
      log.info("-------------------------------");
      log.info("----- DL4J CUDA!         ------");
      log.info("-------------------------------");
      // Use this if you want cuda 9.1 NVidia GPU support
      // TODO: figure out the cuDNN stuff.
      addDependency("org.nd4j", "nd4j-cuda-10.1-platform", dl4jVersion);
      addDependency("org.nd4j", "deeplearning4j-cuda-10.1", dl4jVersion);
    }
    // The default build of 1.0.0-alpha does not support the raspi, we built &
    // host the following dependencies.
    // to support native cpu execution on the raspi.
    if (supportRasPi) {
      addDependency("org.nd4j", "nd4j-native-pi-mrl", dl4jVersion);
      addDependency("org.nd4j", "nd4j-native-platform-pi-mrl", dl4jVersion);
    }
    // due to this bug https://github.com/haraldk/TwelveMonkeys/issues/167 seems
    // we need to explicitly include imageio-core
    addDependency("com.twelvemonkeys.imageio", "imageio-core", "3.1.1");

  }

}
