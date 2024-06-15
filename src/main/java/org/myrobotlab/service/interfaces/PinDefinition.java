package org.myrobotlab.service.interfaces;

import java.io.Serializable;

public class PinDefinition implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * label or name of the pin e.g. P0, A5, D1, D2, GPIO 2, etc...
   */
  String pin;

  /**
   * the address of the pin
   */
  Integer address;

  /**
   * means actively reading
   */
  boolean enabled = false;

  /**
   * pin mode INPUT or OUTPUT, other...
   */
  String mode;
  
  public String serviceName;

  /**
   * statistics
   */
  int totalSamples;
  int min;
  int max;
  int avg;

  /**
   * if the pin is capable of analog values
   */
  boolean isAnalog = false;

  /**
   * if the pin is capable of pwm
   */
  boolean isPwm = false;

  boolean isDigital = true;

  boolean isRx = false;

  boolean isTx = false;

  public boolean isSda() {
    return isSda;
  }

  public void setSda(boolean isSda) {
    this.isSda = isSda;
  }

  public boolean isScl() {
    return isScl;
  }

  public void setScl(boolean isScl) {
    this.isScl = isScl;
  }

  boolean isSda = false;

  boolean isScl = false;

  boolean canRead = true;

  boolean canWrite = true;

  /**
   * the last read value of the pin
   */
  Integer value;

  /**
   * the last written value of the pin
   */
  Integer state;

  transient Object pinImpl;

  /**
   * rate in Hz for which the pin will be polled 0 == no rate imposed
   */
  int pollRateHz = 0;
  
  public PinDefinition() {
  }
  

  public PinDefinition(String serviceName, int address, String pin) {
    this.serviceName = serviceName;
    this.address = address;
    this.pin = pin;
  }

  public PinDefinition(String serviceName, int address) {
    this(serviceName, address, String.format("%d", address));
  }

  public Integer getValue() {
    return value.intValue();
  }

  public void setValue(int value) {
    this.value = value;
  }

  public Integer getState() {
    return state.intValue();
  }

  public void setState(int value) {
    this.state = value;
  }

  public String getPinName() {
    return pin;
  }

  public Integer getAddress() {
    return address;
  }

  public boolean isAnalog() {
    return isAnalog;
  }

  public boolean isDigital() {
    return isDigital;
  }

  public boolean isPwm() {
    return isPwm;
  }

  public void setName(int i) {
    pin = String.format("%d", i);
  }

  public void setPinName(String pin) {
    this.pin = pin;
  }

  public void setAnalog(boolean b) {
    isAnalog = b;
  }

  public void setDigital(boolean b) {
    isDigital = b;
  }

  public void setAddress(int index) {
    this.address = index;
  }

  public void setPwm(boolean b) {
    isPwm = b;
  }

  public void setPinImpl(Object b) {
    pinImpl = b;
  }

  public Object getPinImpl() {
    return pinImpl;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("pin def ");
    sb.append(pin);
    sb.append(" ");
    sb.append(address);
    sb.append(" ");
    if (isPwm) {
      sb.append("isPwm ");
    }
    if (isAnalog) {
      sb.append("isAnalog ");
    }
    if (isDigital) {
      sb.append("isDigital ");
    }
    if (isTx) {
      sb.append("isTx ");
    }
    if (isRx) {
      sb.append("isRx ");
    }

    if (value != null) {
      sb.append("value ");
      sb.append(value);
    }
    return sb.toString();
  }

  public void setRx(boolean b) {
    isRx = true;
  }

  public boolean isRx() {
    return isRx;
  }

  public void setTx(boolean b) {
    isTx = true;
  }

  public boolean isTx() {
    return isTx;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public boolean canRead() {
    return canRead;
  }

  public boolean canWrite() {
    return canWrite;
  }

  public void canWrite(boolean canWrite) {
    this.canWrite = canWrite;
  }

  public void canRead(boolean canRead) {
    this.canRead = canRead;
  }

  public void setPollRate(int rateHz) {
    this.pollRateHz = rateHz;
  }

  public int getPollRate() {
    return pollRateHz;
  }

  public String getPin() {
    return pin;
  }
  
  public void setPin(String pin) {
    this.pin = pin;
  }

}
