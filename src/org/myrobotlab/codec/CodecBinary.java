package org.myrobotlab.codec;

public interface CodecBinary extends Codec {
	byte[] encode(byte[] source) throws CodecException;

	byte[] decode(byte[] source) throws CodecException;
}
