package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class ProgramABMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(ProgramABMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * @param name n
   * 
   */
  public ProgramABMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("AIML 2.0 Reference interpreter based on Program AB");
    addCategory("ai");

    // FIXME - add Wikipedia local search !!
    addPeer("search", "GoogleSearch", "replacement for handling pannous sriax requests");

    // TODO: renamed the bots in the program-ab-data folder to prefix them so we
    // know they are different than the inmoov bots.
    // each bot should have their own name, it's confusing that the inmoov bots
    // are named en-US and so are the program ab bots.

    // addDependency("program-ab", "program-ab-data", "1.2", "zip");
    // addDependency("program-ab", "program-ab-kw", "0.0.8.5");

    addDependency("program-ab", "program-ab-data", null, "zip");
    addDependency("program-ab", "program-ab-kw", "0.0.8.6");

    addDependency("org.json", "json", "20090211");
    // used by FileIO
    addDependency("commons-io", "commons-io", "2.7");
    // TODO: This is for CJK support in ProgramAB move this into the published
    // POM for ProgramAB so they are pulled in transiently.
    addDependency("org.apache.lucene", "lucene-analyzers-common", "8.8.2");
    addDependency("org.apache.lucene", "lucene-analyzers-kuromoji", "8.8.2");
    addCategory("ai", "control");

  }

}
