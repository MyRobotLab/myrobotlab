package org.myrobotlab.codec;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.LoggingSink;
import org.slf4j.Logger;

public abstract class Codec {

	public final static Logger log = LoggerFactory.getLogger(Codec.class);

	// TODO - use ByteBuffer for codecs - only concern is the level of Java
	// supported
	// including the level of Android OS - Android did not have a ByteBuffer
	// until ??? version
	// TODO - possibly model after the apache codec / encoder / decoder design
	/*
	 * Object encode(Object source) ;
	 * 
	 * Object decode(Object source) ;
	 */
	
	public Codec(){
	};
	
	public Codec(LoggingSink sink){
		this.sink = sink;
	};
	
	LoggingSink sink = null;
	
	static Properties keyToType = new Properties();

	static {

		keyToType.put("asc", "Ascii");
		keyToType.put("ascii", "Ascii");
		keyToType.put("hex", "Hex");
		keyToType.put("dec", "Decimal");
		keyToType.put("decimal", "Decimal");
		keyToType.put("ard", "ArduinoMsg");
		keyToType.put("arduino", "ArduinoMsg");
	}

	// FIXME - register - each codec dynamically registers

	static public Codec getDecoder(String key, LoggingSink sink) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException {
		if (key == null) {
			return null;
		}
		String fullTypeName = String.format("org.myrobotlab.codec.%sCodec", keyToType.getProperty(key.toLowerCase(), "decimal"));
		
		Codec formatter = null;
		Class<?> clazz = Class.forName(fullTypeName);
		if (sink == null){
			formatter = (Codec) clazz.newInstance();
		} else {
			Constructor<?> c = clazz.getConstructor(new Class[]{LoggingSink.class});
			formatter = (Codec) c.newInstance(sink);
		}
		return formatter;
	}

	
	abstract public String decode(int newByte) ;

	abstract public String decode(int[] msgs) ;

	abstract public int[] encode(String source) ;

	abstract public String getCodecExt();

	abstract public String getKey();
	
	public void error(String format, Object... args){
		if (sink != null){
			sink.error(format, args);
		} else {
			log.error(String.format(format, args));
		}
	}
}
