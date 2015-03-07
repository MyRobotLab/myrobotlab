package org.myrobotlab.codec;


public interface Codec {

	// TODO - use ByteBuffer for codecs - only concern is the level of Java supported
	// includin the level of Android OS - Android did not have a ByteBuffer until ??? version
	// TODO - possibly model after the apache codec / encoder / decoder design
	/*
	Object encode(Object source) throws CodecException;
	
	Object decode(Object source) throws CodecException;
	*/
	
	public String decode(int[] msgs) throws CodecException;
	
	public int[] encode(String source) throws CodecException;
	
	String decode(int newByte) throws CodecException;
	
	public String getKey();
	
	public String getCodecExt();
}
