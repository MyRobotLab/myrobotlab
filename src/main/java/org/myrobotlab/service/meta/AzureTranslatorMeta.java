package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class AzureTranslatorMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(AzureTranslatorMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   */
  public AzureTranslatorMeta() {
    addDescription("interface to Azure translation services");
    addCategory("translation", "cloud", "ai");
    addDependency("com.squareup.okhttp", "okhttp", "2.7.5");
    setCloudService(true);
    setRequiresKeys(true);
  }

}
