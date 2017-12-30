package org.myrobotlab.swagger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Verb {
    
    public String[] tags;
    public String summary;
    public String description;
    public String operationId;
    public String[] consumes = new String[]{"application/json"};
    public String[] produces = new String[]{"application/json"};
    public List<Parameter> parameters = new ArrayList<Parameter>();
    public Map<String, Response> responses = new TreeMap<String, Response>();
    // security
}
