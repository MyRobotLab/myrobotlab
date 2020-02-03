package org.myrobotlab.service.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.test.AbstractTest;

public class LocaleTest extends AbstractTest {

  @Test
  public void testLocale() {

    Locale locale;

    // String code construction
    String code = null;
    String json = null;
    
    java.util.Locale check = new java.util.Locale("zh-cmn-Hans-CN");
   // java.util.Locale check = new java.util.Locale("cmn-Hans-CN");
    
    
    // test
    // "zh-cmn-Hans-HK", "zh-cmn-Hant-TW", "zh-yue-Hant-HK", "zh-cmn-Hans-CN"
    // vs
    // "zh-cmn-Hans-HK", "zh-cmn-Hant-TW", "zh-yue-Hant-HK", "zh-cmn-Hans-CN"
    
    // Chinese new code
    locale = new Locale("zh-cmn-Hans-CN");
    assertEquals("CN", locale.getCountry());
    assertEquals("zh", locale.getLanguage());
    assertEquals("zh-CN", locale.getTag());
    assertEquals("Chinese", locale.getDisplayLanguage());
    assertEquals("China", locale.getDisplayCountry());
    assertEquals("zh-CN", locale.toString());
    
    
    // Chinese invalid? code
    /*
    locale = new Locale("cmn-Hans-CN");
    assertEquals("US", locale.getCountry());
    assertEquals("en", locale.getLanguage());
    assertEquals("en-US", locale.getTag());
    assertEquals("English", locale.getDisplayLanguage());
    assertEquals("United States", locale.getDisplayCountry());
    assertEquals("en-US", locale.toString());
    */
    
    
    locale = new Locale("zh-cmn-Hant-TW");
    assertEquals("TW", locale.getCountry());
    assertEquals("zh", locale.getLanguage());
    assertEquals("zh-TW", locale.getTag());
    assertEquals("Chinese", locale.getDisplayLanguage());
    assertEquals("Taiwan", locale.getDisplayCountry());
    assertEquals("zh-TW", locale.toString());
    

    locale = new Locale(code);
    json = CodecUtils.toJson(locale);
    assertEquals("{}", json);
    assertEquals(null, locale.getLanguage());

    code = "-";
    locale = new Locale(code);
    json = CodecUtils.toJson(locale);
    assertEquals("{}", json);
    assertEquals(null, locale.getLanguage());

    code = " - ";
    locale = new Locale(code);
    json = CodecUtils.toJson(locale);
    assertEquals("{}", json);
    assertEquals(null, locale.getLanguage());


    code = "  ";
    locale = new Locale(code);
    json = CodecUtils.toJson(locale);
    assertEquals("{}", json);
    assertEquals(null, locale.getLanguage());
    
    code = "";
    locale = new Locale(code);
    json = CodecUtils.toJson(locale);
    assertEquals("{}", json);
    assertEquals(null, locale.getLanguage());

    code = "-uS";
    locale = new Locale(code);
    json = CodecUtils.toJson(locale);
    assertEquals("{\"language\":\"\",\"displayLanguage\":\"\",\"country\":\"US\",\"displayCountry\":\"United States\",\"tag\":\"-US\"}", json);
    assertEquals("US", locale.getCountry());

    code = "EN_us";
    locale = new Locale(code);
    json = CodecUtils.toJson(locale);
    assertEquals("US", locale.getCountry());
    assertEquals("en", locale.getLanguage());
    assertEquals("en-US", locale.getTag());
    assertEquals("English", locale.getDisplayLanguage());
    assertEquals("United States", locale.getDisplayCountry());
    
    
    code = "en-";
    locale = new Locale(code);
    json = CodecUtils.toJson(locale);
    assertEquals("en", locale.getLanguage());
    assertEquals("en", locale.getTag());
    assertEquals(null, locale.getCountry());

 
    locale = new Locale("en-us");
    assertEquals("US", locale.getCountry());
    assertEquals("en", locale.getLanguage());
    assertEquals("en-US", locale.getTag());
    assertEquals("English", locale.getDisplayLanguage());
    assertEquals("United States", locale.getDisplayCountry());
    assertEquals("en-US", locale.toString());
    
    
  
  
    
    

    // check toString

    // check serialization


    //cmn-Hans-CN
    

  }

}
