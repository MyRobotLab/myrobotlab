package org.myrobotlab.service.interfaces;

import java.util.HashMap;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface Language extends NameProvider {

  /**
   * Get language codes
   * 
   * @return HashMap<String key, String description>
   */
  public abstract HashMap<String, String> getLanguages();

  /**
   * set language codes
   * 
   * @param l
   *          - the language code
   */
  public abstract void setLanguage(String l);

  /**
   * get current system language
   * 
   * @return String
   */
  public abstract String getLanguage();
  
}
