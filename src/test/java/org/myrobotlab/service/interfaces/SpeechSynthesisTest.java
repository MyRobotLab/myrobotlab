package org.myrobotlab.service.interfaces;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis.Voice;
import org.slf4j.Logger;

// TODO: this unit test is so loud!  we need a way to run it in mute mode
// also it's long based on the length of the audio being generated/played.
// TODO: find a way to validate that the mp3s are created, but don't actually play them.  (checksum? file length?  other approach?)
@Ignore
public class SpeechSynthesisTest {

  public final static Logger log = LoggerFactory.getLogger(SpeechSynthesisTest.class);

  public static void main(String[] args) {
    // TODO Auto-generated method stub
    Locale locale = Locale.getDefault();
    /*
     * Method[] methods = locale.getClass().getMethods(); for (Method method :
     * methods) { Class<?>[] paramTypes = method.getParameterTypes(); if
     * (paramTypes.length == 0) { System.out.println(String.
     * format("log.info(\"locale.%s() [{}]\", locale.%s());",
     * method.getName(),method.getName())); } else { System.out.println(String.
     * format("log.info(\"locale.%s(xxx) [{}]\", locale.%s(xxx));",
     * method.getName(),method.getName())); } }
     */

    Locale def = Locale.getDefault();

    log.info("default locale {}", def);
    log.info("default language tag {}", def.toLanguageTag());
    log.info("default display language {}", def.getDisplayLanguage());
    log.info("default display country {}", def.getDisplayCountry());

    // Locale t = new Locale(" ", "", " ");
    // Locale.setDefault(t);

    Locale[] locales = Locale.getAvailableLocales();

    // is Locale serializable
    String json = CodecUtils.toJson(locale);
    Locale l = CodecUtils.fromJson(json, Locale.class);

    Locale br = new Locale("en", "BR", "variant");
    log.info("br getLanguage - {}", br.getLanguage());
    log.info("br getDisplayLanguage - {}", br.getDisplayLanguage());
    br.toLanguageTag();
    json = CodecUtils.toJson(br);
    l = CodecUtils.fromJson(json, Locale.class);
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
    // log.info("locale.forLanguageTag(xxx) [{}]", locale.forLanguageTag(xxx));
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
    log.info("locale.getISO3Language() [{}]", locale.getISO3Language());
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
  }

  @Test
  public void testSpeechSynthesis() {
    ServiceData sd = ServiceData.getLocalInstance();
    String[] services = sd.getServiceTypeNames();

    // FIXME - just query from runtime - do not filter here ..
    // FIXME - Runtime should have filtering abilities

    Runtime.start("gui", "SwingGui");

    for (String service : services) {

      try {
        Class<?> clazz = Class.forName(service);
        AbstractSpeechSynthesis speech = null;
        // FIXME - TEST ALL WITH AND WITHOUT CLOUD
        // REQUIREMENTS SHOULD BE ALWAYS ATTEMPT CACHE FIRST e.g. POLLY SHOULD
        // WORK AFTER CACHE AND NO INTERNET !!!
        // FIXME - CACHE VOICES TO FILESYSTEM TOO !
        // FIXME - test multi-platform
        // FIXME - test publish speaking & publish audio

        if (AbstractSpeechSynthesis.class.isAssignableFrom(clazz)) {
          log.info("testing {}", clazz.getSimpleName());
          try {
            // use AbstractSpeechSynthesis
            speech = (AbstractSpeechSynthesis) Runtime.start(clazz.getSimpleName().toLowerCase(), clazz.getSimpleName());
            String toSpeak = String.format("hello my friend, I will be testing speech synthesis service called %s", clazz.getSimpleName());
            speech.speak(toSpeak);
            // FIXME - test begin middle and end files - multiple files too in
            // same text
            speech.speak("I can sound like a robot #R2D2# that is soooo cool");
            Voice voice = speech.getVoice();
            if (voice != null) {
              speech.speak(String.format("my default voice is %s, my default language is %s, my gender is %s", voice.getName(), voice.getLanguage(), voice.getGender()));
            } else {
              speech.speak("WARNING !!!  I DO NOT HAVE A VOICE");
            }

            Date now = new Date();
            // guaranteed to be unique - therefore not cached
            speech.speak(String.format("the date and time is %s", now.toString()));
            List<Voice> voices = speech.getVoices();
            speech.speak(String.format("this speech service has %d voices", voices.size()));
            // FIXME - speakBlocking
            // FIXME - test sound files #LAUGH# - really a AudioFile detail -
            // getFileList

            // FIXME - shouldn't this be done in abstract base class ?????
            // String encoded = URLEncoder.encode(toSpeak, "UTF-8");
            // एक ट्वीट मे उन्होने यह भी कहा था कि उन्हे सनी के साथ काम करने मे
            // कोई परेशानी नही है

            // FIXME - look at "main()" tests

            // FIXME - !!! UTF-8 tests !!

            // FIXME - gui tests ...

            // FIXME - after release -> Exception in thread "AWT-EventQueue-0"
            // java.lang.IndexOutOfBoundsException: Index: 9, Size:
            // 8
            // Runtime.release(clazz.getSimpleName().toLowerCase());
            // FIXME test - restarting same name service

          } catch (Exception e) {
            speech.speak(String.format("oh my ! there was an error %s", e.getMessage()));
            log.info(e.getMessage(), e);
          }
        }
      } catch (ClassNotFoundException e) {
      }
      log.info(service);
    }

  }

}
