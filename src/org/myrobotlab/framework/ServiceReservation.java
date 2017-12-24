package org.myrobotlab.framework;

import java.io.Serializable;

/**
 * ServiceRegistration is a data object containing information regarding a
 * "peer" service within a "composite" service. The Composite service utilizes
 * or controls multiple peer services. In order to do so it needs an internal
 * key used only by the composite service. With this key the composite gets a
 * reference to the peer. In some situations it will be necessary to use a peer
 * with a different name. reserveAs is a method which will re-bind the composite
 * to a differently named peer service.
 * 
 */
public class ServiceReservation implements Serializable {
  private static final long serialVersionUID = 1L;
  // FIXME MAKE KEY FINAL !
  public final String key; // FIXME - remove completely - exists only in Index
  public String actualName;
  public String fullTypeName;
  public String comment;

  public boolean isRoot = false;

  public ServiceReservation(String key, String typeName, String comment) {
    this.key = key;
    this.actualName = key;
    if (!typeName.contains(".")) {
      this.fullTypeName = String.format("org.myrobotlab.service.%s", typeName);
    } else {
      this.fullTypeName = typeName;
    }
    this.comment = comment;
  }

  public ServiceReservation(String key, String actualName, String typeName, String comment) {
    this.key = key;
    this.actualName = actualName;
    if (typeName != null && !typeName.contains(".")) {
      this.fullTypeName = String.format("org.myrobotlab.service.%s", typeName);
    } else {
      this.fullTypeName = typeName;
    }
    this.comment = comment;
  }

  public ServiceReservation(String key, String actualName, String typeName, String comment, boolean isRoot) {
    this.key = key;
    this.actualName = actualName;
    if (!typeName.contains(".")) {
      this.fullTypeName = String.format("org.myrobotlab.service.%s", typeName);
    } else {
      this.fullTypeName = typeName;
    }
    this.comment = comment;
    this.isRoot = isRoot;
  }

  // FIXME - clean up data entry - so this doesnt need the logic !!
  public String getSimpleName() {
    if (fullTypeName != null && fullTypeName.contains(".")) {
      return fullTypeName.substring(fullTypeName.lastIndexOf(".") + 1);
    } else {
      return fullTypeName;
    }

  }

  @Override
  public String toString() {
    // return gson.toJson(this);
    StringBuffer sb = new StringBuffer();

    /*
     * if (!key.equals(actualName)){ sb.append("("); sb.append(key);
     * sb.append(")"); }
     */

    /*
     * if (!key.equals(actualName)) { sb.append("["); sb.append(actualName);
     * sb.append("] "); }
     */
    sb.append("[");
    sb.append(actualName);
    sb.append("] ");

    if (isRoot) {
      sb.append("isRoot");
    }

    sb.append(getSimpleName());
    sb.append(" - ");
    sb.append(comment);

    return sb.toString();
  }

}
