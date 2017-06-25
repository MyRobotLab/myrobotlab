package org.myrobotlab.codec.serial;

import org.myrobotlab.framework.interfaces.LoggingSink;

public class AsciiCodec extends Codec {

  public AsciiCodec(LoggingSink myService) {
    super(myService);
  }

  @Override
  final public String decodeImpl(int data) {
    return String.format("%c", data & 0xff);
  }

  @Override
  public String decode(int[] msgs) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int[] encode(String data) {
    // TODO Auto-generated method stub
    return new int[0];
  }

  @Override
  public String getCodecExt() {
    return getKey().substring(0, 3);
  }

  @Override
  public String getKey() {
    return "ascii";
  }

}
