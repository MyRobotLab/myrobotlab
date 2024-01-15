package org.myrobotlab.service.interfaces;

import org.myrobotlab.arduino.BoardInfo;
import org.myrobotlab.framework.interfaces.Invoker;
import org.myrobotlab.sensor.EncoderData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.data.SerialRelayData;

/**
 * defines the interface for callbacks by listening to an mrlcomm byte stream.
 * TODO: auto-generate this file from the schema.
 * 
 * @author kwatters
 *
 */
public interface MrlCommPublisher extends Invoker {

  public void onBytes(byte[] data);

  public BoardInfo publishBoardInfo(Integer version/* byte */,
      Integer boardTypeId/* byte */, Integer microsPerLoop/* b16 */,
      Integer sram/* b16 */, Integer activePins, int[] deviceSummary/* [] */);

  public void publishAck(Integer function/* byte */);

  public int[] publishCustomMsg(int[] msg);

  public String publishDebug(String debugMsg);

  public void publishEcho(float myFloat, int myByte, float secondFloat);

  public EncoderData publishEncoderData(Integer deviceId, Integer position);

  public void publishI2cData(Integer deviceId, int[] data);

  public SerialRelayData publishSerialData(Integer deviceId, int[] data);

  public Integer publishServoEvent(Integer deviceId, Integer eventType, Integer currentPos, Integer targetPos);

  public void publishMrlCommBegin(Integer version);

  public String publishMRLCommError(String errorMsg);

  public PinData[] publishPinArray(int[] data);
  
  public boolean isConnecting();
  
  public String getName();

  public Integer publishUltrasonicSensorData(Integer deviceId, Integer echoTime);

  public void ackTimeout();


}
