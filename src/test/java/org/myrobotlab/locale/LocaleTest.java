package org.myrobotlab.locale;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Locale;

import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class LocaleTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(LocaleTest.class);

  @Test
  public void testLocale() {
    try {

      Locale locale = Locale.getDefault();

      Locale def = Locale.getDefault();

      log.info("default locale {}", def);
      log.info("default language tag {}", def.toLanguageTag());
      log.info("default display language {}", def.getDisplayLanguage());
      log.info("default display country {}", def.getDisplayCountry());

      // Locale t = new Locale(" ", "", " ");
      // Locale.setDefault(t);

      Locale[] locales = Locale.getAvailableLocales();
      String[] isoLanguages = Locale.getISOLanguages();
      String[] isoCountries = Locale.getISOCountries();

      // is Locale serializable
      Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").setPrettyPrinting().disableHtmlEscaping().create();
      String json = gson.toJson(locale);
      Locale l = gson.fromJson(json, Locale.class);

      Locale br = new Locale("en", "BR", "variant");
      log.info("br getLanguage - {}", br.getLanguage());
      log.info("br getDisplayLanguage - {}", br.getDisplayLanguage());
      br.toLanguageTag();
      json = gson.toJson(br);
      l = gson.fromJson(json, Locale.class);
      br.equals(l);

      l = new Locale("en_BR_variant");
      l.getLanguage();

      HashMap<String, String> languagesList = new HashMap<String, String>();
      for (int i = 0; i < locales.length; i++) {
        log.info(locales[i].toLanguageTag());
        languagesList.put(locales[i].toLanguageTag(), locales[i].getDisplayLanguage());
      }

      // Locale.

      log.info("locale.equals(xxx) [{}]", locale.equals(Locale.getDefault()));
      log.info("locale.toString() [{}]", locale.toString());
      log.info("locale.hashCode() [{}]", locale.hashCode());
      log.info("locale.clone() [{}]", locale.clone());
      log.info("Locale.getDefault(Locale.Category.DISPLAY)) [{}]", Locale.getDefault(Locale.Category.DISPLAY));

      log.info("new Locale(\"en\") [{}]", new Locale("en"));
      log.info("new Locale(\"en\", \"US\") [{}]", new Locale("en", "US"));
      log.info("new Locale(\"xx\", \"US\") [{}]", new Locale("xx", "US"));
      log.info("new Locale(\"en\", \"US\", \"variant\") [{}]", new Locale("en", "US", "variant"));
      log.info("locale.getDefault() [{}]", Locale.getDefault());
      log.info("locale.getLanguage() [{}]", locale.getLanguage());
      log.info("Locale.forLanguageTag(\"en-US\") [{}]", Locale.forLanguageTag("en-US"));

      // lots of filtering and lookup capability with rfc specifications
      // log.info("locale.lookup(xxx) [{}]", locale.lookup(xxx));
      // log.info("locale.filter(xxx) [{}]", locale.filter(xxx));
      // log.info("locale.filter(xxx) [{}]", locale.filter(xxx));
      // log.info("locale.filterTags(xxx) [{}]", locale.filterTags(xxx));
      // log.info("locale.filterTags(xxx) [{}]", locale.filterTags(xxx));
      // log.info("locale.forLanguageTag(xxx) [{}]",
      // locale.forLanguageTag(xxx));
      log.info("locale.getAvailableLocales() [{}]", Locale.getAvailableLocales());
      log.info("locale.getCountry() [{}]", locale.getCountry());
      log.info("locale.getDisplayCountry() [{}]", locale.getDisplayCountry());
      log.info("locale.getDisplayCountry(xxx) [{}]", locale.getDisplayCountry(Locale.getDefault()));
      log.info("locale.getDisplayLanguage(xxx) [{}]", locale.getDisplayLanguage(Locale.getDefault()));
      log.info("locale.getDisplayLanguage() [{}]", locale.getDisplayLanguage());
      log.info("locale.getDisplayName() [{}]", locale.getDisplayName());
      log.info("locale.getDisplayName(xxx) [{}]", locale.getDisplayName(Locale.getDefault()));
      log.info("locale.getDisplayScript() [{}]", locale.getDisplayScript());
      log.info("locale.getDisplayScript(xxx) [{}]", locale.getDisplayScript(Locale.getDefault()));
      log.info("locale.getDisplayVariant() [{}]", locale.getDisplayVariant());
      log.info("locale.getDisplayVariant(xxx) [{}]", locale.getDisplayVariant(Locale.getDefault()));
      // log.info("locale.getExtension(xxx) [{}]", locale.getExtension(xxx));
      log.info("locale.getExtensionKeys() [{}]", locale.getExtensionKeys());
      log.info("locale.getISO3Country() [{}]", locale.getISO3Country());
      // log.info("locale.getISO3Language() [{}]", locale.getISO3Language()); - throws java.util.MissingResourceException: Couldn't find 3-letter language code for en-us
      log.info("locale.getISOCountries() [{}]", Locale.getISOCountries());
      log.info("locale.getISOLanguages() [{}]", Locale.getISOLanguages());
      log.info("locale.getScript() [{}]", locale.getScript());
      log.info("locale.getUnicodeLocaleAttributes() [{}]", locale.getUnicodeLocaleAttributes());
      log.info("locale.getUnicodeLocaleKeys() [{}]", locale.getUnicodeLocaleKeys());
      // log.info("locale.getUnicodeLocaleType(xxx) [{}]",
      // locale.getUnicodeLocaleType(xxx));
      log.info("locale.getVariant() [{}]", locale.getVariant());
      log.info("locale.hasExtensions() [{}]", locale.hasExtensions());

      Locale.setDefault(new Locale("en", "BR"));
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