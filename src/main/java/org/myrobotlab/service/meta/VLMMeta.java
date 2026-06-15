package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class VLMMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(VLMMeta.class);

  /**
   * Meta data for the VLM (Vision Language Model) service. Backed by a local
   * offline Ollama server running an open source vision model such as LLaVA.
   */
  public VLMMeta() {

    addDescription("Vision Language Model - accepts text and images and responds, using an offline open source vision model (LLaVA) via Ollama");

    addDependency("io.github.ollama4j", "ollama4j", "1.0.79");

    exclude("org.slf4j", "slf4j-api");
    exclude("log4j", "log4j");
    exclude("org.slf4j", "slf4j-log4j12");

    addCategory("ai", "vision");

    setSponsor("GroG");
  }

}
