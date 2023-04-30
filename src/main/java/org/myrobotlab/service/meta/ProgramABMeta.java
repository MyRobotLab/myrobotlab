package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.ProgramABConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class ProgramABMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(ProgramABMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public ProgramABMeta() {

    addDescription("AIML 2.0 Reference interpreter based on Program AB");
    addCategory("ai");
    
    addDependency("program-ab", "program-ab-data", null, "zip");
    addDependency("program-ab", "program-ab-kw", "0.0.8.9");
    // For now we ignore Lucene deps from program AB because they conflict with Solr
    // We should update ProgramAB pom
    exclude("org.apache.lucene", "*");
    exclude("ch.qos.logback", "logback-classic");
    exclude("ch.qos.logback", "logback-core");
    
    addDependency("com.fasterxml.jackson.dataformat", "jackson-dataformat-xml", "2.14.0");

    addDependency("org.json", "json", "20090211");
    // used by FileIO
    addDependency("commons-io", "commons-io", "2.7");
    // TODO: This is for CJK support in ProgramAB move this into the published
    // POM for ProgramAB so they are pulled in transiently.
    // For version 9 the coordinates were changed from *-analyzers-* to *-analysis-*
    addDependency("org.apache.lucene", "lucene-analysis-common", "9.0.0");
    addDependency("org.apache.lucene", "lucene-analysis-kuromoji", "9.0.0");
    addCategory("ai", "control");

  }



}
