package org.myrobotlab.codec;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class CodecFactory {

  static final private HashMap<String, String> mimeTypeMap = new HashMap<String, String>();
  static final private HashMap<String, Codec> codecMap = new HashMap<String, Codec>();
  static private boolean initialized = false;

  static public synchronized void init() {
    if (!initialized) {
      mimeTypeMap.put("application/json", "org.myrobotlab.codec.CodecJson"); // vs application/mrl-json
      initialized = true;
    }

  }

  static public Codec getCodec(String mimeType)
      throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    if (!initialized) {
      init();
    }
    String clazz = null;
    if (mimeTypeMap.containsKey(mimeType)) {
      clazz = mimeTypeMap.get(mimeType);
    } else {
      clazz = CodecUtils.MIME_TYPE_JSON;
    }

    if (codecMap.containsKey(mimeType)) {
      return codecMap.get(mimeType);
    } else {
      Class<?> o = Class.forName(clazz);
      Constructor<?> constructor = o.getConstructor();
      Codec codec = (Codec) constructor.newInstance();
      codecMap.put(mimeType, codec);
      return codec;
    }
  }

}
