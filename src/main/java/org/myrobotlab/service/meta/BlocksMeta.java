package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class BlocksMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(BlocksMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * 
   * 
   */
  public BlocksMeta() {

    Platform platform = Platform.getLocalInstance();

    addDescription("basic block programming interface");
    setAvailable(false);
    // add dependency if necessary
    // addDependency("org.coolproject", "1.0.0");
    addCategory("programming");

  }

}
