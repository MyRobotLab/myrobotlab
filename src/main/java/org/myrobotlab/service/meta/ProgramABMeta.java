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
    addDependency("program-ab", "program-ab-kw", "0.0.8.10");
    exclude("ch.qos.logback", "logback-classic");
    exclude("ch.qos.logback", "logback-core");
    
    addDependency("com.fasterxml.jackson.dataformat", "jackson-dataformat-xml", "2.17.1");
    // Pin the revisions Ivy selects. dataformat-xml 2.14 brought older Woodstox.
    addDependency("com.fasterxml.woodstox", "woodstox-core", "7.0.0");
    addDependency("org.codehaus.woodstox", "stax2-api", "4.2.2");

    addDependency("org.json", "json", "20230227");
    addDependency("commons-io", "commons-io", "2.22.0");
    // JapaneseAnalyzer (org.apache.lucene.analysis.ja) is in lucene-analysis-kuromoji.
    addDependency("org.apache.lucene", "lucene-analysis-common", "9.12.3");
    addDependency("org.apache.lucene", "lucene-analysis-kuromoji", "9.12.3");
    addCategory("ai", "control");

  }

}
