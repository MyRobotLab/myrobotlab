package org.myrobotlab.framework.repo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

import org.myrobotlab.framework.interfaces.NameTypeProvider;

public class Category implements Comparator<Category>, Serializable, NameTypeProvider {
  private static final long serialVersionUID = 1L;
  public String name;
  public String description;
  public ArrayList<String> serviceTypes = new ArrayList<String>();

  @Override
  public int compare(Category o1, Category o2) {
    return o1.name.compareTo(o2.name);
  }

  @Override
  public String getName() {    
    return name;
  }

  /**
   * a categories type "is" its name...
   */
  @Override
  public String getSimpleName() {
    return name;
  }
}
