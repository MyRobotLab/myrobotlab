package org.myrobotlab.codec;

public class CodecException extends Exception {

	private static final long serialVersionUID = 1L;

	public CodecException(String msg) {
		super(String.format(msg));
	}
	public CodecException(String msg, Object... params) {
		super(String.format(msg, params));
	}
	
	public CodecException(Throwable msg) {
		super(msg);
	}
}
