package org.myrobotlab.codec;

public class CodecException extends Exception {

	private static final long serialVersionUID = 1L;

	public CodecException(String msg) {
		super(msg);
	}
	
	public CodecException(Throwable msg) {
		super(msg);
	}
}
