package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SerialDevice;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

public class ChessGameManager extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(ChessGameManager.class);

  transient WebGui webgui;
  transient SerialDevice serial;
  transient SpeechSynthesis speech;

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Runtime.start("chessgame", "ChessGameManager");

      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public ChessGameManager(String n) {
    super(n);
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(ChessGameManager.class.getCanonicalName());
    meta.addDescription("manages multiple interfaces for a chess game");
    meta.addCategory("game");
    meta.addPeer("webgui", "WebGui", "webgui");
    meta.addPeer("serial", "Serial", "serial");
    meta.addPeer("speech", "MarySpeech", "speech");
    meta.setAvailable(false);
    return meta;
  }

}
