package org.myrobotlab.net;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to describe all the necessary connection details of
 * an mrl instance
 */
public class Connection {

  /**
   * serializable references
   */
  protected Map<String, Object> serializable = new HashMap<>();
  
  /**
   * all references to connection information
   */
  transient protected Map<String, Object> attributes = new HashMap<>();

  public boolean containsKey(String key) {
    return attributes.containsKey(key);
  }

  public Object get(String key) {
    return attributes.get(key);
  }

  public Object put(String key, Object o) {
    serializable.put(key, o);
    return attributes.put(key, o);
  }

  /**
   * put in objects which should be transient - e.g. sockets all config and
   * other attributes should use put(key, object)
   * 
   * @param key
   * @param o
   * @return
   */
  public Object putTransient(String key, Object o) {
    return attributes.put(key, o);
  }

  public Object remove(String key) {
    serializable.remove(key);
    return attributes.remove(key);
  }

  public void putAll(Connection conn) {
    this.serializable.putAll(conn.serializable);
    this.attributes.putAll(conn.attributes);
  }

}
