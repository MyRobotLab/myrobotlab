package org.myrobotlab.framework.interfaces;

public interface StateSaver {

  public boolean load();

  public boolean loadFromJson(String json);

  /**
   * save to current default location 
   * @return
   */
  public boolean save();
  
  /**
   * save to a specific location
   * @return
   */
  public boolean save(String filename);
}
