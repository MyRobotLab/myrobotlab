package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.meta.abstracts.Meta;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WebkitSpeechSynthesisMeta  extends Meta {
  public final static Logger log = LoggerFactory.getLogger(WebkitSpeechSynthesisMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  public MetaData getMetaData() {


    MetaData meta = AbstractSpeechSynthesis.getMetaData("org.myrobotlab.service.WebkitSpeechSynthesis");

    meta.addDescription("Web speech api using Chrome or Firefox speech synthesis");
    meta.setAvailable(true); // false if you do not want it viewable in a
    // gui
    // add dependency if necessary
    meta.addCategory("speech", "sound");
    return meta;
  }
  
  
}

