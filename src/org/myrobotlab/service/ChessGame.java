/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import org.myrobotlab.chess.HMove;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class ChessGame extends Service {

  public final static Logger log = LoggerFactory.getLogger(ChessGame.class.getCanonicalName());

  private static final long serialVersionUID = 1L;

  int state = 0;

  int column;

  int row;
  int pressedAmount;
  char columnLetter;
  String hmoveMsg = "";

  public static void main(String[] args) throws ClassNotFoundException {
    LoggingFactory.init(Level.DEBUG);

    try {
      Runtime.start("chessgame", "ChessGame");
      Runtime.start("python", "Python");
      Runtime.start("gui", "SwingGui");
    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  public ChessGame(String n) {
    super(n);
  }

  public String computerMoved(String move) {
    log.info("computerMoved " + move);
    return move;
  }

  public HMove inputHMove(HMove s) {
    return s;
  }

  public String inputMove(String s) {
    log.debug("inputMove " + s);
    return s;
  }

  public HMove makeHMove(HMove m) {

    return m;
  }

  public String makeMove(HMove m, String code) {
    String t = m.toString();
    log.info(t);

    if (t.length() == 6) {
      t = t.substring(1);
    }

    t = (t.substring(0, 2) + t.substring(3));

    t = "x" + t + code + "z";
    t = t.toLowerCase();

    invoke("publishMove", t);

    log.info(t);

    return t;
  }

  public String publishMove(String move) {
    return move;
  }

  public void move(String move) {
    invoke("inputMove", move);
  }

  public String parseOSC(String data) {
    ++pressedAmount;

    String inputType = data.substring(0, 7);
    // println(data);

    if (inputType.equals("/1/push")) { // A1
      if (state == 0) {
        state = 1;
      } else if (state == 1) {
        state = 0;
      }
    }

    // int stringLength = data.length();
    String inputNumber = data.substring(7, 9); // bad - just don't know OSC
    // yet
    int indexNumber = Integer.parseInt(inputNumber);

    if (state == 1) {
      if (indexNumber >= 1 && indexNumber <= 8) {
        row = 1;
        column = indexNumber - 1;
      }
      if (indexNumber >= 9 && indexNumber <= 16) {
        row = 2;
        column = indexNumber - 9;
      }
      if (indexNumber >= 17 && indexNumber <= 24) {
        row = 3;
        column = indexNumber - 17;
      }
      if (indexNumber >= 25 && indexNumber <= 32) {
        row = 4;
        column = indexNumber - 25;
      }
      if (indexNumber >= 33 && indexNumber <= 40) {
        row = 5;
        column = indexNumber - 33;
      }
      if (indexNumber >= 41 && indexNumber <= 48) {
        row = 6;
        column = indexNumber - 41;
      }
      if (indexNumber >= 49 && indexNumber <= 56) {
        row = 7;
        column = indexNumber - 49;
      }
      if (indexNumber >= 57 && indexNumber <= 64) {
        row = 8;
        column = indexNumber - 57;
      }
      if (column == 0) {
        columnLetter = 'a';
      }
      if (column == 1) {
        columnLetter = 'b';
      }
      if (column == 2) {
        columnLetter = 'c';
      }
      if (column == 3) {
        columnLetter = 'd';
      }
      if (column == 4) {
        columnLetter = 'e';
      }
      if (column == 5) {
        columnLetter = 'f';
      }
      if (column == 6) {
        columnLetter = 'g';
      }
      if (column == 7) {
        columnLetter = 'h';
      }

      log.debug("" + columnLetter + " row " + row);
      hmoveMsg += ("" + columnLetter) + row;

      /*
       * arduinoPort.write(columnLetter); arduinoPort.write(row);
       * print(columnLetter); print(row);
       */

      if (pressedAmount == 3) {
        log.debug("sending to inputMove from touchOSC " + hmoveMsg);
        invoke("inputMove", hmoveMsg);
        hmoveMsg = "";
      }
    }

    if (pressedAmount == 4) {
      pressedAmount = 0;
      hmoveMsg = "";
    }

    return hmoveMsg;
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

    ServiceType meta = new ServiceType(ChessGame.class.getCanonicalName());
    meta.addDescription("interface to a Chess game");
    meta.addCategory("game");
    meta.addDependency("org.op.chess", "1.0.0");
    return meta;
  }

}
