package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class FabrikMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(FabrikMeta.class);

  public FabrikMeta() {
    addDescription("FABRIK inverse kinematics (Aristidou) for simulated or physical arms");
    addCategory("robot", "control");
    setAvailable(true);
    addLicense("mit");
    setLink("https://github.com/feduni/caliko");
  }

}
