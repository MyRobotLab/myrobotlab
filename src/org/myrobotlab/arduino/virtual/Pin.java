package org.myrobotlab.arduino.virtual;

public class Pin {
  
//Pin Types must be in sync
//with Arduino.getMrlPinType

public final static int DIGITAL =        0;
public final static int ANALOG  =        1;
// pin mode
public final static int INPUT =  0;
public final static int OUTPUT = 1;

  public Pin(int address, int type, Integer rate) {
    this.address = address;
    this.type = type;
    this.rate = rate;
  }

  public Pin(int address, int type, int rate) {
    this.address = address;
    this.type = type;
    this.rate = rate;
  }

  public int rate;
  public long lastUpdate;
  public int type;
  public int value;
  public int address;
}

