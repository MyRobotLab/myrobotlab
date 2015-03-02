package org.myrobotlab.codec;

public interface Codec {

	// TODO - possibly model after the apache codec / encoder / decoder design
	/*
	Object encode(Object source) throws CodecException;
	
	Object decode(Object source) throws CodecException;
	*/
	
	int[] encode(String source) throws CodecException;
	
	String decode(int newByte) throws CodecException;
	
	String getCodecExt();
}
