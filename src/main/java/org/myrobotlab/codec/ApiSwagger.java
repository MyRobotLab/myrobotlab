package org.myrobotlab.codec;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.interfaces.MessageSender;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ApiSwagger  {

  public final static Logger log = LoggerFactory.getLogger(Runtime.class);

  public Object process(MessageSender sender, OutputStream out, Message msgFromUri, String data) {

    return null;
  }

  // edit these to adjust environments
  private String[][] serverEndpoints = new String[][] { { "http://170.2.107.118:3333/api", "dev" },
      { "http://qdtmeutelcebl03.azure-qa-eastus.us164.corpintra.net:3333/api", "qaEast" },
      { "http://stnascvdl009.us164.corpintra.net:2500/proxy?target=http://pdtmeutelcebl02.azure-prd-eastus.us164.corpintra.net:3333/api", "prodEast" } };

  public JsonObject createHeaders() {
    JsonObject obj = new JsonObject();

    // swagger version
    obj.addProperty("openapi", "3.0.0");

    // info
    obj.add("info",
        new JsonParser().parse("{" + "\"description\": \"Entity Broker Auto-generated documentation\"," + "\"title\": \"Entity Broker API\"," + "\"version\": \"1.0.0\"" + "}"));

    // host
    JsonArray servers = new JsonArray();
    for (String[] s : serverEndpoints) {
      JsonObject temp = new JsonObject();
      temp.addProperty("url", s[0]);
      temp.addProperty("description", s[1]);
      servers.add(temp);
    }
    obj.add("servers", servers);

    // schemes
    // obj.add("schemes", new JsonParser().parse("[\"https\"]"));

    return obj;
  }

  public Map<String, String> getSwagger() throws IOException {
    JsonObject data = createHeaders();

    /*
     * ClassPath cp =
     * ClassPath.from(Thread.currentThread().getContextClassLoader());
     * Class<?>[] classes =
     * cp.getTopLevelClasses("com.daimler.eb.endpoint").asList().stream().map((
     * c) -> c.load()).toArray(Class<?>[]::new);
     */
    Class<?>[] classes = new Class<?>[] { Runtime.class, Servo.class };
    data.add("paths", getPaths(classes));

    // convert to a Map<String,String>
    return new Gson().fromJson(data.toString(), new HashMap<String, String>().getClass());
  }

  public JsonObject getPaths(Class<?>[] classes) {
    // get all the methods
    Method[] methods = Arrays.stream(classes).parallel().map(c -> c.getDeclaredMethods()).reduce(new Method[] {},
        (x, y) -> Stream.concat(Arrays.stream(x), Arrays.stream(y)).toArray(Method[]::new));

    // filter out all non-inherited and non-public methods and map each
    // method to a JSON Object describing its GET / POST
    /*
     * { "get":{ "path":..., ... }, "post":{ "path":..., ... }, }
     */
    JsonObject[] methodDetails = Arrays.stream(methods).parallel()
        .filter(m -> Modifier.isPublic(m.getModifiers()) && !m.getName().equals("main") && !Modifier.isStatic(m.getModifiers())).map((m) -> {
          JsonObject get = new JsonObject();
          JsonObject post = new JsonObject();

          // construct the api path
          Parameter[] params = m.getParameters();
          String paramNames = "";
          for (Parameter p : params)
            paramNames += "/{" + p.getName() + "}";

          String apiPathGet = "/" + m.getDeclaringClass().getSimpleName() + "/" + m.getName() + paramNames;
          String apiPathPost = "/" + m.getDeclaringClass().getSimpleName() + "/" + m.getName();

          // get properties & map each parameter t a JsonObject
          get.addProperty("apiPath", apiPathGet);
          get.add("tags", new JsonParser().parse("[" + m.getDeclaringClass().getSimpleName() + "]"));
          get.add("responses", new JsonParser().parse("{" + "\"200\": {" + "\"description\": \"successful response\"" + "}" + "}"));
          JsonObject[] paramDetails = Arrays.stream(params).parallel().map((p) -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", p.getName());
            obj.addProperty("in", "path");
            obj.addProperty("required", true);
            obj.add("schema", getSchemaFromType(p.getType().getSimpleName()));
            return obj;
          }).toArray(JsonObject[]::new);
          JsonArray parameters = new JsonArray();
          for (JsonObject pd : paramDetails)
            parameters.add(pd);
          get.add("parameters", parameters);

          // post properties
          post.addProperty("apiPath", apiPathPost);
          post.add("tags", new JsonParser().parse("[" + m.getDeclaringClass().getSimpleName() + "]"));
          post.add("responses", new JsonParser().parse("{" + "\"200\": {" + "\"description\": \"successful response\"" + "}" + "}"));

          JsonObject requestBody = new JsonObject();
          requestBody.addProperty("required", true);
          JsonObject requestBodyContent = new JsonObject();
          requestBody.add("content", requestBodyContent);
          JsonObject rbcAppJson = new JsonObject();
          requestBodyContent.add("application/json", rbcAppJson);
          JsonObject postBodySchema = new JsonObject();
          postBodySchema.addProperty("type", "array");
          postBodySchema.addProperty("minItems", params.length);
          postBodySchema.addProperty("maxItems", params.length);
          JsonObject arrayItems = new JsonObject();
          JsonArray types = new JsonArray();
          for (Parameter p : params) {
            types.add(getSchemaFromType(p.getType().getSimpleName()));
          }
          arrayItems.add("oneOf", types);
          postBodySchema.add("items", arrayItems);
          rbcAppJson.add("schema", postBodySchema);
          post.add("requestBody", requestBody);

          JsonObject res = new JsonObject();
          res.add("get", get);
          res.add("post", post);
          return res;
        }).toArray(JsonObject[]::new);

    // convert each method get/post info to distinct get/post infos
    JsonObject res = new JsonObject();
    for (JsonObject m : methodDetails) {
      // if both GET/POST have same path, then combine into one object
      if (m.get("get").getAsJsonObject().get("apiPath").equals(m.get("post").getAsJsonObject().get("apiPath"))) {
        res.add(m.get("get").getAsJsonObject().get("apiPath").getAsString(), m);
      } else { // otherwise, separate them into different objects
        JsonObject g = new JsonObject();
        g.add("get", m.get("get"));
        res.add(m.get("get").getAsJsonObject().get("apiPath").getAsString(), g);

        JsonObject p = new JsonObject();
        p.add("post", m.get("post"));
        res.add(m.get("post").getAsJsonObject().get("apiPath").getAsString(), p);
      }

      m.get("get").getAsJsonObject().remove("apiPath");
      m.get("post").getAsJsonObject().remove("apiPath");
    }

    return res;
  }

  private JsonObject getSchemaFromType(String s) {
    JsonObject res = new JsonObject();
    s = s.toLowerCase();
    if (s.equals("String"))
      res.addProperty("type", "string");
    else if (s.equals("int"))
      res.addProperty("type", "integer");
    else if (s.equals("boolean"))
      res.addProperty("type", "boolean");
    else if (s.equals("double") || s.equals("float") || s.equals("long") || s.equals("byte"))
      res.addProperty("type", "number");
    else if (s.contains("[]")) {
      res.addProperty("type", "array");
      res.add("items", getSchemaFromType(s.substring(0, s.length() - 2)));
    } else
      res.addProperty("type", "object");

    return res;
  }

  public static void main(String[] args) {
    try {
      List<String> myList =
          Arrays.asList("a1", "a2", "b1", "c2", "c1");

      myList
          .stream()
          // .filter(s -> s.startsWith("c"))
          .map(String::toUpperCase)
          .map(s -> s + "blah")
          .map(s -> s + "oink")
          .sorted()
          .forEach(System.out::println);
      
      
      ApiSwagger swagger = new ApiSwagger();
      
      log.info("{}", CodecUtils.toJson(swagger.getSwagger()));
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  public Object process(MessageSender webgui, String apiKey, String uri, String uuid, OutputStream out, String json) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

}
