package org.myrobotlab.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class IndexNode<T> {

  public final static Logger log = LoggerFactory.getLogger(IndexNode.class);
  private HashMap<String, IndexNode<T>> branches = new HashMap<String, IndexNode<T>>();
  private T value;

  public IndexNode() {
  }

  public IndexNode(T value) {
    // leaf value
    this.value = value;
  }

  public void buildTree(IndexNode<T> node, String key, T value) {
    // if there is a "." - then we are a branch - not a leaf
    if (key.indexOf(".") > 0) {
      // if (key.length() > 0) {
      // build branches of tree
      String branch = key.substring(0, key.indexOf("."));
      String theRest = key.substring(key.indexOf(".") + 1);
      log.info(String.format("adding branch %s", branch));
      if (!node.branches.containsKey(branch)) {
        // log.debug(String.format("adding branch %s", branch));
        IndexNode<T> twig = new IndexNode<T>();
        node.branches.put(branch, twig);
      }
      buildTree(node.getNode(branch), theRest, value);
    } else {
      // add a leaf
      log.debug(String.format("adding leaf %s=%s", key, value));
      node.branches.put(key, new IndexNode<T>(value));
    }
  }

  public void clear() {
    branches.clear();
  }

  public boolean containsKey(String key) {
    IndexNode<T> node = getNode(key);
    return node != null;
  }

  // http://stackoverflow.com/questions/2319538/most-concise-way-to-convert-a-setstring-to-a-liststring
  // FIXME not the most effecient - should simply return the Set<Entry<String,
  // IndexNode<ServiceReservation>>> from each branches !!!
  // good thing you put the key in the data node :P (lame)
  // limitCount = 0 means no limit
  public ArrayList<T> crawlForData(ArrayList<T> data, int limitCount, String key) {

    if (value != null) {
      // found some data add it
      data.add(value);
    }
    if (key != null && key.contains(".")) {
      String subkey = key.substring(0, 1);
      String climb = key.substring(1);
      IndexNode<T> t = branches.get(subkey);
      if (t == null) {
        return data; // nothing here - return what was passed in
      } else {
        t.crawlForData(data, limitCount, climb); // climb some more
      }
    } else {
      // no key at this point - just get data
      if (limitCount < 1 || data.size() < limitCount) {
        // still under our data limit
        // look for more data - by crawling up children
        for (String k : branches.keySet()) {
          // return crawlForData(data, limitCount,
          // String.format("%s%s",key,k));
          // FIXME - data structure change will do concurrent mod erro
          IndexNode<T> n = branches.get(k);
          n.crawlForData(data, limitCount, "");
        }
      }

    }

    return data;
  }

  public HashMap<String, IndexNode<T>> getBranches() {
    return branches;
  }

  public IndexNode<T> getNode(String key) {
    if (key == null) {
      return this;
    }
    IndexNode<T> target = null;
    if (key.contains(".")) {
      String subkey = key.substring(0, key.indexOf("."));
      String climb = key.substring(key.indexOf(".") + 1);
      IndexNode<T> t = branches.get(subkey);
      if (t == null) {
        return null;
      } else {
        return t.getNode(climb);
      }
    } else {
      target = branches.get(key);
    }

    return target;

  }

  public T getValue() {
    return value;
  }

  public T getValue(T defaultValue) {
    return (value == null) ? defaultValue : value;
  }

  public Enumeration<String> propertyNames() {
    Vector<String> n = new Vector<String>(branches.keySet());
    return n.elements();
  }

  public Enumeration<String> propertySortedNames() {
    List<String> n = new Vector<String>(branches.keySet());
    Collections.sort(n);
    return ((Vector<String>) n).elements();
  }

  public IndexNode<T> putNode(String key, T value) {
    if (key == null) {
      setValue(value);
      return this;
    }

    if (key.contains(".")) {
      // if (key.length() > 1) {
      // find the last node we from which
      // we need to start building branches
      // String subkey = key.substring(0, 1);
      // String climb = key.substring(1);
      String subkey = key.substring(0, key.indexOf("."));
      String climb = key.substring(key.indexOf(".") + 1);

      IndexNode<T> t = branches.get(subkey);
      if (t == null) {
        // no branch - we need to build it
        IndexNode<T> twig = new IndexNode<T>();
        branches.put(subkey, twig);
        return twig.putNode(climb, value);
      } else {
        return t.putNode(climb, value);
      }
    } else {
      IndexNode<T> targetNode = branches.get(key);
      if (targetNode == null) {
        return branches.put(key, new IndexNode<T>(value));
      } else {
        targetNode.setValue(value);
        return targetNode;
      }
    }

    // return destination;
  }

  public void setValue(T value) {
    this.value = value;
  }

  public int size() {
    return branches.size();
  }

  @Override
  public String toString() {
    return toString(null);
  }

  public String toString(String contextPath) {
    StringBuffer sb = new StringBuffer();

    if (contextPath != null && getValue() != null) {
      sb.append(contextPath);
      sb.append("=");
      sb.append(getValue());
      sb.append("\n");
    }

    for (String key : branches.keySet()) {

      if (contextPath != null) {
        sb.append(branches.get(key).toString(contextPath + "." + key));
      } else {
        sb.append(branches.get(key).toString(key));
      }
    }

    return sb.toString();
  }

}
