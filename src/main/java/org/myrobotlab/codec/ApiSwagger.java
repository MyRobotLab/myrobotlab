package org.myrobotlab.codec;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class ApiSwagger {

  public final static Logger log = LoggerFactory.getLogger(Runtime.class);
  
  static final Map<Class<?>, String> primitiveMap = new HashMap<>();
  static {
  primitiveMap.put(int.class, "integer");
  primitiveMap.put(long.class, "integer");
  primitiveMap.put(float.class, "number");
  primitiveMap.put(double.class, "number");
  primitiveMap.put(short.class, "integer");
  primitiveMap.put(byte.class, "integer");
  primitiveMap.put(boolean.class, "boolean");
  primitiveMap.put(char.class, "string");
  primitiveMap.put(Integer.class, "integer");
  primitiveMap.put(Long.class, "integer");
  primitiveMap.put(Float.class, "number");
  primitiveMap.put(Double.class, "number");
  primitiveMap.put(Short.class, "integer");
  primitiveMap.put(Byte.class, "integer");
  primitiveMap.put(Boolean.class, "boolean");
  primitiveMap.put(Character.class, "string");
  }

  
  public static String toPrimitive(Class<?> clazz) {
    if (primitiveMap.containsKey(clazz)) {
      return primitiveMap.get(clazz);
    } else {
      return "string";
    }
  }

  public static String generateSwaggerYaml(Class<?> clazz) {
    Map<String, Object> swaggerData = new TreeMap<>();
    Map<String, Object> paths = new TreeMap<>();

    Method[] methods = clazz.getMethods();
    for (Method method : methods) {
      if (Modifier.isPublic(method.getModifiers())) {
        String prefix = "/" + method.getName(); // Method name is used as the path
        String path = prefix;

        Class<?>[] parameterTypes = method.getParameterTypes();
        // Parameter names are only available if compiled with -parameters
        Parameter[] params = method.getParameters();
        
        List<Map<String,Object>> paramArray = new ArrayList<>();
        
        
        for (int i = 0; i < parameterTypes.length; i++) {
          Parameter param = params[i];
          String paramName = "param" + i; // Using param0, param1, etc. as path
                                          // variable names
          // only available with compiler -parameters option
          paramName = param.getName();
          
          Map<String, Object> paramDetail = new TreeMap<>();
          
          paramDetail.put("in", "path");
          paramDetail.put("name", paramName);
          paramDetail.put("required", true);
          Map<String,Object>paramSchema = new TreeMap<>();
          paramDetail.put("schema", paramSchema);
          paramSchema.put("type", toPrimitive(param.getType()));
          
          paramArray.add(paramDetail);
          path = path + "/{"+paramName+"}";
        }

        // Create the path entry for the method
        Map<String, Object> data = new TreeMap<>();
        
        Map<String, Object> get = new TreeMap<>();
        data.put("get", get);
        List<String> tags = new ArrayList<>();
        get.put("tags", tags);
        tags.add(clazz.getSimpleName());
        get.put("parameters", paramArray);
        Map<String, Object> responses = new TreeMap<>();
        get.put("responses", responses);
        Map<String, Object> http_200 = new TreeMap<>();
        responses.put("200", http_200);
        http_200.put("description", "OK");
        Map<String, Object> content = new TreeMap<>();
        Map<String, Object> json = new TreeMap<>();
        http_200.put("content", content);
        content.put("application/json", json);
        Map<String, Object> schema = new TreeMap<>();
        json.put("schema", schema);


        paths.put(path, data);
      }
    }

    swaggerData.put("paths", paths);

    // Convert to YAML
    Yaml yaml = new Yaml(getDumperOptions());
    return yaml.dump(swaggerData);
  }

  private static DumperOptions getDumperOptions() {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    return options;
  }

  public static void main(String[] args) {
    try {
   
      // log.info("{}", CodecUtils.toJson(ApiSwagger.generateSwaggerYaml(Servo.class)));
      log.info("{}", ApiSwagger.generateSwaggerYaml(Servo.class));
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
