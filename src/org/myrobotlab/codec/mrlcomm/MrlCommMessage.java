package org.myrobotlab.codec.mrlcomm;

public class MrlCommMessage {
  // TODO when an mrl message is received from arduino. cast it to this message
  // class
  // and use that in the code. (it'll be cleaner.)
  private final int function;
  private final int[] payload;

  public MrlCommMessage(int function, int[] payload) {
    super();
    this.function = function;
    this.payload = payload;
  }

  // TODO add a bunch of helper functions that take care of the casting and bit
  // masking
  // for typical usage patterns. (as we discover what those are.)
  public int getFunction() {
    return function;
  }

  public int[] getPayload() {
    return payload;
  }

  public int getPayloadSize() {
    return payload.length;
  }

  public boolean hasPayload() {
    if (payload.length > 0) {
      return true;
    } else {
      return false;
    }
  }

}
