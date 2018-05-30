package org.myrobotlab.service.abstracts;

import java.util.HashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.Language;

/**
 * One place for standardized language things for every services
 * 
 * @author moz4r
 */
public abstract class AbstractLanguage extends Service implements Language {
  public HashMap<String, String> languagesList = new HashMap<String, String>();
  private transient HashMap<String, String> languagesListISO = new HashMap<String, String>();
  public String currentLanguage = getLanguage();

  private static final long serialVersionUID = 1L;

  public AbstractLanguage(String reservedKey) {
    super(reservedKey);
    languagesList.put("en-US", "English - United States");
    languagesList.put("en-GB", "English - British");
    languagesList.put("af-ZA", "Afrikaans");
    languagesList.put("id-ID", "Bahasa Indonesia");
    languagesList.put("ms-MY", "Bahasa Melayu");
    languagesList.put("ca-ES", "Català");
    languagesList.put("cs-CZ", "Čeština");
    languagesList.put("da-DK", "Dansk");
    languagesList.put("de-DE", "Deutsch");
    languagesList.put("en-AU", "English - Australia");
    languagesList.put("en-CA", "English - Canada");
    languagesList.put("en-IN", "English - India");
    languagesList.put("en-NZ", "English - New Zealand");
    languagesList.put("en-ZA", "English - South Africa");
    languagesList.put("en-GB", "English - United Kingdom");
    languagesList.put("en-US", "English - United States");
    languagesList.put("es-AR", "Español - Argentina");
    languagesList.put("es-BO", "Español - Bolivia");
    languagesList.put("es-CL", "Español - Chile");
    languagesList.put("es-CO", "Español - Colombia");
    languagesList.put("es-CR", "Español - Costa Rica");
    languagesList.put("es-EC", "Español - Ecuador");
    languagesList.put("es-SV", "Español - El Salvador");
    languagesList.put("es-ES", "Español - España");
    languagesList.put("es-US", "Español - Estados Unidos");
    languagesList.put("es-GT", "Español - Guatemala");
    languagesList.put("es-HN", "Español - Honduras");
    languagesList.put("es-MX", "Español - México");
    languagesList.put("es-NI", "Español - Nicaragua");
    languagesList.put("es-PA", "Español - Panamá");
    languagesList.put("es-PY", "Español - Paraguay");
    languagesList.put("es-PE", "Español - Perú");
    languagesList.put("es-PR", "Español - Puerto Rico");
    languagesList.put("es-DO", "Español - República Dominicana");
    languagesList.put("es-UY", "Español - Uruguay");
    languagesList.put("es-VE", "Español - Venezuela");
    languagesList.put("eu-ES", "Euskara");
    languagesList.put("fil-PH", "Filipino");
    languagesList.put("fr-FR", "Français");
    languagesList.put("gl-ES", "Galego");
    languagesList.put("hi-IN", "Hindi - हिंदी");
    languagesList.put("hr_HR", "Hrvatski");
    languagesList.put("zu-ZA", "IsiZulu");
    languagesList.put("is-IS", "Íslenska");
    languagesList.put("it-IT", "Italiano - Italia");
    languagesList.put("it-CH", "Italiano - Svizzera");
    languagesList.put("lt-LT", "Lietuvių");
    languagesList.put("hu-HU", "Magyar");
    languagesList.put("nl-NL", "Nederlands");
    languagesList.put("nb-NO", "Norsk bokmål");
    languagesList.put("pl-PL", "Polski");
    languagesList.put("pt-BR", "Português - Brasil");
    languagesList.put("pt-PT", "Português - Portugal");
    languagesList.put("ro-RO", "Română");
    languagesList.put("sl-SI", "Slovenščina");
    languagesList.put("sk-SK", "Slovenčina");
    languagesList.put("fi-FI", "Suomi");
    languagesList.put("sv-SE", "Svenska");
    languagesList.put("vi-VN", "Tiếng Việt");
    languagesList.put("tr-TR", "Türkçe");
    languagesList.put("el-GR", "Ελληνικά");
    languagesList.put("bg-BG", "български");
    languagesList.put("ru-RU", "Pусский");
    languagesList.put("sr-RS", "Српски");
    languagesList.put("uk-UA", "Українська");
    languagesList.put("ko-KR", "한국어");
    languagesList.put("cmn-Hans-CN", "中文 - 普通话 (中国大陆)");
    languagesList.put("cmn-Hans-HK", "中文 - 普通话 (香港)");
    languagesList.put("cmn-Hant-TW", "中文 - 中文 (台灣)");
    languagesList.put("yue-Hant-HK", "中文 - 粵語 (香港)");
    languagesList.put("ja-JP", "日本語");
    languagesList.put("th-TH", "ภาษาไทย");

    languagesListISO.put("th", "th-TH");
    languagesListISO.put("hi", "hi-IN");
    languagesListISO.put("ja", "ja-JP");
    languagesListISO.put("zh", "cmn-Hans-CN");
    languagesListISO.put("ko", "ko-KR");
    languagesListISO.put("uk", "uk-UA");
    languagesListISO.put("sr", "sr-RS");
    languagesListISO.put("ru", "ru-RU");
    languagesListISO.put("bg", "bg-BG");
    languagesListISO.put("el", "el-GR");
    languagesListISO.put("tr", "tr-TR");
    languagesListISO.put("vi", "vi-VN");
    languagesListISO.put("sv", "sv-SE");
    languagesListISO.put("fi", "fi-FI");
    languagesListISO.put("sk", "sk-SK");
    languagesListISO.put("sl", "sl-SI");
    languagesListISO.put("ro", "ro-RO");
    languagesListISO.put("pt", "pt-PT");
    languagesListISO.put("nb", "nb-NO");
    languagesListISO.put("nl", "nl-NL");
    languagesListISO.put("hu", "hu-HU");
    languagesListISO.put("lt", "lt-LT");
    languagesListISO.put("hr", "hr_HR");
    languagesListISO.put("is", "is-IS");
    languagesListISO.put("it", "it-IT");
    languagesListISO.put("fr", "fr-FR");
    languagesListISO.put("es", "es-ES");
    languagesListISO.put("de", "de-DE");

    languagesListISO.put("da", "da-DK");
    languagesListISO.put("cs", "cs-CZ");
    languagesListISO.put("en", "en-US");

  }

  public HashMap<String, String> getLanguages() {
    return languagesList;
  }

  public String getLanguage() {
    if (Runtime.getInstance().getLanguage() == null) {
      setLanguage("en");
    }
    return Runtime.getInstance().getLanguage();
  }

  public void setLanguage(String language) {
    if (languagesList.containsKey(language)) {
      Runtime.getInstance().setLanguage(language);
      info("Set system language to : %s", languagesList.get(language));
    }
    // ISO-639-1 codes support
    else if (languagesListISO.containsKey(language)) {
      Runtime.getInstance().setLanguage(languagesListISO.get(language));
      info("Set system language to : %s", languagesList.get(languagesListISO.get(language)));
    } else {
      error("This language is not supported : %s", language);
    }
    currentLanguage = getLanguage();
    broadcastState();
  }
}
