package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.service.data.PinData;

/**
 * 
 * @author GroG
 *
 */
public interface PinArrayControl extends PinArrayPublisher {

  public void attachPinListener(PinListener listener);

  public void detachPinListener(PinListener listener);

  public void disablePin(String pin);

  @Deprecated /* use disablePin(String pin) */
  public void disablePin(int address);

  public void disablePins();

  public void enablePin(String pin);

  public void enablePin(String pin, int rate);

  public PinDefinition getPin(String pin);

  public List<PinDefinition> getPinList();

  public void pinMode(String pin, String mode);

  public PinData publishPin(PinData pinData);

  public PinDefinition publishPinDefinition(PinDefinition pinDef);

  /**
   * read the pin value e.g. a = read("P0")
   * @param pin
   * @return
   */
  public int read(String pin);


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
  // public void write(int address, int state);

  Integer getAddress(String pin);

  public void attach(String name) throws Exception;

  public void detach(String name) throws Exception;

}
