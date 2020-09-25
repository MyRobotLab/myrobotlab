package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
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

  public ChessGameManager(String n, String id) {
    super(n, id);
  }

}
