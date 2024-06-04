package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class DocumentPipelineMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(DocumentPipelineMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public DocumentPipelineMeta() {

    addDescription("This service will pass a document through a document processing pipeline made up of transformers");
    addCategory("ingest");
    addDependency("org.apache.tika", "tika-core", "2.8.0");
    exclude("org.slf4j", "*");
    exclude("log4j", "*");
    exclude("org.apache.logging.log4j", "*");
    exclude("com.fasterxml.jackson.core", "*");
    exclude("io.netty", "*");

    addDependency("org.apache.tika", "tika-parser-audiovideo-module", "2.8.0");
    exclude("org.slf4j", "*");
    exclude("log4j", "*");
    exclude("org.apache.logging.log4j", "*");
    exclude("com.fasterxml.jackson.core", "*");
    exclude("io.netty", "*");

    
    addDependency("org.apache.opennlp", "opennlp-tools", "1.6.0");
    addDependency("net.objecthunter", "exp4j", "0.4.8");
    // for parsing wikitext
    addDependency("org.sweble.wikitext", "swc-engine", "3.1.7");
    addDependency("org.sweble.wom3", "sweble-wom3-core", "3.1.7");
    addDependency("com.thoughtworks.xstream", "xstream", "1.4.19");

    // FIXME - add service page, python script, give example of how to use
    setAvailable(true);

  }

}
