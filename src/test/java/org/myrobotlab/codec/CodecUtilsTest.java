package org.myrobotlab.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bouncycastle.util.Strings;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.test.AbstractTest;

public class CodecUtilsTest extends AbstractTest {

  @Ignore /*
           * need to ignore it for the moment - should not compare exact json -
           * not effective
           */
  @Test
  public void testLocale() {
    Locale mrlLocale;

    // String code construction
    String code = null;
    String json = null;

    java.util.Locale check = new java.util.Locale("zh-cmn-Hans-CN");

    code = "-";
    mrlLocale = new Locale(code);
    json = CodecUtils.toJson(mrlLocale);
    assertEquals("{\"language\":null,\"displayLanguage\":null,\"country\":null,\"displayCountry\":null,\"tag\":null,\"class\":\"org.myrobotlab.service.data.Locale\"}", json);

    mrlLocale = new Locale("en");
    json = CodecUtils.toJson(mrlLocale);
    Locale locale = CodecUtils.fromJson(json, Locale.class);
    assertEquals("en", locale.getLanguage());

    code = " - ";
    mrlLocale = new Locale(code);
    json = CodecUtils.toJson(mrlLocale);
    assertEquals("{\"language\":null,\"displayLanguage\":null,\"country\":null,\"displayCountry\":null,\"tag\":null,\"class\":\"org.myrobotlab.service.data.Locale\"}", json);

    code = "  ";
    mrlLocale = new Locale(code);
    json = CodecUtils.toJson(mrlLocale);
    assertEquals("{\"language\":null,\"displayLanguage\":null,\"country\":null,\"displayCountry\":null,\"tag\":null,\"class\":\"org.myrobotlab.service.data.Locale\"}", json);

    code = "";
    mrlLocale = new Locale(code);
    json = CodecUtils.toJson(mrlLocale);
    assertEquals("{\"language\":null,\"displayLanguage\":null,\"country\":null,\"displayCountry\":null,\"tag\":null,\"class\":\"org.myrobotlab.service.data.Locale\"}", json);

    code = "-uS";
    mrlLocale = new Locale(code);
    json = CodecUtils.toJson(mrlLocale);
    assertEquals(
        "{\"language\":\"\",\"displayLanguage\":\"\",\"country\":\"US\",\"displayCountry\":\"United States\",\"tag\":\"-US\",\"class\":\"org.myrobotlab.service.data.Locale\"}",
        json);

  }

  @Test
  public void testDefaultSerialization() {
    // default json serialization
    String json = CodecUtils.toJson(new Locale("en"));
    Locale locale = CodecUtils.fromJson(json, Locale.class);
    assertEquals("en", locale.getLanguage());

    // default serialization
    @SuppressWarnings({ "unchecked", "rawtypes" })
    Map<String, Object> defaultObj = (Map) CodecUtils.fromJson(json);
    assertEquals("en", defaultObj.get("language"));

    // type supplied
    Locale check = (Locale) CodecUtils.fromJson(json, Locale.class);
    assertEquals("en", check.getLanguage());

    // Object.class supplied
    check = (Locale) CodecUtils.fromJson(json, Locale.class);
    assertEquals("en", check.getLanguage());

    // primitives objects arrays
    Object test = CodecUtils.fromJson("1");
    assertEquals(test.getClass(), Integer.class);

    test = CodecUtils.fromJson("1.0");
    assertEquals(test.getClass(), Double.class);
    
    test = CodecUtils.fromJson("\"Ahoy!\"");
    assertEquals(test.getClass(), String.class);
    
    test = CodecUtils.fromJson("[]");
    assertEquals(test.getClass(), ArrayList.class);
    
    test = CodecUtils.fromJson("{}");
    assertEquals(test.getClass(), LinkedHashMap.class);
    
    
  }

  @Test
  public void testNormalizeServiceName() {
    Platform.getLocalInstance().setId("test-id");
    assertEquals("runtime@test-id", CodecUtils.getFullName("runtime"));
    assertEquals("runtime@test-id", CodecUtils.getFullName("runtime@test-id"));
  }

  @Test
  public void testCheckServiceNameEqual() {
    Platform.getLocalInstance().setId("test-id");
    assertTrue(CodecUtils.checkServiceNameEquality("runtime", "runtime"));
    assertTrue(CodecUtils.checkServiceNameEquality("runtime", "runtime@test-id"));
    assertTrue(CodecUtils.checkServiceNameEquality("runtime@test-id", "runtime"));
    assertTrue(CodecUtils.checkServiceNameEquality("runtime@test-id", "runtime@test-id"));
    assertFalse(CodecUtils.checkServiceNameEquality("runtime", "runtime@not-corr-id"));
    assertFalse(CodecUtils.checkServiceNameEquality("runtime@not-corr-id", "runtime"));

  }
  
  @Test
  public void testBase64() {
    // not a very comprehensive test, but a sanity check none the less.
    String input = "input string.";
    String output = CodecUtils.toBase64(input.getBytes());
    assertEquals(output.length(), 20);
    byte[] covertedBack = CodecUtils.fromBase64(output);
    String result = Strings.fromByteArray(covertedBack);
    assertEquals(input, result);

  }
  

}
