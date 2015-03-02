package org.myrobotlab.codec;

public interface CodecIntString extends Codec {
	
	int[] encode(String source) throws CodecException;
	
	String decode(int newByte) throws CodecException;
}
