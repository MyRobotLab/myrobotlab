package org.myrobotlab.arduino;

import java.io.Serializable;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * BoardInfo is all info which needs to be published only once after connection
 * 
 * FIXME - make a BoardInfo that has more generic information in common with many board types
 * 
 */
public class BoardInfo implements Serializable {

  private static final long serialVersionUID = 1L;
  transient public final static Logger log = LoggerFactory.getLogger(BoardInfo.class);

  /**
   * version of firmware
   */
  public Integer version;
  
  /**
   * id of board type - FIXME change to string
   */
  public Integer boardTypeId;
  
  /**
   * Number of microseconds arduino uses to pass through a
   * control loop in MrlComm - very Arduino/MrlComm specific
   * FIXME - make generalized BoardType that can report useful information
   * from any type of board with pins
   */
  public Integer microsPerLoop;
  
  /**
   * 
   */
  public Integer sram;
  public Integer activePins;
  public DeviceSummary[] deviceSummary; // deviceList with types

  public String boardTypeName;

  public long heartbeatMs;
  public long receiveTs;
  
  public BoardInfo() {    
  }

  public BoardInfo(Integer version, Integer boardTypeId, String boardTypeName, Integer microsPerLoop, Integer sram, Integer activePins, DeviceSummary[] deviceSummary,
      long boardInfoRequestTs) {
    this.version = version;
    this.boardTypeId = boardTypeId;
    this.microsPerLoop = microsPerLoop;
    this.sram = sram;
    this.activePins = activePins;
    this.deviceSummary = deviceSummary;

    long now = System.currentTimeMillis();
    this.boardTypeName = boardTypeName;
    this.receiveTs = now;
    this.heartbeatMs = now - boardInfoRequestTs;
  }

  public Integer getVersion() {
    return version;
  }

  @Override
  public String toString() {
    if (version != null) {
      return String.format("version %s load %d heartbeat %d sram %d devices %d recvTs %d", version, microsPerLoop, heartbeatMs, sram,
          (deviceSummary != null) ? deviceSummary.length : 0, receiveTs);
    } else {
      return "unknown";
    }
  }

  public DeviceSummary[] getDeviceSummary() {
    return deviceSummary;
  }

  public String getBoardTypeName() {
    return boardTypeName;
  }

  public long getReceiveTs() {
    return receiveTs;
  }

}
