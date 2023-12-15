package org.myrobotlab.codec;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.myrobotlab.codec.json.JacksonPolymorphicModule;
import org.myrobotlab.codec.json.JacksonPrettyPrinter;
import org.myrobotlab.codec.json.JsonDeserializationException;
import org.myrobotlab.codec.json.JsonSerializationException;
import org.myrobotlab.codec.json.ProxySerializer;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodCache;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.StaticType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.config.ServiceConfig;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.noctordeser.NoCtorDeserModule;

/**
 * handles all encoding and decoding of MRL messages or api(s) assumed context -
 * services can add an assumed context as a prefix
 * /api/returnEncoding/inputEncoding/service/method/param1/param2/ ...
 * <p>
 * xmpp for example assumes (/api/string/json)/service/method/param1/param2/ ...
 * <p>
 * scheme = alpha *( alpha | digit | "+" | "-" | "." ) Components of all URIs: [
 * &lt;scheme&gt;:]&lt;scheme-specific-part&gt;[#&lt;fragment&gt;]
 * <p>
 * branch API test 5
 *
 * @see <a href=
 *      "http://stackoverflow.com/questions/3641722/valid-characters-for-uri-schemes">Valid
 *      characters for URI schemes</a>
 */
public class CodecUtils {

  public final static Logger log = LoggerFactory.getLogger(CodecUtils.class);
  /**
   * The string to be used to specify the API in URIs, with leading and trailing
   * slash.
   */
  public final static String PARAMETER_API = "/api/";
  /**
   * The string to be used in URIs to specify the API
   */
  public final static String PREFIX_API = "api";
  /**
   * The MIME type used to specify JSON data.
   *
   * @see <a href=
   *      "https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types">MIME
   *      types</a>
   */
  public final static String MIME_TYPE_JSON = "application/json";

  /**
   * The key used to locate type information in a JSON dictionary. This is used
   * to serialize type information into the JSON and to deserialize JSON into
   * the correct type.
   */
  public static final String CLASS_META_KEY = "class";
  /**
   * Set of all known wrapper types, which are classes that correspond to Java
   * primitives (plus {@link Void}).
   *
   * @see <a href="https://www.w3schools.com/java/java_wrapper_classes.asp">Java
   *      Wrapper Classes</a>
   */
  public static final Set<Class<?>> WRAPPER_TYPES = new HashSet<>(
      Arrays.asList(Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class,
          Double.class, Void.class));
  /**
   * Set of the fully-qualified (AKA canonical) names of {@link #WRAPPER_TYPES}.
   *
   * @see <a href="https://www.w3schools.com/java/java_wrapper_classes.asp">Java
   *      Wrapper Classes</a>
   */
  public static final Set<String> WRAPPER_TYPES_CANONICAL = WRAPPER_TYPES.stream().map(Object::getClass)
      .map(Class::getCanonicalName).collect(Collectors.toSet());
  public static final String API_MESSAGES = "messages";
  public static final String API_SERVICE = "service";

  /**
   * The path from a top-level URL to the messages API endpoint.
   * <p>
   * </p>
   * FIXME This should be moved to WebGui, CodecUtils should have no knowledge
   * of URLs
   */
  public static final String API_MESSAGES_PATH = PARAMETER_API + API_MESSAGES;

  /**
   * The path from a top-level URL to the service API endpoint.
   * <p>
   * </p>
   * FIXME This should be moved to WebGui, CodecUtils should have no knowledge
   * of URLs
   */
  public static final String API_SERVICE_PATH = PARAMETER_API + API_SERVICE;
  /**
   * use {@link MethodCache}
   */
  @Deprecated
  final static HashMap<String, Method> methodCache = new HashMap<String, Method>();
  /**
   * a method signature map based on name and number of methods - the String[]
   * will be the keys into the methodCache A method key is generated by input
   * from some encoded protocol - the method key is object name + method name +
   * parameter number - this returns a full method signature key which is used
   * to look up the method in the methodCache
   */
  final static HashMap<String, ArrayList<Method>> methodOrdinal = new HashMap<String, ArrayList<Method>>();
  final static HashSet<String> objectsCached = new HashSet<String>();
  /**
   * Equivalent to {@link #MIME_TYPE_JSON}
   */
  @Deprecated
  static final String JSON = "application/javascript";

  /**
   * The type that Jackson uses when it attempts to deserialize without knowing
   * the target type, e.g. if the target is {@link Object} and no field matching
   * {@link #CLASS_META_KEY} is found.
   */
  private static final Class<?> JACKSON_DEFAULT_OBJECT_TYPE = LinkedHashMap.class;
  /**
   * The type that the chosen JSON backend uses when it attempts to deserialize
   * without knowing the target type, e.g. if the target is {@link Object} and
   * no field matching {@link #CLASS_META_KEY} is found.
   */
  public static final Class<?> JSON_DEFAULT_OBJECT_TYPE = JACKSON_DEFAULT_OBJECT_TYPE;

  /**
   * Default type for single parameter fromJson(String json), we initially
   * assume this type
   */
  public static final Class<?> DEFAULT_OBJECT_TYPE = LinkedHashMap.class;

