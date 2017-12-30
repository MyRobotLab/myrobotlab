package org.myrobotlab.arduino;

import java.io.Serializable;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * BoardInfo is all info which needs to be published only once after connection
 */
public class BoardInfo implements Serializable {

	private static final long serialVersionUID = 1L;
	transient public final static Logger log = LoggerFactory.getLogger(BoardInfo.class);
	
	 // requested by the user
  // String boardName;
  
	// reported from the board
	// Integer boardId;
	Integer version;

	// FIXME - isUserSet isBoardSet isLoaded - BoardType as a class ?? - to String give {board}name ?
	// boolean valid = false;

	// dynamic changing values
	Integer sram;
	Integer microsPerLoop;
	Integer activePins;
	DeviceSummary[] deviceSummary; // deviceList with types
	

	// if true boardInfo will be published at a regular 1s interval
	boolean enableBoardInfo = false;
	public long heartbeatMs;

	public BoardInfo() {
		// setType(-1);
	}

	public BoardInfo(int version) {
		this.version = version;
		// this.boardId = boardId;
	}

	/*
	public String getName() {
		return boardName;
	}
	*/
	/*

	public boolean isUnknown() {
		return (boardName == null) || boardName.equals("unknown");
	}
	*/
	/*

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
	*/

	/**
	 * called on disconnect() so it can re-initalize if connected to a different
	 * arduino
	 */
	public void reset() {
		// boardId = -1;
		version = null;
		// boardName = null;
		// valid = false;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		// valid = true;
		this.version = version;
	}

	/*
	public boolean isValid() {
		return valid;
	}
	*/

	/*
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
  */
	/*
	public int getBoardType() {
		return boardId;
	}
	*/

	public void setMicrosPerLoop(int microsPerLoop) {
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

	public String toString() {
		if (version != null) {
		  /*
			return String.format("%s version %s load %d heartbeat %d sram %d devices %d", boardName, version, microsPerLoop, heartbeatMs, sram,
					(deviceSummary != null) ? deviceSummary.length : 0);
					*/
		  return String.format("version %s load %d heartbeat %d sram %d devices %d", version, microsPerLoop, heartbeatMs, sram,
          (deviceSummary != null) ? deviceSummary.length : 0);
		} else {
			return "unknown";
		}
	}
	
}
