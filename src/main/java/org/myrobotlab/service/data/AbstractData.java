package org.myrobotlab.service.data;

/**
 * An attempt to try to normalize some of the common fields of these pojos
 * 
 * @author grog
 *
 */
public abstract class AbstractData {

  public String source;

  public AbstractData(String name) {
    source = name;
  }

}