  /**
   * The Jackson {@link ObjectMapper} used for JSON operations when the selected
   * backend is Jackson.
   *
   */
  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * The pretty printer to be used with {@link #mapper}
   */
  private static final PrettyPrinter jacksonPrettyPrinter = new JacksonPrettyPrinter();

  /**
   * The {@link TypeFactory} used to generate type information for
   * {@link #mapper} when the selected backend is Jackson.
   * <p>
   */
  private static final TypeFactory typeFactory = TypeFactory.defaultInstance();

  // Class initializer to setup mapper when the class is loaded
  static {
    // This allows Jackson to when no default constructor is
    // available
    mapper.registerModule(new NoCtorDeserModule());

    SimpleModule proxySerializerModule = new SimpleModule();
    proxySerializerModule.addSerializer(Proxy.class, new ProxySerializer());
    mapper.registerModule(proxySerializerModule);

    // Actually add our polymorphic support
    mapper.registerModule(JacksonPolymorphicModule.getPolymorphicModule());

    // Disables Jackson's automatic property detection
    mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    // mapper.setVisibility(PropertyAccessor.SETTER,
    // JsonAutoDetect.Visibility.PUBLIC_ONLY);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    // Make jackson behave such that unknown properties are ignored
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  }

  /**
   * Ensures a service type name is fully qualified. If the type name is short,
   * it will assume the type exists in the {@code org.myrobotlab.service}
   * package.
   *
   * @param type
   *             The service type name, either shortened or fully qualified.
   * @return Null if type is null, otherwise fully qualified name.
   */
  public static String makeFullTypeName(String type) {
    if (type == null) {
      return null;
    }
    if (!type.contains(".")) {
      return ("Service".equals(type)) ? "org.myrobotlab.framework.Service"
          : String.format("org.myrobotlab.service.%s", type);
    }
    return type;
  }

  /**
   * Capitalize the first character of the given string
   *
   * @param line
   *             The string to be capitalized
   * @return The capitalized version of line.
   */
  public static String capitalize(final String line) {
    return Character.toUpperCase(line.charAt(0)) + line.substring(1);
  }

  /**
   * Deserializes a JSON string into the target object (or subclass of if
   * {@link #CLASS_META_KEY} exists) using the selected JSON backend.
   *
   * @param json
   *              The JSON to be deserialized in String form
   * @param clazz
   *              The target class. If a class is not supplied the default class
   *              returned will be an {@link #DEFAULT_OBJECT_TYPE}
   * @param <T>
   *              The type of the target class.
   * @return An object of the specified class (or a subclass of) with the state
   *         given by the json. Null is an allowed return object.
   * @throws JsonDeserializationException
   *                                      if an error during deserialization
   *                                      occurs.
   */
  @SuppressWarnings("unchecked")
  public static <T> /* @Nullable */ T fromJson(/* @Nonnull */ String json,
      /* @Nonnull */ Class<T> clazz) {
    try {
      if (clazz == null) {

        JsonParser parser = mapper.getFactory().createParser(json);

        // "peek" at the next token to determine its type
        JsonToken token = parser.nextToken();

        if (token == JsonToken.START_OBJECT) {
          clazz = (Class<T>) Map.class;
        } else if (token == JsonToken.START_ARRAY) {
          clazz = (Class<T>) ArrayList.class;
        } else if (token.isScalarValue()) {
          JsonNode node = mapper.readTree(json);
          if (node.isInt()) {
            return mapper.readValue(json, (Class<T>) Integer.class);
          } else if (node.isBoolean()) {
            return mapper.readValue(json, (Class<T>) Boolean.class);
          } else if (node.isNumber()) {
            return mapper.readValue(json, (Class<T>) Double.class);
          } else if (node.isTextual()) {
            return mapper.readValue(json, (Class<T>) String.class);
          }
        } else {
          log.error("could not derive type from peeking json {}", json);
        }

        parser.close();

      }
      return mapper.readValue(json, clazz);
    } catch (Exception e) {
      throw new JsonDeserializationException(e);
    }
  }

  /**
   * Deserializes a JSON string into the target object (or subclass of if
   * {@link #CLASS_META_KEY} exists) using the selected JSON backend.
   *
   * @param json
   *                      The JSON to be deserialized in String form
   * @param genericClass
   *                      The target class.
   * @param parameterized
   *                      The list of types used as the genericClass type
   *                      parameters of
   *                      genericClass.
   * @param <T>
   *                      The type of the target class.
   * @return An object of the specified class (or a subclass of) with the state
   *         given by the json. Null is an allowed return object.
   * @throws JsonDeserializationException
   *                                      if an error during deserialization
   *                                      occurs.
   */
  public static <T> /* @Nullable */ T fromJson(/* @Nonnull */ String json,
      /* @Nonnull */ Class<?> genericClass, /* @Nonnull */ Class<?>... parameterized) {
    try {
      return mapper.readValue(json, typeFactory.constructParametricType(genericClass, parameterized));
    } catch (Exception e) {
      throw new JsonDeserializationException(e);
    }
  }

  /**
   * Deserializes a json string into the type represented by {@link T}.
   * {@code type} must must match {@link T} exactly, otherwise the deserializers
   * may not deserialize into T.
   *
   * @param json
   *             A string encoded in JSON
   * @param type
   *             Reified type information to pass to the deserializers
   * @return An instance of T decoded from the json
   * @param <T>
   *            The type to deserialize into
   * @throws JsonDeserializationException
   *                                      if the selected deserializer throws an
   *                                      exception
   */
  public static <T> /* @Nullable */ T fromJson(/* @NonNull */ String json,
      /* @NonNull */ StaticType<T> type) {
    return fromJson(json, type.getType());
  }

  /**
   * Deserializes a JSON string into the target object (or subclass of if
   * {@link #CLASS_META_KEY} exists) using the selected JSON backend.
   *
   * @param json
   *             The JSON to be deserialized in String form
   * @param type
   *             The target type.
   * @param <T>
   *             The type of the target class.
   * @return An object of the specified class (or a subclass of) with the state
   *         given by the json. Null is an allowed return object.
   * @throws JsonDeserializationException
   *                                      if an error during deserialization
   *                                      occurs.
   */
  public static <T> /* @Nullable */ T fromJson(/* @Nonnull */ String json,
      /* @Nonnull */ Type type) {
    try {
      return mapper.readValue(json, typeFactory.constructType(type));
    } catch (Exception e) {
      throw new JsonDeserializationException(e);
    }
  }

  /**
   * Convert the given JSON string into an equivalent tree map.
   *
   * @param json
   *             The json to be converted
   * @return The json in a tree map form
   * @throws JsonDeserializationException
   *                                      if deserialization fails
   */
  @SuppressWarnings("unchecked")
  public static LinkedHashMap<String, Object> toTree(String json) {
    try {
      return (LinkedHashMap<String, Object>) mapper.readValue(json, LinkedHashMap.class);
    } catch (Exception e) {
      throw new JsonDeserializationException(e);
    }
  }

  public static Type getType(final Class<?> rawClass, final Class<?>... parameterClasses) {
    return new ParameterizedType() {
      @Override
      public Type[] getActualTypeArguments() {
        return parameterClasses;
      }

      @Override
      public Type getRawType() {
        return rawClass;
      }

      @Override
      public Type getOwnerType() {
        return null;
      }

    };
  }

  static public byte[] getBytes(Object o) throws IOException {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
    ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(byteStream));
    os.flush();
    os.writeObject(o);
    os.flush();
    return byteStream.toByteArray();
  }

