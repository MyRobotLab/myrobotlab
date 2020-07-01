package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class SphinxMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(SphinxMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.Sphinx");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("open source pure Java speech recognition");
    meta.addCategory("speech recognition");

    // details here ...
    // https://cmusphinx.github.io/wiki/tutorialsphinx4/
    // meta.addDependency("javax.speech.recognition", "1.0");
    // meta.addDependency("edu.cmu.sphinx", "4-1.0beta6");
    meta.addDependency("edu.cmu.sphinx", "sphinx4-core", "5prealpha-SNAPSHOT");
    meta.addDependency("edu.cmu.sphinx", "sphinx4-data", "5prealpha-SNAPSHOT");
    return meta;
  }

  
  
}

