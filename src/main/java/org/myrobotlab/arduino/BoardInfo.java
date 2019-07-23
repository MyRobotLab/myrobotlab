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

  Integer version;
  Integer boardTypeId;
  Integer microsPerLoop;
  Integer sram;
  Integer activePins;
  DeviceSummary[] deviceSummary; // deviceList with types
  
  String boardTypeName;

  long heartbeatMs;
  long receiveTs;

  public BoardInfo(Integer version, Integer boardTypeId, String boardTypeName, Integer microsPerLoop, Integer sram, Integer activePins, DeviceSummary[] deviceSummary, long boardInfoRequestTs) {
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
  public String toString() {
    if (version != null) {      
      return String.format("version %s load %d heartbeat %d sram %d devices %d recvTs %d", version, microsPerLoop, heartbeatMs, sram, (deviceSummary != null) ? deviceSummary.length : 0, receiveTs);
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
