package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class DocumentPipelineMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(DocumentPipelineMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return MetaData - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.DocumentPipeline");
    Platform platform = Platform.getLocalInstance();
    meta.addDescription("This service will pass a document through a document processing pipeline made up of transformers");
    meta.addCategory("ingest");
    meta.addDependency("org.apache.tika", "tika-core", "1.22");
    meta.addDependency("org.apache.opennlp", "opennlp-tools", "1.6.0");
    meta.addDependency("net.objecthunter", "exp4j", "0.4.8");
    // for parsing wikitext
    meta.addDependency("org.sweble.wikitext", "swc-engine", "3.1.7");
    meta.addDependency("org.sweble.wom3", "sweble-wom3-core", "3.1.7");

    meta.addDependency("com.thoughtworks.xstream", "xstream", "1.4.9");

    // FIXME - add service page, python script, give example of how to use
    meta.setAvailable(false);
    return meta;
  }

  
}

