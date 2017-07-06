package org.myrobotlab.swagger;

import java.util.Map;
import java.util.TreeMap;

public class ObjectDefinition {
    String type;
    String[] required;
    // Map<String, Property> properties = new TreeMap<String, Property>();
    Map<String, Map<String,Object>> properties = new TreeMap<String, Map<String,Object>>();
}
