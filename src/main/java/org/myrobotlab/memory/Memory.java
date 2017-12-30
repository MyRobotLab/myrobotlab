package org.myrobotlab.memory;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

public class Memory {

  public final static Logger log = LoggerFactory.getLogger(Memory.class.getCanonicalName());

  Node root = new Node("/"); // test for new Node("") & new Node(".")

  boolean autoBuildPaths = true;

  // private ArrayList<MemoryChangeListener> listeners = new
  // ArrayList<MemoryChangeListener>();
  MemoryChangeListener listener = null;

  public void addMemoryChangeListener(MemoryChangeListener listener) {
    if (this.listener == null) {
      this.listener = listener;
    } else {
      log.error("too many listeners");
      // bad form - shouldn't follow rxtx ;p
    }
  }

  // TODO - move these into Memory ???
  public void crawlAndPublish() {
    crawlAndPublish("", root);
  }

  // TODO - move these into Memory ???
  public void crawlAndPublish(String parentPath, Node currentNode) {
    log.info("{}.{}", parentPath, currentNode.getName());

    // publish recursively
    HashMap<String, Object> objects = currentNode.getNodes();
    for (Map.Entry<String, Object> o : objects.entrySet()) {
      Object value = o.getValue();
      if (value.getClass() == Node.class) {
        Node node = (Node) value;

        String newPath = String.format("%s/%s", parentPath, node.getName());
        if (listener != null) { // FIXME - don't publish root - design
          // problem !
          listener.publish(parentPath, node);
        }

        crawlAndPublish(newPath, node);
      }
    }
  }

  public Object get(String path) {
    if (path == null || path == "") // TODO - test for .getNode(null) !!!
    {
      return root;
    } else {
      // return root.getNode(path);
      return root.get(path);
    }
  }

  public Node getNode(String path) {
    return (Node) get(path);
  }

  public Node getRoot() {
    return root;
  }

  // TODO - optimization put reference in of parents ???
  public Object put(String parentPath, Node node) {
    // FIXME - A CLUE the root node name is null !

    Object object = root.get(parentPath);

    /*
     * AUTOBUILD - WOULD BE NICE if (object == null && autoBuildPaths) { int
     * pos0 = 0; int pos1 = parentPath.indexOf("/"); while (pos1 != -1) { String
     * parentKey = parentPath.substring(pos0, pos1); pos0 = pos1; pos1 =
     * parentPath.indexOf("/", pos0 + 1); String childKey =
     * parentPath.substring(pos0 + 1, pos1); log.info("adding {} -> new node {}"
     * , parentKey, childKey); put(parentKey, new Node(childKey)); if (listener
     * != null) { listener.onPut(null, node); // <-- ? needs full path? }
     * 
     * }
     * 
     * }
     */

    if (object == null) {
      log.error("could not add node {} to path {}", node.getName(), parentPath);
      return null;
    }

    Class<?> c = object.getClass();
    Object ret = null;
    if (c == Node.class) {
      Node parent = (Node) object;
      ret = parent.getNodes().put(node.getName(), node);
    } else if (c == HashMap.class) {
      // it must be data right ?
      HashMap<String, Object> data = (HashMap<String, Object>) object;
      ret = data.put(node.getName(), node);
    } else {
      log.error("wtf ??? - something besides node or hashmap !!!");
    }
    if (listener != null) {
      listener.onPut(parentPath, node);
    }
    return ret;
  }

  /*
   * public static void main(String[] args) {
   * LoggingFactory.getInstance().configure();
   * LoggingFactory.getInstance().setLevel(Level.INFO);
   * 
   * Memory memory = new Memory(); // name root ? // memory.put(null, new
   * Node("k1")); test case - delemiter in name // memory.put(new Node("k1"))
   * simplified root case memory.put("", new Node("k1")); memory.put("/k1", new
   * Node("k2")); memory.put("/k1/k2", new Node("k3")); Node node =
   * memory.getNode("/k1/k2/k3"); log.info("{}", node.getName());
   * 
   * memory.putNode("/k1/k2/k3", "k4"); log.info("{}",
   * memory.getNode("/k1/k2/k3/k4").getName()); // is this // object of // node?
   * // memory.get log.info("{}", node.getName());
   * 
   * memory.put("/k1/k2/k3", "k5", new Integer(5)); log.info("{}",
   * memory.get("/k1/k2/k3/k5"));
   * 
   * memory.toXMLFile("m1.xml");
   * 
   * memory.putNode("/k1/k2/k3/k4", "v4"); // bad test cases
   * 
   * memory.put(null, new Node(null)); memory.put(null, new Node("/"));
   * memory.put(null, new Node("/k1/k2")); memory.put("/k1/k2/", new
   * Node("k4")); memory.toXMLFile("m1.xml");
   * 
   * // memory.put("k1.k2.k3.k4", new Node("k5"));
   * 
   * }
   */

  public Object put(String path, String key, Object value) {
    Object o = root.get(path);
    Class<?> c = o.getClass();
    if (c == Node.class) {
      Node node = (Node) o;
      return node.put(key, value);
    } else {
      log.error("path {} is not to a Node", path);
      return null;
    }
  }

  /*
   * put gets the last node in path - and adds a key to the node's data with a
   * node with Node named with the second parameter
   * 
   */
  public Object putNode(String path, String nodeName) {
    Object o = root.get(path);
    Class<?> c = o.getClass();
    if (c == Node.class) {
      Node newNode = new Node(nodeName);
      Node node = (Node) o;
      return node.put(newNode);
    } else {
      log.error("path {} is not to a Node", path);
      return null;
    }

  }

  public String toJSON() {
    return CodecUtils.toJson(this);
  }

  public void toJSONFile(String string) {
    try {

      CodecUtils.toJsonFile(this, string);

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
