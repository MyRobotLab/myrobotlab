package org.myrobotlab.codec;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class CodecFactory {

  // static public encodeMethodSignature()

  static final private HashMap<String, String> mimeTypeMap = new HashMap<String, String>();
  static private boolean initialized = false;

  static public synchronized void init() {
    if (!initialized) {
      mimeTypeMap.put("application/json", "org.myrobotlab.codec.CodecJson");
      mimeTypeMap.put("application/mrl-json", "org.myrobotlab.codec.CodecMessage");
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
      clazz = CodecUtils.MIME_TYPE_MESSAGES;
    }

    Class<?> o = Class.forName(clazz);
    Constructor<?> constructor = o.getConstructor();
    Codec codec = (Codec) constructor.newInstance();
    // return new CodecJson();
    return codec;
  }

  /*
   * static public Codec getCodec(String clazz, Object... params) throws
   * ClassNotFoundException, NoSuchMethodException, SecurityException,
   * InstantiationException, IllegalAccessException, IllegalArgumentException,
   * InvocationTargetException{ Class<?>[] parameterTypes = null; if (params !=
   * null){ parameterTypes = new Class<?>[params.length]; for (int i = 0; i <
   * params.length; ++i){ parameterTypes[i] = params[i].getClass(); } } Class<?>
   * o = Class.forName(clazz); Constructor<?> constructor =
   * o.getConstructor(parameterTypes); Codec codec =
   * (Codec)constructor.newInstance(params); //return new CodecJson(); return
   * codec; }
   */
}
