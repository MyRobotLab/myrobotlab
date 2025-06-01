package org.myrobotlab.service.data;

import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.test.AbstractTest;

import java.util.HashMap;

import static org.junit.Assert.*;

public class LocaleTest extends AbstractTest {

  @Test
  public void testLocale() {

    Locale mrlLocale;

    // String code construction
    String code = null;

    java.util.Locale check = new java.util.Locale("zh-cmn-Hans-CN");
    // java.util.Locale check = new java.util.Locale("cmn-Hans-CN");

    // test
    // "zh-cmn-Hans-HK", "zh-cmn-Hant-TW", "zh-yue-Hant-HK", "zh-cmn-Hans-CN"
    // vs
    // "zh-cmn-Hans-HK", "zh-cmn-Hant-TW", "zh-yue-Hant-HK", "zh-cmn-Hans-CN"

    // Chinese new code
    mrlLocale = new Locale("zh-cmn-Hans-CN");
    assertEquals("CN", mrlLocale.getCountry());
    assertEquals("zh", mrlLocale.getLanguage());
    assertEquals("zh-CN", mrlLocale.getTag());
    assertEquals("Chinese", mrlLocale.getDisplayLanguage());
    assertEquals("China", mrlLocale.getDisplayCountry());
    assertEquals("zh-CN", mrlLocale.toString());

    // Chinese invalid? code
    /*
     * locale = new Locale("cmn-Hans-CN"); assertEquals("US",
     * locale.getCountry()); assertEquals("en", locale.getLanguage());
     * assertEquals("en-US", locale.getTag()); assertEquals("English",
     * locale.getDisplayLanguage()); assertEquals("United States",
     * locale.getDisplayCountry()); assertEquals("en-US", locale.toString());
     */

    mrlLocale = new Locale("zh-cmn-Hant-TW");
    assertEquals("TW", mrlLocale.getCountry());
    assertEquals("zh", mrlLocale.getLanguage());
    assertEquals("zh-TW", mrlLocale.getTag());
    assertEquals("Chinese", mrlLocale.getDisplayLanguage());
    assertEquals("Taiwan", mrlLocale.getDisplayCountry());
    assertEquals("zh-TW", mrlLocale.toString());

    /*
    locale = new Locale(code);
    json = CodecUtils.toJson(locale);
    assertEquals("{}", json);
    assertEquals(null, locale.getLanguage());
    */

    code = "-";
    mrlLocale = new Locale(code);
    assertNull(mrlLocale.getLanguage());

    code = " - ";
    mrlLocale = new Locale(code);
    assertNull(mrlLocale.getLanguage());

    code = "  ";
    mrlLocale = new Locale(code);
    assertNull(mrlLocale.getLanguage());

    code = "";
    mrlLocale = new Locale(code);
    assertNull(mrlLocale.getLanguage());

    code = "-uS";
    mrlLocale = new Locale(code);
    assertEquals("US", mrlLocale.getCountry());

    code = "EN_us";
    mrlLocale = new Locale(code);
    assertEquals("US", mrlLocale.getCountry());
    assertEquals("en", mrlLocale.getLanguage());
    assertEquals("en-US", mrlLocale.getTag());
    assertEquals("English", mrlLocale.getDisplayLanguage());
    assertEquals("United States", mrlLocale.getDisplayCountry());

    code = "en-";
    mrlLocale = new Locale(code);
    assertEquals("en", mrlLocale.getLanguage());
    assertEquals("en", mrlLocale.getTag());
    assertNull(mrlLocale.getCountry());

    mrlLocale = new Locale("en-us");
    assertEquals("US", mrlLocale.getCountry());
    assertEquals("en", mrlLocale.getLanguage());
    assertEquals("en-US", mrlLocale.getTag());
    assertEquals("English", mrlLocale.getDisplayLanguage());
    assertEquals("United States", mrlLocale.getDisplayCountry());
    assertEquals("en-US", mrlLocale.toString());

    // check toString

    // check serialization

    // cmn-Hans-CN

  }

  
  @Test
  public void testJavaLocale() {
    try {

      java.util.Locale locale = java.util.Locale.getDefault();

      java.util.Locale def = java.util.Locale.getDefault();

      log.info("default locale {}", def);
      log.info("default language tag {}", def.toLanguageTag());
      log.info("default display language {}", def.getDisplayLanguage());
      log.info("default display country {}", def.getDisplayCountry());

      // java.util.Locale t = new java.util.Locale(" ", "", " ");
      // java.util.Locale.setDefault(t);

      java.util.Locale[] locales = java.util.Locale.getAvailableLocales();

      // is java.util.Locale serializable
      String json = CodecUtils.toJson(locale);
      
      java.util.Locale l = CodecUtils.fromJson(json, java.util.Locale.class);

      java.util.Locale br = new java.util.Locale("en", "BR", "variant");
      log.info("br getLanguage - {}", br.getLanguage());
      log.info("br getDisplayLanguage - {}", br.getDisplayLanguage());
      br.toLanguageTag();
      json = CodecUtils.toJson(br);
      l = CodecUtils.fromJson(json, java.util.Locale.class);
      br.equals(l);

      l = new java.util.Locale("en_BR_variant");
      l.getLanguage();

      HashMap<String, String> languagesList = new HashMap<String, String>();
      for (int i = 0; i < locales.length; i++) {
        log.info(locales[i].toLanguageTag());
        languagesList.put(locales[i].toLanguageTag(), locales[i].getDisplayLanguage());
      }

      // java.util.Locale.

      log.info("locale.equals(xxx) [{}]", locale.equals(java.util.Locale.getDefault()));
      log.info("locale.toString() [{}]", locale.toString());
      log.info("locale.hashCode() [{}]", locale.hashCode());
      log.info("locale.clone() [{}]", locale.clone());
      log.info("java.util.Locale.getDefault(java.util.Locale.Category.DISPLAY)) [{}]", java.util.Locale.getDefault(java.util.Locale.Category.DISPLAY));

      log.info("new java.util.Locale(\"en\") [{}]", new java.util.Locale("en"));
      log.info("new java.util.Locale(\"en\", \"US\") [{}]", new java.util.Locale("en", "US"));
      log.info("new java.util.Locale(\"xx\", \"US\") [{}]", new java.util.Locale("xx", "US"));
      log.info("new java.util.Locale(\"en\", \"US\", \"variant\") [{}]", new java.util.Locale("en", "US", "variant"));
      log.info("locale.getDefault() [{}]", java.util.Locale.getDefault());
      log.info("locale.getLanguage() [{}]", locale.getLanguage());
      log.info("java.util.Locale.forLanguageTag(\"en-US\") [{}]", java.util.Locale.forLanguageTag("en-US"));

      // lots of filtering and lookup capability with rfc specifications
      // log.info("locale.lookup(xxx) [{}]", locale.lookup(xxx));
      // log.info("locale.filter(xxx) [{}]", locale.filter(xxx));
      // log.info("locale.filter(xxx) [{}]", locale.filter(xxx));
      // log.info("locale.filterTags(xxx) [{}]", locale.filterTags(xxx));
      // log.info("locale.filterTags(xxx) [{}]", locale.filterTags(xxx));
      // log.info("locale.forLanguageTag(xxx) [{}]",
      // locale.forLanguageTag(xxx));
      log.info("locale.getAvailableLocales() [{}]", java.util.Locale.getAvailableLocales());
      log.info("locale.getCountry() [{}]", locale.getCountry());
      log.info("locale.getDisplayCountry() [{}]", locale.getDisplayCountry());
      log.info("locale.getDisplayCountry(xxx) [{}]", locale.getDisplayCountry(java.util.Locale.getDefault()));
      log.info("locale.getDisplayLanguage(xxx) [{}]", locale.getDisplayLanguage(java.util.Locale.getDefault()));
      log.info("locale.getDisplayLanguage() [{}]", locale.getDisplayLanguage());
      log.info("locale.getDisplayName() [{}]", locale.getDisplayName());
      log.info("locale.getDisplayName(xxx) [{}]", locale.getDisplayName(java.util.Locale.getDefault()));
      log.info("locale.getDisplayScript() [{}]", locale.getDisplayScript());
      log.info("locale.getDisplayScript(xxx) [{}]", locale.getDisplayScript(java.util.Locale.getDefault()));
      log.info("locale.getDisplayVariant() [{}]", locale.getDisplayVariant());
      log.info("locale.getDisplayVariant(xxx) [{}]", locale.getDisplayVariant(java.util.Locale.getDefault()));
      // log.info("locale.getExtension(xxx) [{}]", locale.getExtension(xxx));
      log.info("locale.getExtensionKeys() [{}]", locale.getExtensionKeys());
      log.info("locale.getISO3Country() [{}]", locale.getISO3Country());
      // log.info("locale.getISO3Language() [{}]", locale.getISO3Language()); -
      // throws java.util.MissingResourceException: Couldn't find 3-letter
      // language code for en-us
      log.info("locale.getISOCountries() [{}]", java.util.Locale.getISOCountries());
      log.info("locale.getISOLanguages() [{}]", java.util.Locale.getISOLanguages());
      log.info("locale.getScript() [{}]", locale.getScript());
      log.info("locale.getUnicodeLocaleAttributes() [{}]", locale.getUnicodeLocaleAttributes());
      log.info("locale.getUnicodeLocaleKeys() [{}]", locale.getUnicodeLocaleKeys());
      // log.info("locale.getUnicodeLocaleType(xxx) [{}]",
      // locale.getUnicodeLocaleType(xxx));
      log.info("locale.getVariant() [{}]", locale.getVariant());
      log.info("locale.hasExtensions() [{}]", locale.hasExtensions());

      java.util.Locale.setDefault(new java.util.Locale("en", "BR"));
      // log.info("locale.lookupTag(xxx) [{}]", locale.lookupTag("en"));
      // log.info("locale.setDefault(xxx) [{}]", ));
      // log.info("locale.setDefault(xxx) [{}]", locale.setDefault(
      // log.info("locale.stripExtensions() [{}]", locale.stripExtensions());
      log.info("locale.toLanguageTag() [{}]", locale.toLanguageTag());
      System.out.println(locale.toString());
    } catch (Exception e) {
      log.error("locale test failed", e);
      assertTrue(false);
    }
  }

}
