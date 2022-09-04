package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class ArmMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(ArmMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   */
  public ArmMeta() {
    addDescription("robot arm service");
    addCategory("robot");
    setLicenseApache();
    addTodo("add IK interfacing points");
    // FIXME - add IK & DH Parameters
    // not ready for primetime - nothing implemented
    setAvailable(false);

  }

}
