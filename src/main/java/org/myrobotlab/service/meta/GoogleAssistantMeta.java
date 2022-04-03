package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class GoogleAssistantMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(GoogleAssistantMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public GoogleAssistantMeta() {
    addDescription("Access Google Assistant through voice interaction");
    setAvailable(true); // false if you do not want it viewable in a gui
    addCategory("ai", "cloud");
    setCloudService(true);
    setRequiresKeys(true);
    // return PythonProxy.addMetaData(meta); ???

  }

}
