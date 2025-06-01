package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class SphinxMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(SphinxMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public SphinxMeta() {

    addDescription("open source pure Java speech recognition");
    addCategory("speech recognition");

    // details here ...
    // https://cmusphinx.github.io/wiki/tutorialsphinx4/
    // addDependency("javax.speech.recognition", "1.0");
    // addDependency("edu.cmu.sphinx", "4-1.0beta6");
    addDependency("edu.cmu.sphinx", "sphinx4-core", "5prealpha-SNAPSHOT");
    addDependency("edu.cmu.sphinx", "sphinx4-data", "5prealpha-SNAPSHOT");

  }

}
