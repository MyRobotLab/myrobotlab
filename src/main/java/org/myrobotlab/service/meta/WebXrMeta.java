package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class WebXrMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WebXrMeta.class);

  public WebXrMeta(String name) {

    super(name);

    addDescription("used as a general webcam");
    addCategory("vr");

  }

}