  /**
   * Gets the short name of a service. A short name is the name of the service
   * without any runtime IDs, meaning no '@' signs.
   *
   * @param name
   *             The service name to be converted
   * @return The simple name of the service. If null, will return null, and if
   *         already a simple name then will return name
   */
  static public String getShortName(String name) {
    if (name == null) {
      return null;
    }
    if (name.contains("@")) {
      return name.substring(0, name.indexOf("@"));
    } else {
      return name;
    }
  }

  // TODO
  // public static Object encode(Object, encoding) - dispatches appropriately
  // should be simple using an enum and map to a new Encoder functional
  // interface

  /**
   * Gets the instance id from a service name
   *
   * @param name
   *             the name of the instance
   * @return the name of the instance
   */
  static public String getId(String name) {
    if (name == null) {
      return null;
    }
    if (name.contains("@")) {
      return name.substring(name.lastIndexOf("@") + 1);
    } else {
      return null;
    }
  }

  /**
   * Normalizes a service name to its full name, if not already. A full name
   * consists of two parts: the short name that identifies the service in the
   * context of its own runtime, and the Runtime ID, that identifies the
   * service's runtime from others in the network. The two parts are separated
   * by an {@code @} symbol.
   * <p>
   * If this method is given a short name, it is assumed to be local to this
   * runtime, and it is normalized with the ID of this runtime. If the name is
   * already a full name, then it is returned unmodified.
   * 
   * @param name
   *             The service name to normalize
   * @return The normalized (full) name, or null if name is null
   */
  public static String getFullName(String name) {
    if (name == null) {
      return null;
    }

    if (getId(name) == null) {
      return name + '@' + Platform.getLocalInstance().getId();
    } else {
      return name;
    }
  }

  /**
   * Checks whether two service names are equal by first normalizing each. If a
   * name does not have a runtime ID, it is assumed to be a local service.
   * 
   * @param name1
   *              The first service name
   * @param name2
   *              The second service name
   * @return Whether the two names are effectively equal
   */
  public static boolean checkServiceNameEquality(String name1, String name2) {
    return Objects.equals(getFullName(name1), getFullName(name2));
  }

  /**
   * Converts a topic method name to the name of the method that is used for
   * callbacks. Usually this involves prepending the string "on", removing any
   * "get" or "publish" prefix, and converting it all to proper camelCase.
   *
   * @param topicMethod
   *                    The topic method name, such as "publishState"
   * @return The name for the callback method, such as "onState"
   */
  static public String getCallbackTopicName(String topicMethod) {
    // replacements
    if (topicMethod.startsWith("publish")) {
      return String.format("on%s", capitalize(topicMethod.substring("publish".length())));
    } else if (topicMethod.startsWith("get")) {
      return String.format("on%s", capitalize(topicMethod.substring("get".length())));
    }

    // no replacement - just pefix and capitalize
    // FIXME - subscribe to onMethod --- gets ---> onOnMethod :P
    return String.format("on%s", capitalize(topicMethod));
  }

  /**
   * Gets a String representation of a Message
   *
   * @param msg
   *            The message
   * @return The String representation of the message
   */
  static public String getMsgKey(Message msg) {
    if (msg.sendingMethod != null) {
      return String.format("%s.%s --> %s.%s(%s) - %d", msg.sender, msg.sendingMethod, msg.name, msg.method,
          CodecUtils.getParameterSignature(msg.data), msg.msgId);
    } else {
      return String.format("%s --> %s.%s(%s) - %d", msg.sender, msg.name, msg.method,
          CodecUtils.getParameterSignature(msg.data), msg.msgId);
    }
  }

  /**
   * Get a String representing the data in method parameter form, i.e. each
   * element is separated by a comma. Only {@link #WRAPPER_TYPES} and
   * {@link MRLListener} will be directly converted to string form using
   * {@link Object#toString()}, all other types will be represented as their
   * class's simple name.
   *
   * @param data
   *             The list of objects to be represented as a parameter list string.
   * @return The string representing the data array
   */
  static public String getParameterSignature(final Object[] data) {
    if (data == null) {
      return "";
    }

    StringBuffer ret = new StringBuffer();
    for (int i = 0; i < data.length; ++i) {
      if (data[i] != null) {
        Class<?> c = data[i].getClass(); // not all data types are safe
        // toString() e.g.
        // SerializableImage
        // if (c == String.class || c == Integer.class || c == Boolean.class ||
        // c == Float.class || c == MRLListener.class) {
        if (WRAPPER_TYPES.stream().anyMatch(n -> n.equals(c)) || MRLListener.class.equals(c)) {
          ret.append(data[i].toString());
        } else {
          String type = data[i].getClass().getCanonicalName();
          String shortTypeName = type.substring(type.lastIndexOf(".") + 1);
          ret.append(shortTypeName);
        }

        if (data.length != i + 1) {
          ret.append(",");
        }
      } else {
        ret.append("null");
      }

    }
    return ret.toString();

  }

  static public String getServiceType(String inType) {
    if (inType == null) {
      return null;
    }
    if (inType.contains(".")) {
      return inType;
    }
    return String.format("org.myrobotlab.service.%s", inType);
  }

  /**
   * Deserializes a message and its data from a JSON string representation into
   * a fully decoded Message object. This method will first attempt to use the
   * method cache to determine what types the data elements should be
   * deserialized to, and if the method cache lookup fails it relies on the
   * virtual "class" field of the JSON to provide the type information.
   *
   * @param jsonData
   *                 The serialized Message in JSON form
   * @return A completely decoded Message object. Null is allowed if the JSON
   *         represented null.
   * @throws JsonDeserializationException
   *                                      if jsonData is malformed
   */
  public static /* @Nullable */ Message jsonToMessage(/* @Nonnull */ String jsonData) {
    if (log.isDebugEnabled()) {
      log.debug("Deserializing message: {}", jsonData);
    }
    Message msg = fromJson(jsonData, Message.class);

    if (msg == null) {
      log.warn("Null message within json, probably shouldn't happen");
      return null;
    }
    return decodeMessageParams(msg);
  }

