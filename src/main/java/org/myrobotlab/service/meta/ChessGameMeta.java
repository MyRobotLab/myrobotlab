package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class ChessGameMeta {
  public final static Logger log = LoggerFactory.getLogger(ChessGameMeta.class);
  
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType("org.myrobotlab.service.ChessGame");
    Platform platform = Platform.getLocalInstance();
    meta.addCategory("game");
    meta.addDependency("ChessBoard", "ChessBoard", "1.0.0");
    meta.addDescription("Would you like to play a game?");
    return meta;
  }
}

