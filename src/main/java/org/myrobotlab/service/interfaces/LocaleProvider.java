package org.myrobotlab.service.interfaces;

import java.util.Map;

import org.myrobotlab.service.data.Locale;

public interface LocaleProvider {
  
  /**
   * set a locale from a code string.
   * The code string accepts underscore or hyphens
   * en-US fr-FR or just "en" or "fr" is the country is not needed
   * @param code
   */
  public void setLocale(String code);
  
  /**
   * get the locale's language - this would be "en" if "en-US" is the locale, or "fr" if "fr-FR" is the locale
   * ie "only" the language part of the locale
   * @return
   */
  public String getLanguage();
  
  /**
   * return currently set local
   */
  public Locale getLocale();
  
  /**
   * locales this service supports - implementation can simply get runtime.getLocales() if acceptable or create their own locales
   * @return
   */
  public Map<String, Locale> getLocales();

}
