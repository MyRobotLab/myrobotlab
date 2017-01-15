package org.myrobotlab.arduino;

import org.myrobotlab.service.Arduino;

/**
 * BoardInfo is all info which needs to be published only once after connection
 */
public class BoardInfo {

  Integer boardId;
  Integer version;
  String boardName;
  boolean valid = false;

  Integer sram;
  Integer microsPerLoop;
  Integer activePins;
  DeviceSummary[] deviceSummary; // deviceList with types
  
  // if true it boardInfo will be published at a regular 1s interval
  boolean enableBoardInfo = false;
  public long heartbeatMs;

  public BoardInfo() {
    setType(-1);
  }

  public BoardInfo(int version, int boardId) {
    this.version = version;
    this.boardId = boardId;
  }

  public String getName() {
    return boardName;
  }

  public boolean isUnknown() {
    return (boardName == null) || boardName.equals("unknown");
  }

  public void setType(int boardId) {
    this.boardId = boardId;
    switch (boardId) {
      case Arduino.BOARD_TYPE_ID_MEGA:
        boardName = Arduino.BOARD_TYPE_MEGA;
        break;
      case Arduino.BOARD_TYPE_ID_UNO:
        boardName = Arduino.BOARD_TYPE_UNO;
        break;
      case Arduino.BOARD_TYPE_ID_ADK_MEGA:
        boardName = Arduino.BOARD_TYPE_MEGA_ADK;
        break;
      case Arduino.BOARD_TYPE_ID_NANO:
        boardName = Arduino.BOARD_TYPE_NANO;
        break;
      case Arduino.BOARD_TYPE_ID_PRO_MINI:
        boardName = Arduino.BOARD_TYPE_PRO_MINI;
        break;
      default:
        boardName = "unknown";
        break;
    }
  }

  /**
   * called on disconnect() so it can re-initalize if connected to a different
   * arduino
   */
  public void reset() {
    boardId = -1;
    version = null;
    boardName = null;
    valid = false;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    valid = true;
    this.version = version;
  }

  public boolean isValid() {
    return valid;
  }

  public void setType(String board) {
    boardName = board;
    if (Arduino.BOARD_TYPE_MEGA.equals(board)) {
      boardId = Arduino.BOARD_TYPE_ID_MEGA;
    } else if (Arduino.BOARD_TYPE_MEGA_ADK.equals(board)) {
      boardId = Arduino.BOARD_TYPE_ID_ADK_MEGA;
    } else if (Arduino.BOARD_TYPE_UNO.equals(board)) {
      boardId = Arduino.BOARD_TYPE_ID_UNO;
    } else if (Arduino.BOARD_TYPE_NANO.equals(board)) {
      boardId = Arduino.BOARD_TYPE_ID_NANO;
    } else if (Arduino.BOARD_TYPE_PRO_MINI.equals(board)) {
      boardId = Arduino.BOARD_TYPE_ID_PRO_MINI;
    } else {
      boardId = Arduino.BOARD_TYPE_ID_UNKNOWN;
    }
  }

  public int getBoardType() {
    return boardId;
  }

  public void setMicrosPerLoop(int microsPerLoop){
    this.microsPerLoop = microsPerLoop;
  }

  public void setSram(Integer sram) {
    this.sram = sram;
  }
  
  public void setDeviceSummary(DeviceSummary[] deviceSummary) {
    this.deviceSummary = deviceSummary;
  }

  public void setActivePins(Integer activePins) {
    this.activePins = activePins;
  }
  
}