  /**
   * Performs the second-stage decoding of a Message with JSON-encoded data
   * parameters. This method is meant to be a helper for the top-level Message
   * decoding methods to go straight from the various codecs to a completely
   * decoded Message.
   * <p>
   * Package visibility to allow alternative codecs to use this method.
   * </p>
   * <p>
   * </p>
   * <h2>Implementation Details</h2> There are important caveats to note when
   * using this method as a result of the implementation chosen.
   * <p>
   * If the method msg invokes is contained within the {@link MethodCache},
   * there exists type information for the data parameters and they can be
   * deserialized into the correct type using this method.
   * </p>
   * <p>
   * However, if no such method exists within the cache this method falls back
   * on using the embedded virtual meta field ({@link #CLASS_META_KEY}). Since
   * there is no type information available, there are two main caveats to using
   * this fallback method:
   * </p>
   *
   * <ol>
   * <li>Without the type information from the method cache we have no way of
   * knowing whether to interpret an array as an array of Objects or as a List
   * (or even what implementor of List to use)</li>
   * </ol>
   *
   *
   * @param msg
   *            The Message object containing the json-encoded data parameters.
   *            This object will be modified in-place
   * @return A fully-decoded Message
   * @throws JsonDeserializationException
   *                                      if any of the data parameters are
   *                                      malformed JSON
   */
  public static /* @Nonnull */ Message decodeMessageParams(/* @Nonnull */ Message msg) {
    String serviceName = msg.getFullName();
    Class<?> clazz = Runtime.getClass(serviceName);

    // Nullability of clazz is checked with this, if null
    // falls back to virt class field
    boolean useVirtClassField = clazz == null;
    if (!useVirtClassField) {
      try {
        Object[] params = MethodCache.getInstance().getDecodedJsonParameters(clazz, msg.method, msg.data);
        if (params == null)
          useVirtClassField = true;
        else {
          msg.data = params;
        }
        msg.encoding = null;
      } catch (RuntimeException e) {
        log.info(String.format("MethodCache lookup fail: %s.%s", serviceName, msg.method));
        // Fallback to virtual class field
        useVirtClassField = true;
      }
    }

    // Not an else since useVirtClassField can be set in the above if block
    if (useVirtClassField && msg.data != null) {
      for (int i = 0; i < msg.data.length; i++) {
        if (msg.data[i] instanceof String) {

          if (isBoolean((String) msg.data[i])) {
            msg.data[i] = makeBoolean((String) msg.data[i]);
          } else if (isInteger((String) msg.data[i])) {
            msg.data[i] = makeInteger((String) msg.data[i]);
          } else if (isDouble((String) msg.data[i])) {
            msg.data[i] = makeDouble((String) msg.data[i]);
          } else if (((String) msg.data[i]).startsWith("\"")) {
            msg.data[i] = fromJson((String) msg.data[i], String.class);
          } else if (((String) msg.data[i]).startsWith("[")) {
            // Array, deserialize to ArrayList to maintain compat with jackson
            msg.data[i] = fromJson((String) msg.data[i], ArrayList.class);
          } else {
            // Object
            // Serializable should cover everything of interest

            msg.data[i] = fromJson((String) msg.data[i], Serializable.class);
          }

          if (msg.data[i] != null && JSON_DEFAULT_OBJECT_TYPE.isAssignableFrom(msg.data[i].getClass())) {
            log.warn("Deserialized parameter to default object type. " + "Possibly missing virtual class field: "
                + msg.data[i]);
          }
        } else {
          log.error(
              "Attempted fallback Message decoding with virtual class field but " + "parameter is not String: %s");
        }
      }
      msg.encoding = null;
    }

    return msg;
  }

  /**
   * most lossy protocols need conversion of parameters into correctly typed
   * elements this method is used to query a candidate method to see if a simple
   * conversion is possible
   *
   * @param clazz
   *              the class
   * @return true/false
   */
  public static boolean isSimpleType(Class<?> clazz) {
    return WRAPPER_TYPES.contains(clazz) || clazz == String.class;
  }

  public static boolean isWrapper(Class<?> clazz) {
    return WRAPPER_TYPES.contains(clazz);
  }

  public static boolean isWrapper(String className) {
    return WRAPPER_TYPES_CANONICAL.contains(className);
  }

  /**
   * Converts a snake_case String to a camelCase variant.
   *
   * @param s
   *          A String written in snake_case
   * @return The same String but converted to camelCase
   */
  static public String toCamelCase(String s) {
    String[] parts = s.split("_");
    String camelCaseString = "";
    for (String part : parts) {
      camelCaseString = camelCaseString + toCCase(part);
    }
    return String.format("%s%s", camelCaseString.substring(0, 1).toLowerCase(), camelCaseString.substring(1));
  }

  /**
   * Capitalizes the first character of the string while the rest is set to
   * lower case.
   *
   * @param s
   *          The string
   * @return A String that is all lower case except for the first character
   */
  static public String toCCase(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
  }

  /**
   * Convert an Object to its JSON string form using the chosen JSON backend.
   *
   * @param o
   *          The object to be converted
   * @return The object in String JSON form
   * @throws JsonSerializationException
   *                                    if serialization fails
   */
  public static String toJson(Object o) {
    try {
      return mapper.writeValueAsString(o);
    } catch (Exception e) {
      throw new JsonSerializationException(e);
    }
  }

  /**
   * Convert an object to JSON using the chosen backend and write the result to
   * the specified output stream.
   *
   * @param out
   *            The OutputStream that the resultant JSON will be written to.
   * @param obj
   *            The object that will be converted to JSON.
   * @throws IOException
   *                                    if writing to the output stream fails
   * @throws JsonSerializationException
   *                                    if an exception occurs during
   *                                    serialization.
   */
  static public void toJson(OutputStream out, Object obj) throws IOException {
    String json;
    try {
      json = mapper.writeValueAsString(obj);
    } catch (Exception jsonProcessingException) {
      throw new JsonSerializationException(jsonProcessingException);
    }
    if (json != null)
      out.write(json.getBytes());
  }

