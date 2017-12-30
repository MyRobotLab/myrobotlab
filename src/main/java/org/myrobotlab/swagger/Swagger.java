package org.myrobotlab.swagger;

import java.util.Map;
import java.util.TreeMap;
// validate - http://bigstickcarpet.com/swagger-parser/www/index.html
public class Swagger {
  public String swagger = "2.0";
  // Info
  public String host = "localhost";
  public String basePath = "/api";
  // Tags
  public String[] schemes = new String[]{"ws","wss","http","https"};
  public Map<String, Path> paths = new TreeMap<String, Path>();
  
  public Map<String, ObjectDefinition> definitions = new TreeMap<String, ObjectDefinition>();
}
