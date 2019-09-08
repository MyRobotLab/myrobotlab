package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.PinData;

/**
 * 
 * @author GroG
 *
 */
public interface PinArrayControl extends PinArrayPublisher {

  public void attach(PinListener listener, int address);
  
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

  public int read(String pin);
  
  public int read(int address);

  public void write(String pin, int value);
  
  public void write(int address, int value);

  Integer getAddress(String pin);

}