  // === method signatures begin ===

  /**
   * Convert the given object to JSON, as if the object were an instance of the
   * given class.
   *
   * @param o
   *              The object to be serialized
   * @param clazz
   *              The class to treat the object as
   * @return The resultant JSON string
   * @throws JsonSerializationException
   *                                    if an exception occurs during
   *                                    serialization
   */
  public static String toJson(Object o, Class<?> clazz) {
    try {
      return mapper.writerFor(clazz).writeValueAsString(o);
    } catch (Exception e) {
      throw new JsonSerializationException(e);
    }
  }

  /**
   * Serialize the given object to JSON using the selected JSON backend and
   * write the result to a file with the given filename.
   *
   * @param o
   *                 The object to be serialized
   * @param filename
   *                 The name of the file to write the JSON to
   * @throws IOException
   *                                    if writing to the file fails
   * @throws JsonSerializationException
   *                                    if serialization throws an exception
   */
  public static void toJsonFile(Object o, String filename) throws IOException {
    byte[] json;
    try {
      json = mapper.writeValueAsBytes(o);
    } catch (Exception e) {
      throw new JsonSerializationException(e);
    }

    // try-wth-resources, ensures a file is closed even if an exception is
    // thrown
    try (FileOutputStream fos = new FileOutputStream(filename)) {
      fos.write(json);
    }
  }

  /**
   * Converts a given String from camelCase to snake_case, setting the entire
   * string to be lowercase.
   *
   * @param camelCase
   *                  The camelCase string to be converted
   * @return The string in snake_case form
   */
  static public String toUnderScore(String camelCase) {
    return toUnderScore(camelCase, false);
  }

  /**
   * Converts a given String from camelCase to snake_case, If toLowerCase is
   * true, the entire string will be set to lower case. If false, it will be set
   * to uppercase, and if null the casing will not be changed.
   *
   * @param camelCase
   *                    The camelCase string to be converted
   * @param toLowerCase
   *                    Whether the entire string should be lowercase, uppercase,
   *                    or not
   *                    changed (null)
   * @return The string in snake_case form
   */
  static public String toUnderScore(String camelCase, Boolean toLowerCase) {

    byte[] a = camelCase.getBytes();
    boolean lastLetterLower = false;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < a.length; ++i) {
      boolean currentCaseUpper = Character.isUpperCase(a[i]);

      Character newChar = null;
      if (toLowerCase != null) {
        if (toLowerCase) {
          newChar = (char) Character.toLowerCase(a[i]);
        } else {
          newChar = (char) Character.toUpperCase(a[i]);
        }
      } else {
        newChar = (char) a[i];
      }

      sb.append(String.format("%s%c", (lastLetterLower && currentCaseUpper) ? "_" : "", newChar));
      lastLetterLower = !currentCaseUpper;
    }

