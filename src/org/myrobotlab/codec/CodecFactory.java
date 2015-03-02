package org.myrobotlab.codec;

import java.util.Properties;



public class CodecFactory {

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
	
	static public Codec getDecoder(String key) throws ClassNotFoundException, InstantiationException, IllegalAccessException{
		if (key == null){
			return null;
		}
		String fullTypeName = String.format("org.myrobotlab.codec.%sCodec", keyToType.getProperty(key.toLowerCase(), "decimal"));
		Class<?> clazz = Class.forName(fullTypeName);
		Codec formatter = (Codec)clazz.newInstance();
		return formatter;
	}
	
}
