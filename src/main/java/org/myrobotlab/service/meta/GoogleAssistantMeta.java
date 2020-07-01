package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class GoogleAssistantMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(GoogleAssistantMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.GoogleAssistant");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("Access Google Assistant through voice interaction");
    meta.setAvailable(true); // false if you do not want it viewable in a gui
    meta.addCategory("ai", "cloud");
    meta.setCloudService(true);
    meta.setRequiresKeys(true);
    // return PythonProxy.addMetaData(meta); ???
    return meta;
  }
  
  
}