    return sb.toString();

  }

  /**
   * Equivalent to {@link #isInteger(String)}
   *
   * @param string
   *               The String to be checked
   * @return Whether the String can be parsed as an Integer
   */
  @Deprecated
  public static boolean tryParseInt(String string) {
    try {
      Integer.parseInt(string);
      return true;
    } catch (Exception e) {

    }
    return false;
  }

  public static String type(String type) {
    int pos0 = type.indexOf(".");
    if (pos0 > 0) {
      return type;
    }
    return String.format("org.myrobotlab.service.%s", type);
  }

  /**
   * Get the simple name of a service type name. A simple name is the name of
   * the service type without any package specifier.
   *
   * @param serviceType
   *                    The service type in String form
   * @return The simple name of the servide type
   */
  public static String getSimpleName(String serviceType) {
    int pos = serviceType.lastIndexOf(".");
    if (pos > -1) {
      return serviceType.substring(pos + 1);
    }
    return serviceType;
  }

  public static String getSafeReferenceName(String name) {
    return name.replaceAll("[@/ .-]", "_");
  }

  /**
   * Serializes the specified object to JSON, using {@link #mapper} with
   * {@link #jacksonPrettyPrinter} to pretty-ify the result.
   *
   * @param ret
   *            The object to be serialized
   * @return The object in pretty JSON form
   */
  public static String toPrettyJson(Object ret) {
    try {
      return mapper.writer(jacksonPrettyPrinter).writeValueAsString(ret);
    } catch (Exception e) {
      throw new JsonSerializationException(e);
    }

  }

  /**
   * Deserialize a given String into an array of Objects, treating the String as
   * JSON and using the selected JSON backend.
   *
   * @param data
   *             A String containing a JSON array
   * @return An array of Objects created by deserializing the JSON array
   * @throws Exception
   *                   If deserialization fails
   */
  static public Object[] decodeArray(Object data) throws Exception {
    // ITS GOT TO BE STRING - it just has to be !!! :)
    String instr = (String) data;
    // array of Strings ? - don't want to double encode !
    Object[] ret = null;
    synchronized (data) {
      ret = mapper.readValue(instr, Object[].class);
    }
    return ret;
  }

  /**
   * This is a Path to Message decoder - it takes a line of text and generates
   * the appropriate msg with json encoded string parameters and either invokes
   * (locally) or sendBlockingRemote (remotely)
   *
   * <pre>
   *
   * The expectation of this encoding is:
   *    if "/api/service/" is found - the end of that string is the starting point
   *    if "/api/service/" is not found - then the starting point of the string should be the service
   *      e.g "runtime/getUptime"
   *
   * Important to remember getRequestURI is NOT decoded and getPathInfo is.
   *
   *
   *
   * Method              URL-Decoded Result
   * ----------------------------------------------------
   * getContextPath()        no      /app
   * getLocalAddr()                  127.0.0.1
   * getLocalName()                  30thh.loc
   * getLocalPort()                  8480
   * getMethod()                     GET
   * getPathInfo()           yes     /a?+b
   * getProtocol()                   HTTP/1.1
   * getQueryString()        no      p+1=c+dp+2=e+f
   * getRequestedSessionId() no      S%3F+ID
   * getRequestURI()         no      /app/test%3F/a%3F+b;jsessionid=S+ID
   * getRequestURL()         no      http://30thh.loc:8480/app/test%3F/a%3F+b;jsessionid=S+ID
   * getScheme()                     http
   * getServerName()                 30thh.loc
   * getServerPort()                 8480
   * getServletPath()        yes     /test?
   * getParameterNames()     yes     [p 2, p 1]
   * getParameter("p 1")     yes     c d
   * </pre>
   *
   * @param from
   *             - sender
   * @param path
   *             - cli encoded msg
   * @return - a Message derived from cli
   */
  static public Message pathToMsg(String from, String path) {
    // Message msg = Message.createMessage(from,"ls", null);
    Message msg = new Message();
    msg.name = "runtime"; // default ?
    msg.method = "ls";

    // not required unless asynchronous
    msg.sender = from;

    /**
     * <pre>
    
     The key to this interface is leading "/" ...
     "/" is absolute path - dir or execute
     without "/" means runtime method - spaces and quotes can be delimiters
    
     "/"  -  list services
     "/{serviceName}" - list data of service
     "/{serviceName}/" - list methods of service
     "/{serviceName}/{method}" - invoke method
     "/{serviceName}/{method}/" - list parameters of method
     "/{serviceName}/{method}/jsonP0/jsonP1/jsonP2/..." - invoke method with parameters
    
     or runtime
     {method}
     {method}/
     {method}/p01
     *
     *
     * </pre>
     */

    path = path.trim();

    // remove uninteresting api prefix
    if (path.startsWith(API_SERVICE_PATH)) {
      path = path.substring(API_SERVICE_PATH.length());
    }

    // two possibilities - either it begins with "/" or it does not
    // if it does begin with "/" its an absolute path to a dir, ls, or invoke
    // if not then its a runtime method

    if (path.startsWith("/")) {
      // ABSOLUTE PATH !!!
      String[] parts = path.split("/"); // <- this breaks things ! e.g.
                                        // /runtime/connect/"http://localhost:8888"
      // path parts less than 3 is a dir or ls
      if (parts.length < 3) {
        // this morphs a path which has less than 3 parts
        // into a runtime "ls" method call to do reflection of services or
        // service methods
        // e.g. /clock -> /runtime/ls/"/clock"
        // e.g. /clock/ -> /runtime/ls/"/clock/"

        msg.method = "ls";
        msg.data = new Object[] { "\"" + path + "\"" };
        return msg;
      }

      // ["", "runtime", "shutdown"]
      if (parts.length == 3) {
        msg.name = parts[1];
        msg.method = parts[2];
        return msg;
      }

      // fix me diff from 2 & 3 "/"
      if (parts.length >= 3) {
        // prepare to parse the arguments

        msg.name = parts[1];
        // prepare the method
        msg.method = parts[2].trim();

        // remove the first 3 slashes
        String data = path.substring(("/" + msg.name + "/" + msg.method + "/").length());
        msg.data = extractJsonParamsFromPath(data);
      }
      return msg;
    } else {
      // e.g. ls /webgui/
      // retrieves all webgui methods
      String[] spaces = path.split(" ");
      // FIXME - need to deal with double quotes e.g. func A "B and C" D - p0 =
      // "A" p1 = "B and C" p3 = "D"
      msg.method = spaces[0];
      Object[] payload = new Object[spaces.length - 1];
      for (int i = 1; i < spaces.length; ++i) {
        // webgui will never use this section of code
        // currently the codepath is only excercised by InProcessCli
        // all of this methods will be "optimized" single commands to runtime (i
        // think)
        // so we are going to error on the side of String parameters - other
        // data types will have problems
        payload[i - 1] = spaces[i];
      }
      msg.data = payload;

      return msg;
    }
  }

  /**
   * extractJsonFromPath exects a forwad slash deliminated string
   * 
   * <pre>
   * json1 / json2 / json3
   * </pre>
   * 
   * It will return the json parts in a string array
   * 
   * @param input
   * @return
   */
  public static Object[] extractJsonParamsFromPath(String input) {
    List<Object> fromJson = new ArrayList<>();
    StringBuilder currentJson = new StringBuilder();
    boolean insideQuotes = false;

    for (char c : input.toCharArray()) {
      if (c == '"' && !insideQuotes) {
        insideQuotes = true;
      } else if (c == '"' && insideQuotes) {
        insideQuotes = false;
      }

      if (c == '/' && !insideQuotes) {
        if (currentJson.length() > 0) {
          fromJson.add(currentJson.toString());
          currentJson = new StringBuilder();
        }
      } else {
        currentJson.append(c);
      }
    }

    if (currentJson.length() > 0) {
      fromJson.add(currentJson.toString());
    }

    return fromJson.toArray(new Object[0]);
  }

  /**
   * Parse the specified data as an Integer. If parsing fails, returns null
   *
   * @param data
   *             The String to be coerced into an Integer
   * @return the data as an Integer, if parsing fails then null instead
   */
  static public Integer makeInteger(String data) {
    try {
      return Integer.parseInt(data);
    } catch (Exception e) {
    }
    return null;
  }

  /**
   * Checks whether the given String can be parsed as an Integer
   *
   * @param data
   *             The string to be checked
   * @return true if the data can be parsed as an Integer, false otherwise
   */
  static public boolean isInteger(String data) {
    try {
      Integer.parseInt(data);
      return true;
    } catch (Exception e) {
    }
    return false;
  }

  /**
   * Checks whether the given String can be parsed as a Double
   *
   * @param data
   *             The string to be checked
   * @return true if the data can be parsed as a Double, false otherwise
   */
  static public boolean isDouble(String data) {
    try {
      Double.parseDouble(data);
      return true;
    } catch (Exception e) {
    }
    return false;
  }

  /**
   * Parse the specified data as a Double. If parsing fails, returns null
   *
   * @param data
   *             The String to be coerced into a Doubled=
   * @return the data as a Double, if parsing fails then null instead
   */
  static public Double makeDouble(String data) {
    try {
      return Double.parseDouble(data);
    } catch (Exception e) {
    }
    return null;
  }

  /**
   * Checks whether the given String can be parsed as a Boolean
   *
   * @param data
   *             The string to be checked
   * @return true if the data can be parsed as a boolean, false otherwise
   */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  static public Boolean isBoolean(String data) {
    try {
      Boolean.parseBoolean(data);
      // The above will return false and not throw
      // exception for most cases
      return "true".equals(data) || "false".equals(data);
    } catch (Exception ignored) {
      return false;
    }
  }

  /**
   * Parse the specified data as a Boolean. If parsing fails, returns null
   *
   * @param data
   *             The String to be coerced into a Boolean
   * @return the data as a boolean, if parsing fails then null instead
   */
  static public Boolean makeBoolean(String data) {
    try {
      return Boolean.parseBoolean(data);
    } catch (Exception e) {
    }
    return null;
  }

  /**
   * Get a description for each of the supported APIs
   *
   * @return A list containing a description for each supported API
   */
  static public List<ApiDescription> getApis() {
    List<ApiDescription> ret = new ArrayList<>();
    ret.add(new ApiDescription("message", "{scheme}://{host}:{port}" + API_MESSAGES_PATH,
        "ws://localhost:8888" + API_MESSAGES_PATH,
        "An asynchronous api useful for bi-directional websocket communication, primary messages api for the webgui.  URI is "
            + API_MESSAGES_PATH
            + " data contains a json encoded Message structure"));
    ret.add(new ApiDescription("service", "{scheme}://{host}:{port}" + API_SERVICE_PATH,
        "http://localhost:8888" + API_SERVICE_PATH + "/runtime/getUptime",
        "An synchronous api useful for simple REST responses"));
    return ret;
  }

  /**
   * Creates a properly double encoded Json msg string. Why double encode ? -
   * because initial decode should decode router and header information. The
   * first decode will leave the payload a array of json strings. The header
   * will send it to a another process or it will go to the MethodCache of some
   * service. The MethodCache will decode a 2nd time based on a method signature
   * key match (key based on parameter types).
   *
   * @param sender
   *                      the sender of the message
   * @param sendingMethod
   *                      the method sending it
   * @param name
   *                      dest service
   * @param method
   *                      dest method
   * @param params
   *                      params to pass
   * @return the string representation of the json message
   */
  public static String createJsonMsg(String sender, String sendingMethod, String name, String method,
      Object... params) {
    Message msg = Message.createMessage(sender, name, method, null);
    msg.sendingMethod = sendingMethod;
    Object[] d = null;
    if (params != null) {
      d = new Object[params.length];
      for (int i = 0; i < params.length; ++i) {
        d[i] = CodecUtils.toJson(params[i]);
      }
      msg.setData(d);
    }
    return CodecUtils.toJson(msg);
  }

  /**
   * Encodes a Message as JSON, double-encoding the {@link Message#data} if not
   * already double-encoded. The selected JSON backend will be used.
   *
   * @param inMsg
   *              The message to be encoded
   * @return A String representation of the message and all of its members in
   *         JSON format.
   */
  public static String toJsonMsg(Message inMsg) {
    if ("json".equals(inMsg.encoding)) {
      // msg already has json encoded data parameters
      // just encode the msg envelope
      return CodecUtils.toJson(inMsg);
    }
    Message msg = new Message(inMsg);
    msg.encoding = "json";
    Object[] params = inMsg.getData();
    Object[] d = null;
    if (params != null) {
      d = new Object[params.length];
      for (int i = 0; i < params.length; ++i) {
        d[i] = CodecUtils.toJson(params[i]);
      }
      msg.setData(d);
    }
    return CodecUtils.toJson(msg);
  }

  @Deprecated
  public static Message toJsonParameters(Message msg) {
    Object[] data = msg.getData();
    if (data != null) {
      Object[] params = new Object[data.length];
      for (int i = 0; i < params.length; ++i) {
        params[i] = toJson(data[i]);
      }
      msg.setData(params);
    }
    return msg;
  }

  /**
   * Serialize the given object to YAML.
   *
   * @param o
   *          The object to be serialized
   * @return A String formatted as YAML representing the object
   */
  public static String toYaml(Object o) {
    // not thread safe - so we new here
    DumperOptions options = new DumperOptions();
    options.setIndent(2);
    options.setPrettyFlow(true);
    // options.setBeanAccess(BeanAccess.FIELD);
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    /**
     * <pre>
     *  How to suppress null fields if desired
     Representer representer = new Representer() {
     &#64;Override
     protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
     // if value of property is null, ignore it.
     if (propertyValue == null) {
     return null;
     } else {
     return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
     }
     }
     };
     * </pre>
     */

    Yaml yaml = new Yaml(options);
    // yaml.setBeanAccess(BeanAccess.FIELD);
    String c = yaml.dump(o);
    return c;
  }

  public static String allToYaml(Iterator<? extends Object> o) {
    // not thread safe - so we new here
    DumperOptions options = new DumperOptions();
    options.setIndent(2);
    options.setPrettyFlow(true);
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

    Yaml yaml = new Yaml(options);
    // yaml.setBeanAccess(BeanAccess.FIELD);
    String c = yaml.dumpAll(o);
    return c;
  }

  public static Iterable<Object> allFromYaml(InputStream is) {
    // Yaml yaml = new Yaml(new Constructor(clazz));
    Yaml yaml = new Yaml();
    // yaml.setBeanAccess(BeanAccess.FIELD);
    return yaml.loadAll(is);
  }

  /**
   * Deserialize the given string into the specified class, treating the string
   * as YAML.
   *
   * @param data
   *              The YAML to be deserialized
   * @param clazz
   *              The target class
   * @param <T>
   *              The type of the target class
   * @return An instance of the target class with the state given by the YAML
   *         string
   */
  public static <T extends Object> T fromYaml(String data, Class<T> clazz) {
    Yaml yaml = new Yaml(new Constructor(clazz));
    // yaml.setBeanAccess(BeanAccess.FIELD);
    return (T) yaml.load(data);
  }

  /**
   * Checks if the service name is local to the current process instance
   * 
   * @param name
   *             The service name to be checked
   * @return Whether the service name is local to the given ID
   */
  public static boolean isLocal(String name) {
    if (!name.contains("@")) {
      return true;
    }
    return name.substring(name.indexOf("@") + 1).equals(Platform.getLocalInstance().getId());
  }

  /**
   * Checks if the service name given by name is local, i.e. it has no remote ID
   * (has no '@' symbol), or if it has a remote ID it matches the ID given.
   *
   * @param name
   *             The service name to be checked
   * @param id
   *             The runtime ID of the local instance
   * @return Whether the service name is local to the given ID
   */
  public static boolean isLocal(String name, String id) {
    if (!name.contains("@")) {
      return true;
    }
    return name.substring(name.indexOf("@") + 1).equals(id);
  }

  public static ServiceConfig readServiceConfig(String filename) throws IOException {
    return readServiceConfig(filename, new StaticType<>() {
    });
  }

  /**
   * Read a YAML file given by the filename and convert it into a ServiceConfig
   * object by deserialization.
   *
   * @param filename
   *                 The name of the YAML file
   * @return The equivalent ServiceConfig object
   * @throws IOException
   *                     if reading the file fails
   */
  public static <C extends ServiceConfig> C readServiceConfig(String filename, StaticType<C> type) throws IOException {
    String data = Files.readString(Paths.get(filename));
    Yaml yaml = new Yaml();
    C parsed = yaml.load(data);
    if (type.asClass().isAssignableFrom(parsed.getClass())) {
      return parsed;
    } else {
      throw new InvalidObjectException(
          "Deserialized type was " + parsed.getClass() + ", expected " + type + ". Deserialized object: " + parsed);
    }
  }

  /**
   * Set a field of the given object identified by the given field name to the
   * given value. If the field does not exist or the value is of the wrong type,
   * this method is a no-op.
   *
   * @param o
   *              The object whose field will be modified
   * @param field
   *              The name of the field to be modified
   * @param value
   *              The new value to set the field to
   */
  public static void setField(Object o, String field, Object value) {
    try {
      // TODO - handle all types :P
      Field f = o.getClass().getDeclaredField(field);
      f.setAccessible(true);
      f.set(o, value);
    } catch (Exception e) {
      /** don't care - if its not there don't set it */
    }
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Object o = readServiceConfig("data/config/InMoov2_FingerStarter/i01.chatBot.yml");

      String json = CodecUtils.fromJson("test", String.class);
      log.info("json {}", json);
      json = CodecUtils.fromJson("a test", String.class);
      log.info("json {}", json);
      json = CodecUtils.fromJson("\"a/test\"", String.class);
      log.info("json {}", json);
      CodecUtils.fromJson("a/test", String.class);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  /**
   * A description of an API type
   */
  public static class ApiDescription {
    /**
     * The string to use after {@link #PARAMETER_API} in URIs to select this
     * API.
     */
    public final String key;

    /**
     * The path to reach this API
     */
    public final String path; // {scheme}://{host}:{port}/api/messages

    /**
     * An example URI to reach this API
     */
    public final String exampleUri;

    /**
     * The description of this API
     */
    public final String description;

    /**
     * Construct a new API description.
     *
     * @param key
     *                       {@link #key}
     * @param uriDescription
     *                       {@link #path}
     * @param exampleUri
     *                       {@link #exampleUri}
     * @param description
     *                       {@link #description}
     */
    public ApiDescription(String key, String uriDescription, String exampleUri, String description) {
      this.key = key;
      this.path = uriDescription;
      this.exampleUri = exampleUri;
      this.description = description;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      ApiDescription that = (ApiDescription) o;
      return Objects.equals(key, that.key) && Objects.equals(path, that.path)
          && Objects.equals(exampleUri, that.exampleUri) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, path, exampleUri, description);
    }
  }

  /**
   * Single parameter from JSON. Will use default return type, currently
   * LinkedTreeMap to return a POJO object that can be easily accessed.
   * 
   * @param json
   * @return
   */
  public static Object fromJson(String json) {
    return fromJson(json, (Class<Object>) null);
  }

  public static String toBase64(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  public static byte[] fromBase64(String input) {
    return Base64.getDecoder().decode(input);
  }

  public static int[] getColor(String value) {
    String hex = getColorHex(value);
    if (hex != null) {
      return hexToRGB(hex);
    }
    return hexToRGB(value);
  }

  public static List<String> getColorNames() {
    Field[] colorFields = Color.class.getDeclaredFields();
    List<String> colorNames = new ArrayList<>();

    for (Field field : colorFields) {
      if (field.getType().equals(Color.class)) {
        colorNames.add(field.getName());
      }
    }
    return colorNames;
  }

  public static String getColorHex(String colorName) {
    Color color;
    try {
      color = (Color) Color.class.getField(colorName.toLowerCase()).get(null);
    } catch (Exception e) {
      return null;
    }
    return String.format("#%06X", (0xFFFFFF & color.getRGB()));
  }

  public static int[] hexToRGB(String hexValue) {
    if (hexValue == null) {
      return null;
    }
    int[] rgb = new int[3];
    try {
      // Check if the hex value starts with '#' and remove it if present
      if (hexValue.startsWith("#")) {
        hexValue = hexValue.substring(1);
      }

      if (hexValue.startsWith("0x")) {
        hexValue = hexValue.substring(2);
      }

      // Parse the hex string into integers for red, green, and blue components
      rgb[0] = Integer.parseInt(hexValue.substring(0, 2), 16); // Red
      rgb[1] = Integer.parseInt(hexValue.substring(2, 4), 16); // Green
      rgb[2] = Integer.parseInt(hexValue.substring(4, 6), 16); // Blue
    } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
      log.error("Invalid hex color value {}", hexValue);
    }
    return rgb;
  }

  public static String hashcodeToHex(int hashCode) {
    String hexString = Long.toHexString(hashCode).toUpperCase();
    return String.format("%6s", hexString).replace(' ', '0').substring(0, 6);
  }

}
