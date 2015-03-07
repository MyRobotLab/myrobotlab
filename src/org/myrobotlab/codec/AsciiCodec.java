package org.myrobotlab.codec;



public class AsciiCodec implements Codec {

	@Override
	final public String decode(int data) {
		return String.format("%c", data & 0xff);
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
	public String decode(int[] msgs) throws CodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getKey() {
		return "ascii";
	}

}
