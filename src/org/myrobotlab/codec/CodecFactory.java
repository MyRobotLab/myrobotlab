package org.myrobotlab.codec;

public class CodecFactory {
	
	//static public encodeMethodSignature()
	
	static public Codec getCodec(String clazz){
		return new CodecJson();
	}

}
