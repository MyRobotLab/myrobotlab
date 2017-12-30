package org.myrobotlab.framework;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.google.gson.Gson;

/**
 * JSON TypeConverter - used in general REST api to convert url JSON parameters
 * appropriately to hard types for method invoking used in WebGui and Cli
 * 
 * @author GroG
 *
 */
public class TypeConverter {

  public final static Logger log = LoggerFactory.getLogger(TypeConverter.class);

  private static Gson gson = new Gson();

  // Possible Optimization -> pointers to known method signatures -
  // optimization so that once a
  // method's signature is processed and
  // known conversion exists - it is saved
  static public HashMap<String, Method[]> knownMethodSignatureConverters = new HashMap<String, Method[]>();

  // pointers to conversion methods
  // static public HashMap<String, Method> conversions = new HashMap<String,
  // Method>();

  /**
   * this method tries to get the appropriate 'Typed parameter array for a
   * specific method It "converts" parameters of strings into typed parameters
   * which can then be used to reflectively invoke the appropriate method
   * @param clazz c
   * @param method m
   * @param stringParams p 
   * @return object array
   * @throws IOException e
   * 
   */
  static public Object[] getTypedParamsFromJson(Class<?> clazz, String method, String[] stringParams) throws IOException {

    // try {

    Method[] methods = clazz.getMethods();
    for (int i = 0; i < methods.length; ++i) {
      Method m = methods[i];
      Class<?>[] types = m.getParameterTypes();
      // TODO optimize getting name ??? why didn't Java reflect api
      // use a HashMap ???
      if (method.equals(m.getName()) && stringParams.length == types.length) {
        log.debug("method with same ordinal of params found {}.{} - building new converter", method, stringParams.length);

        try {
          Object[] newGSONTypedParamters = new Object[stringParams.length];

          for (int j = 0; j < types.length; ++j) {
            Class<?> pType = types[j];
            String param = stringParams[j];

            log.debug(String.format("attempting conversion into %s from inbound data %s", pType.getSimpleName(), stringParams[j]));
            if (pType == String.class) {
              // escape quotes
              param = param.replaceAll("\"", "\\\"");
              // add quotes
              param = String.format("\"%s\"", param);
            }
            newGSONTypedParamters[j] = gson.fromJson(param, pType);

          }

          log.debug("successfully converted all types");
          return newGSONTypedParamters;

        } catch (Exception e) {
          // Logging.logException(e);
          log.warn("could not match type from inbound data");
          continue;
        }

      } // if name and ordinal match
    } // through each method

    String error = String.format("could not find or convert %s", method);
    log.error(error);

    /*
     * } catch (Exception e) { Logging.logError(e); }
     * 
     * return null;
     */
    throw new IOException(error);

  }

  public static void main(String[] args) {

    try {
      LoggingFactory.init(Level.DEBUG);

      /*
       * FIXME PUT IN JUNIT TEST !!
       * org.myrobotlab.service.Runtime.createAndStart("clock", "Clock");
       * 
       * ServiceInterface si =
       * org.myrobotlab.service.Runtime.getService("clock");
       * 
       * 
       * String stringParams[] = new String[] { "13", "1" }; String method =
       * "digitalWrite"; Class<?> clazz = si.getClass();
       * 
       * Object[] params = getTypedParamsFromJson(clazz, method, stringParams);
       * 
       * si.invoke(method, params);
       * 
       * log.info("here");
       * 
       * Object[] params2 = getTypedParamsFromJson(clazz, method, stringParams);
       * log.info("here");
       */
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  static public boolean StringToBoolean(String in) {
    return Boolean.parseBoolean(in);
  }

  // -------- primitive boxed types conversion begin ------------
  static public byte StringToByte(String in) {
    return Byte.parseByte(in);
  }

  static public char StringToChar(String in) {
    return in.charAt(0);
  }

  static public double StringToDouble(String in) {
    return Double.parseDouble(in);
  }

  static public float StringToFloat(String in) {
    return Float.parseFloat(in);
  }

  static public int StringToInteger(String in) {
    return Integer.parseInt(in);
  }

  static public long StringToLong(String in) {
    return Long.parseLong(in);
  }

  // -------- primitive boxed types conversion end ------------

  static public short StringToShort(String in) {
    return Short.parseShort(in);
  }

  /*
   * static public Object[] convert(String[] stringParams, Method[] converter) {
   * try { Object[] newTypedParams = new Object[stringParams.length]; for (int i
   * = 0; i < stringParams.length; ++i) { // static calls on conversion -
   * probably not thread safe newTypedParams[0] = converter[i].invoke(null,
   * stringParams[i]); }
   * 
   * return newTypedParams; } catch (Exception e) { Logging.logException(e); }
   * 
   * return null; }
   */

  static public String StringToString(String in) {
    return in;
  }

}
