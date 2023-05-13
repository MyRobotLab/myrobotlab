package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class EasyBertMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(EasyBertMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public EasyBertMeta() {

    addDescription("EasyBert service - Java BERT sentence embeddings.");
    addCategory("search");

    addDependency("com.robrua.nlp", "easy-bert", "1.0.3");
    addDependency("com.robrua.nlp.models", "easy-bert-uncased-L-12-H-768-A-12", "1.0.0");
    addDependency("org.tensorflow", "tensorflow", "1.15.0");

    setAvailable(true);

  }

}
