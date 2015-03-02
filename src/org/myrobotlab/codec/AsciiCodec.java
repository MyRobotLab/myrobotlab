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
		return "asc";
	}

}
