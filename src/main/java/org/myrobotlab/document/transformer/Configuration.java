package org.myrobotlab.document.transformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class Configuration {

  // TODO: add a map type.
  // TODO: push the name/class onto this ?
  // The config consists of many parts.
  // server level config
  // stage config
  // workflow /pipeline config
  // connector config

  protected HashMap<String, Object> config = null;
  // private XStream xstream = null;

  public Configuration() {
    config = new HashMap<String, Object>();
    // figure that we need to be able to serialize / deserialize
    // TODO: consider a faster driver / serializer
    // xstream = new XStream(new StaxDriver());
  }

  public void setStringParam(String name, String value) {
    config.put(name, value);
  }

  public String getStringParam(String name) {
    return getStringParam(name, null);
  }

  public String getStringParam(String name, String defaultValue) {
    if (config.containsKey(name)) {
      Object val = config.get(name);
      if (val instanceof String) {
        return ((String) val).trim();
      } else {
        // TOOD: this value was not a string?
        return val.toString().trim();
      }
    } else {
      return defaultValue;
    }
  }

  public String getProperty(String name) {
    return getStringParam(name);
  }

  public String getProperty(String name, String defaultValue) {
    String val = getStringParam(name);
    if (val == null) {
      return defaultValue;
    } else {
      return val;
    }
  }

  public void setIntegerParam(String name, Integer value) {
    config.put(name, value);
  }

  public Integer getIntegerParam(String name, Integer defaultValue) {
    if (config.containsKey(name)) {
      Object val = config.get(name);
      if (val instanceof Integer) {
        return (Integer) val;
      } else {
        // TOOD: this value was not a string?
        return Integer.valueOf(val.toString());
      }
    } else {
      return defaultValue;
    }
  }

  public void setBoolParam(String name, Boolean value) {
    config.put(name, value);
  }

  public Boolean getBoolParam(String name, Boolean defaultValue) {
    if (config.containsKey(name)) {
      Object val = config.get(name);
      if (val instanceof Boolean) {
        return (Boolean) val;
      } else {
        // TOOD: this value was not a string?
        return Boolean.valueOf(val.toString());
      }
    } else {
      return defaultValue;
    }
  }

  public void setStringArray(String name, String[] values) {
    config.put(name, values);
  }

  public String[] getStringArray(String name) {
    // TODO Auto-generated method stub
    if (config.containsKey(name)) {
      Object val = config.get(name);
      if (val instanceof String[]) {
        return (String[]) val;
      } else {
        // TODO: what if it's not a valid type?!
      }
    }
    // TODO: what should we return?
    String[] empty = new String[0];
    return empty;
  }

  public void setListParam(String name, List<String> values) {
    config.put(name, values);
  }

  // @SuppressWarnings("unchecked")
  public List<String> getListParam(String name) {
    Object val = config.get(name);
    if (val instanceof List) {
      // TODO: type safety?!
      return (List<String>) val;
    }
    // TODO: null or empty list?
    return null;
  }

  public Map<String, String> getMapProperty(String name) {
    // TODO type safety?!
    return (Map<String, String>) config.get(name);
  }

  public String toXML() {
    // TODO: does this serialize the xstream object itself?
    // maybe we should convert the xstream to a singleton in the platform.
    // TODO: get rid of xstream!
    String xml = (new XStream(new StaxDriver())).toXML(this);
    return xml;
  }

}
