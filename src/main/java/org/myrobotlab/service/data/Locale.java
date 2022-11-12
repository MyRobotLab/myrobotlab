package org.myrobotlab.service.data;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * 
 * A Locale unlike Java's which actually serializes all the parts
 *
 */
public class Locale {

  public final static Logger log = LoggerFactory.getLogger(Locale.class);

  /**
   * 2 letter iso language
   */
  String language;
  String displayLanguage;
  /**
   * two letter country code
   */
  String country;
  String displayCountry;

  /**
   * e.g. en-US, en, en-BR, fr, fr-FR are all valid tags
   */
  String tag;

  /**
   * correct conversion form java.util.Locale to mrl Locale
   * 
   * @param locale
   *          the java locale
   */
  public Locale(java.util.Locale locale) {
    this(locale.toString());
  }

  public Locale(String code) {
    if (code == null) {
      code = java.util.Locale.getDefault().toString();
    }

    // trim
    code = code.trim();
    if (code.length() == 0) {
      return;
    }

    // convert underscores to hyphens
    code = code.replace("_", "-");

    // TODO - check for 0 length
    if (code.contains("-")) {
      String parts[] = code.split("-");
      if (parts.length == 0) {
        return;
      }
      // first part is always the "language" descriptor
      language = parts[0].toLowerCase();
      if (parts.length > 1) {

        // IETF - "last" part is country/region
        String p = parts[parts.length - 1].toUpperCase();
        if (p.length() > 0) {
          country = p;
        }
        // language = code.substring(0, code.lastIndexOf("-"));
      }
    } else {
      language = code;
    }

    // get the display language from the java locale
    displayLanguage = new java.util.Locale(language).getDisplayLanguage();

    if (country != null) {
      // get the display language from the java locale
      displayCountry = new java.util.Locale(language, country).getDisplayCountry();
    }

    // form tag
    if (country != null && country.length() > 0) {
      tag = language + "-" + country;
    } else {
      tag = language;
    }
  }

  public Locale(String language, String country) {
    this(((language == null) ? "" : language + ((country == null) ? "" : "-" + country)));
  }

  public static Map<String, Locale> getLocaleMap(String... codes) {
    Map<String, Locale> ret = new TreeMap<>();
    for (String code : codes) {
      Locale l = new Locale(new java.util.Locale(code));
      ret.put(l.getTag(), l);
    }
    return ret;
  }

  public static Map<String, Locale> getLanguageMap(String... codes) {
    Map<String, Locale> ret = new TreeMap<>();
    for (String code : codes) {
      Locale l = new Locale(new java.util.Locale(code));
      ret.put(l.language, l);
    }
    return ret;
  }

  public String getTag() {
    return tag;
  }

  public static Locale getDefault() {
    return new Locale(java.util.Locale.getDefault());
  }

  public String getLanguage() {
    return language;
  }

  public String getDisplayLanguage() {
    return displayLanguage;
  }

  public String getCountry() {
    return country;
  }

  public String getDisplayCountry() {
    return displayCountry;
  }

  public static Map<String, Locale> getDefaults() {

    Map<String, Locale> locales = new TreeMap<>();
    java.util.Locale[] ls = java.util.Locale.getAvailableLocales();
    for (java.util.Locale l : ls) {
      Locale newLocale = new Locale(l.toString());
      if (l.toString() != null && l.toString().length() != 0) {
        locales.put(newLocale.tag, newLocale);
      }
    }
    return locales;
  }

  public static Map<String, Locale> getAvailableLanguages() {

    Map<String, Locale> locales = new TreeMap<>();
    java.util.Locale[] ls = java.util.Locale.getAvailableLocales();

    for (int i = 0; i < ls.length; ++i) {
      java.util.Locale l = ls[i];
      if (l.getLanguage() != null) {
        locales.put(l.getLanguage(), new Locale(l));
      }
    }
    return locales;
  }

  public java.util.Locale transform() {
    return new java.util.Locale(getTag());
  }

  @Override
  public String toString() {
    return getTag();
  }

  final static public boolean hasLanguage(Map<String, Locale> locales, String language) {
    if (language == null || locales == null) {
      return false;
    }
    // let Locale parse the incoming string to be safe
    Locale l = new Locale(language);
    for (Locale locale : locales.values()) {
      if (locale.getLanguage().contentEquals(l.getLanguage())) {
        return true;
      }
    }
    return false;
  }

  final static public Properties loadLocalizations(String fullPath) {
    Properties props = new Properties();
    try {
      props.load(new InputStreamReader(new FileInputStream(fullPath), Charset.forName("UTF-8")));
      log.debug("found {} properties from {}", props.size(), fullPath);
    } catch (Exception e) {
      /* don't care common use case */
      log.debug("{} does not exist", fullPath);
    }
    return props;
  }

}
