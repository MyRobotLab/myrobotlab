package org.myrobotlab.codec;



public class DecimalCodec implements Codec {

	String displayDelimiter =  " ";

	@Override
	public int[] encode(String source) throws CodecException {
		// TODO Auto-generated method stub
		return new int[0];
	}

	@Override
	public String decode(int newByte) throws CodecException {
		return String.format("%03d%s", newByte, displayDelimiter);
	}

	@Override
	public String getCodecExt() {
		return "dec";
	}

}
