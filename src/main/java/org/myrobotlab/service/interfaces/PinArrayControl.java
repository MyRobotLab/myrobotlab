package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.service.data.PinData;

/**
 * 
 * @author GroG
 *
 */
public interface PinArrayControl extends PinArrayPublisher {

  @Deprecated /* use attach(String) or attachPinListener(PinListener) */
  public void attachPinListener(PinListener listener, int address);

  @Deprecated /* use attach(String) */
  public void attach(PinListener listener, String pin);

  public void disablePin(String pin);

  public void disablePin(int address);

  public void disablePins();

  public void enablePin(String pin);

  public void enablePin(int address);

  public void enablePin(String pin, int rate);

  public void enablePin(int address, int rate);

  public PinDefinition getPin(String pin);

  public PinDefinition getPin(int address);

  public List<PinDefinition> getPinList();

  public void pinMode(String pin, String mode);

  public void pinMode(int address, String mode);

  public PinData publishPin(PinData pinData);

  public PinDefinition publishPinDefinition(PinDefinition pinDef);

  /**
   * read the pin value e.g. a = read("P0")
   * @param pin
   * @return
   */
  public int read(String pin);

  /**
   * read the address location a = read(20)
   * @param address
   * @return
   */
  public int read(int address);

  /**
   * write to the pin e.g. write("P0", 1)
   * @param pin
   * @param state
   */
  public void write(String pin, int state);

  /**
   * write to the address e.g. write(20, 1)
   * @param address
   * @param state
   */
  public void write(int address, int state);

  Integer getAddress(String pin);

  public void attach(String name) throws Exception;

  public void detach(String name) throws Exception;

}
