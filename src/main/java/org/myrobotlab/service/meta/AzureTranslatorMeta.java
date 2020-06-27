package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class AzureTranslatorMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(AzureTranslatorMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {

    MetaData meta = new MetaData("org.myrobotlab.service.AzureTranslator");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("interface to Azure translation services");
    meta.addCategory("translation", "cloud", "ai");
    meta.addDependency("io.github.firemaples", "microsoft-translator-java-api", "0.8.3");
    meta.setCloudService(true);
    meta.setRequiresKeys(true);
    return meta;
  }
  
}

