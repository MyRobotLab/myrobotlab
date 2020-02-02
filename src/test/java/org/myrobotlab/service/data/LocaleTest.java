package org.myrobotlab.service.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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


    // check toString

    // check serialization

    // java.util.Locale construction

    java.util.Locale jl = null;
    jl = new java.util.Locale("en-us");
    locale = new Locale(jl);
    assertEquals("US", locale.getCountry());
    assertEquals("en", locale.getLanguage());
    assertEquals("en-US", locale.getTag());
    assertEquals("English", locale.getDisplayLanguage());
    assertEquals("United States", locale.getDisplayCountry());
    
    jl = new java.util.Locale("en", "us");
    locale = new Locale(jl);
    

  }

}
