package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class AzureTranslatorMeta {
  public final static Logger log = LoggerFactory.getLogger(AzureTranslatorMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.AzureTranslator");
    Platform platform = Platform.getLocalInstance();
    
    meta.addDescription("interface to Azure translation services");
    meta.addCategory("translation", "cloud", "ai");
    meta.addDependency("io.github.firemaples", "microsoft-translator-java-api", "0.8.3");
    meta.setCloudService(true);
    meta.setRequiresKeys(true);
    return meta;
  }
  
}

