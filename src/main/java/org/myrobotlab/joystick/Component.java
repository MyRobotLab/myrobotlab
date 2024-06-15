package org.myrobotlab.joystick;

import java.io.Serializable;

import org.myrobotlab.framework.interfaces.NameProvider;

public class Component implements Serializable, NameProvider {
  private static final long serialVersionUID = 1L;
  public String id;
  public boolean isRelative = false;
  public boolean isAnalog = false;
  public String type;
  public int index;
  public float value = 0; // last value of hardware (virtual or non-virtual)
  public float virtualValue = 0; // virtual hardware value
  String serviceName;
  transient private net.java.games.input.Component jinputComponent;

  public Component(String serviceName, int index, net.java.games.input.Component c) {

    this.serviceName = serviceName;
    this.index = index;
    this.isRelative = c.isRelative();
    this.isAnalog = c.isAnalog();
    this.type = c.getIdentifier().getClass().getSimpleName();
    this.id = c.getIdentifier().toString();
    this.jinputComponent = c;
  }

  @Override
  public String toString() {
    return String.format("%d %s [%s] relative %b analog %b", index, type, id, isRelative, isAnalog);
  }

  @Override
  public String getName() {
    return serviceName;
  }

  // FIXME - configurations to handle virtual/real/and mesh of the two ????
  public float getPollData() {
    // FIXME getDeadZone ??
    if (jinputComponent != null) {
      // value = jinputComponent.getPollData(); <- heh - this borked everything
      // ! :) cuz abs(value - component.value) :P
      return jinputComponent.getPollData();
    } // else
      // FIXME - handle virtual input
    return virtualValue;
  }

  public String getIdentifier() {
    return id;
    // FIXME - handle virtual
    /*
     * if (jinputComponent != null) { return jinputComponent.getIdentifier() }
     */
  }

  public String getType() {
    return type;
  }

  public void setVirtualValue(double value) {
    this.virtualValue = (float) value; // jinput uses floats :( bleh
  }
}
