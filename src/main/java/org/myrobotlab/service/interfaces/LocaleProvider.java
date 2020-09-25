package org.myrobotlab.service.interfaces;

import java.util.Map;

import org.myrobotlab.service.data.Locale;

public interface LocaleProvider {

  /**
   * locales this service supports - implementation can simply get
   * runtime.getLocales() if acceptable or create their own locales
   * 
   * @return
   */
  public Map<String, Locale> getLocales();

}
