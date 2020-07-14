package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class AgentMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(AgentMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * 
   * 
   */
  public AgentMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("responsible for spawning a MRL process. Agent can also terminate, respawn and control the spawned process");
    addCategory("framework");
    setSponsor("GroG");
    setLicenseApache();

    // includeServiceInOneJar(true);

  }

  public static void main(String[] args) {
    try {
      AgentMeta meta = new AgentMeta("agent");
      LoggingFactory.init(Level.WARN);
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
