package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class SwingGuiMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(SwingGuiMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.SwingGui");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("Service used to graphically display and control other services");
    meta.addCategory("display");

    meta.includeServiceInOneJar(true);
    meta.addDependency("com.fifesoft", "rsyntaxtextarea", "2.0.5.1");
    meta.addDependency("com.fifesoft", "autocomplete", "2.0.5.1");
    meta.addDependency("com.jidesoft", "jide-oss", "3.6.18");
    meta.addDependency("com.mxgraph", "jgraphx", "1.10.4.2");

    return meta;
  }
  
}

