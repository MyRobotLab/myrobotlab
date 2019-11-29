package org.myrobotlab.swagger;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import java.lang.reflect.Type;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Swagger3 {

  public final static Logger log = LoggerFactory.getLogger(Runtime.class);

  private transient static Gson gson = new GsonBuilder()
      /* .setDateFormat("yyyy-MM-dd HH:mm:ss.SSS") */.setPrettyPrinting().disableHtmlEscaping().create();

  // FIXME - should be in an Api (no 3rd part libraries) - this is global static
  // config
  static private HashSet<String> hideMethods = new HashSet<String>();

  // FIXME - resolve
  // {instance} vs {static class} info ???
  public Map<String, Object> getSwagger(List<Registration> nameAndTypes) {
    Map<String, Object> swagger = new TreeMap<>();

    try {

      hideMethods.add("main");
      hideMethods.add("loadDefaultConfiguration");
      hideMethods.add("getDescription");
      hideMethods.add("run");
      hideMethods.add("access$0");

      // build the map
      swagger.put("openapi", "3.0.0");
      Map<String, Object> info = new TreeMap<>();
      swagger.put("info", info);
      info.put("title", "MyRobotLab");
      info.put("description", "This is a MyRobotLab server.  You can find out more about MyRobotLab at [http://myrobotlab.org](http://myrobotlab.org).");
      // info.put("termsOfService", "http://myrobotlab.org/terms/");
      // info.put("contact", "admin@myrobotlab.org");
      Map<String, Object> license = new TreeMap<>();
      info.put("license", license);
      license.put("name", "Apache 2.0");
      license.put("url", "http://www.apache.org/licenses/LICENSE-2.0.html");
      info.put("version", "1.1.15");

      Map<String, Object> externalDocs = new TreeMap<>();
      swagger.put("externalDocs", externalDocs);
      externalDocs.put("description", "Find out more about MyRobotLab");
      externalDocs.put("url", "http://myrobotlab.org");

      List<Object> servers = new ArrayList<>();
      swagger.put("servers", servers);
      Map<String, Object> server = new TreeMap<>();
      server.put("url", "http://demo.myrobotlab.org/api/messages");
      servers.add(server);

      // object definition ?
      Map<String, Object> tags = new TreeMap<>();
      swagger.put("tags", tags);

      Map<String, Object> paths = new TreeMap<>();
      swagger.put("paths", paths);

      // param object defintion ???
      Map<String, Object> components = new TreeMap<>();
      swagger.put("components", components);

      for (Registration nameAndType : nameAndTypes) {
        ///////////////////////////////////
        // BUILD IT !!!
        ///////////////////////////////////
        // get static class info
        String name = nameAndType.name;
        Class<?> c = Class.forName(String.format("org.myrobotlab.service.%s", nameAndType.typeKey));

        // TODO - make examples and other info in MetaData !
        Method method = c.getMethod("getMetaData");
        ServiceType serviceType = (ServiceType) method.invoke(null); // Tags ?
        // Examples ?

        // prefix ... ???
        String template = "/api/service/%s/%s";

        Method[] methods = c.getDeclaredMethods();

        Method m;
        // MethodEntry me;
        String s;
        for (int i = 0; i < methods.length; ++i) {
          m = methods[i];

          if (hideMethods.contains(m.getName())) {
            continue;
          }

          Map<String, Object> path = new TreeMap<>();
          Map<String, Object> get = new TreeMap<>();
          paths.put(String.format(template, name, m.getName()), path);
          path.put("get", get);

          Map<String, Object> responses = new TreeMap<>();

          Map<String, Object> response200 = new TreeMap<>();
          responses.put("200", response200);
          response200.put("description", "successful operation");
          get.put("responses", responses);

          get.put("summary", "summary");
          get.put("description", "description");
          // get.put("operationId", "operationId");
          // Map<String, Object> parameters = new TreeMap<>();
          List<Object> parameters = new ArrayList<>();
          get.put("parameters", parameters);
          Class<?> paramTypes[] = m.getParameterTypes();
          Parameter[] params = m.getParameters();
          if (m.getName().equals("createAndStartServices")) {
            log.info("here");
          }
          for (int j = 0; j < params.length; ++j) {
            Parameter parameter = params[j];
            
            Map<String, Object> p = new TreeMap<>();
            if (parameter.isNamePresent()) {
              p.put("name", parameter.getName());
            } else {
              p.put("name", String.format("p%d", j));
            }

            p.put("in", "path");
            p.put("description", "description");
            p.put("required", true);
            p.put("schema", toSchema(parameter, paramTypes[j]));

            parameters.add(p);
            // FIXME p.description from JavaDoc
            parameter.getType();
            // String parameterName = parameter.getName();
          }
        }

      } // for each nameAndType

    } catch (Exception e) {
      log.error("getSwagger threw", e);
    }

    return swagger;
  }


  public Map<String, Object> toSchema(Parameter parameter,  Class<?> type) {
    Map<String, Object> schema = new TreeMap<>();
    
    Type pt = parameter.getParameterizedType();

    if (type.equals(String.class)) {
      schema.put("type", "string");
    } else if (type.equals(Integer.class) || type.equals(int.class)) {
      schema.put("type", "integer");
      // schema.put("format", "int64");
    } else if (type.equals(Long.class) || type.equals(long.class)) {
      schema.put("type", "integer");
      // schema.put("format", "int64");
    } else if (type.equals(Double.class) || type.equals(double.class)) {
      schema.put("type", "number");
      // schema.put("format", "int64");
    } else if (type.equals(Float.class) || type.equals(float.class)) {
      schema.put("type", "number");
      // schema.put("format", "int64");
    } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
      schema.put("type", "boolean");
    } else if (type.equals(Array.class) || type.isAssignableFrom(List.class) || type.isAssignableFrom(Set.class)) {
      schema.put("type", "array");
 //     schema.put
    } else {
      schema.put("type", "object");
    }

    return schema;
  }

  public static void main(String[] args) {
    LoggingFactory.init("info");

    try {

      Swagger3 swagger = new Swagger3();
      List<Registration> nameAndTypes = new ArrayList<>();
      nameAndTypes.add(new Registration(Platform.getLocalInstance().getId(), "runtime", "Runtime", null));
      nameAndTypes.add(new Registration(Platform.getLocalInstance().getId(), "servo01", "Servo", null));

      System.out.println(gson.toJson(swagger.getSwagger(nameAndTypes)));
      // cli.processInput("test", null);

      // log.info("here");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}