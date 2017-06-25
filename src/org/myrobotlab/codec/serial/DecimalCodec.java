package org.myrobotlab.codec.serial;

import org.myrobotlab.framework.interfaces.LoggingSink;

public class DecimalCodec extends Codec {

  String columnDelimiter = " ";
  String rowDelimiter = "\n";
  int byteCount = 0;
  int width = 16;

  public DecimalCodec(LoggingSink myService) {
    super(myService);
  }

  @Override
  public String decodeImpl(int newByte) {
    ++byteCount;
    String ret = String.format("%03d%s%s", 0xff & newByte, columnDelimiter, ((byteCount % width == 0) ? rowDelimiter : ""));
    return ret;
  }

  @Override
  public String decode(int[] msgs) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int[] encode(String source) {
    // TODO Auto-generated method stub
    return new int[0];
  }

  @Override
  public String getCodecExt() {
    return getKey().substring(0, 3);
  }

  @Override
  public String getKey() {
    // TODO Auto-generated method stub
    return "decimal";
  }

}
