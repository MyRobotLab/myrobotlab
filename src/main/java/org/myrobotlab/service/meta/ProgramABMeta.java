package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class ProgramABMeta {
  public final static Logger log = LoggerFactory.getLogger(ProgramABMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.ProgramAB");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("AIML 2.0 Reference interpreter based on Program AB");
    meta.addCategory("ai");

    // FIXME - add Wikipedia local search !!
    meta.addPeer("search", "GoogleSearch", "replacement for handling pannous sriax requests");

    // TODO: renamed the bots in the program-ab-data folder to prefix them so we
    // know they are different than the inmoov bots.
    // each bot should have their own name, it's confusing that the inmoov bots
    // are named en-US and so are the program ab bots.

    // meta.addDependency("program-ab", "program-ab-data", "1.2", "zip");
    // meta.addDependency("program-ab", "program-ab-kw", "0.0.8.5");

    meta.addDependency("program-ab", "program-ab-data", null, "zip");
    meta.addDependency("program-ab", "program-ab-kw", "0.0.8.6");

    meta.addDependency("org.json", "json", "20090211");
    // used by FileIO
    meta.addDependency("commons-io", "commons-io", "2.5");
    // TODO: This is for CJK support in ProgramAB move this into the published
    // POM for ProgramAB so they are pulled in transiently.
    meta.addDependency("org.apache.lucene", "lucene-analyzers-common", "8.4.1");
    meta.addDependency("org.apache.lucene", "lucene-analyzers-kuromoji", "8.4.1");
    meta.addCategory("ai", "control");
    return meta;
  }
  
}

