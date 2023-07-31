package org.myrobotlab.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.util.Strings;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.data.Orientation;
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
  public void testExtractParams() {
    // /runtime/connect/"http://blah:8888"
    String input = null;
    Object[] params = null;
    double delta = 0.0001;
    
    input = "\"http://blah:8888\"";
    params = CodecUtils.extractJsonMsgFromUriPath(input);
    assertEquals("http://blah:8888", params[0]);
    
    input = "\"http://blah:8888/this/path/\"";
    params = CodecUtils.extractJsonMsgFromUriPath(input);
    assertEquals("http://blah:8888/this/path/", params[0]);
    
    input = "\"http://blah:8888/this/path/\"/5";
    params = CodecUtils.extractJsonMsgFromUriPath(input);
    assertEquals("http://blah:8888/this/path/", params[0]);
    assertEquals(5, params[1]);
    
    input = "[3,5,7,8]/[1.0, 2.0, 3.0]/true";
    params = CodecUtils.extractJsonMsgFromUriPath(input);
    assertEquals(8,((List)params[0]).get(3));
    assertEquals(3.0,(double)((List)params[1]).get(2), delta);
    assertEquals(true, params[2]);
    
    input = "[\"apple\",\"banana\",\"orange\"]/[\"a\",\"b\",\"c\"]/true";
    params = CodecUtils.extractJsonMsgFromUriPath(input);
    assertEquals("apple",((List)params[0]).get(0));
    assertEquals("a",((List)params[1]).get(0));
    assertEquals(true, params[2]);
    
    String[] files =new String[] {"f:\\testdir\\blah","/root/","/home/mydir/blah"};
    String[] abc = new String[] {"a", "b", "c"};
    
    input = CodecUtils.toJson(files) + "/" + CodecUtils.toJson(abc) + "/" + CodecUtils.toJson(true);
    
    // input = "\"[\"f:\\testdir\\blah\",\"/root/\",\"/home/mydir/blah\"]\"/\"[\"a\",\"b\",\"c\"]\"/true";
    params = CodecUtils.extractJsonMsgFromUriPath(input);
    assertEquals(3, params.length);
    List testFiles = (List)params[0];
    assertEquals("/root/", testFiles.get(1));
    // String[] files = CodecUtils.fromJson(params[0], String[].class);
    // assertEquals("\"[\"apple\",\"banana\",\"orange\"]\"", CodecUtils.fromJson(params[0], String[].class));
    Orientation o = new Orientation();
    o.pitch = 2.2342;
    o.yaw = 1.234;
    o.roll = 0.343;
        
    input = CodecUtils.toJson(files) + "/" + CodecUtils.toJson(o) + "/" + CodecUtils.toJson(true);
    params = CodecUtils.extractJsonMsgFromUriPath(input);
    assertEquals(3, params.length);
    Map t = (Map)params[1];
    
    assertEquals(2.2342, (double)t.get("pitch"), delta);
    

    // bonus
    input = "This is /a block of/ text between/double quotes/ and /a single character";
    params = CodecUtils.extractJsonMsgFromUriPath(input);
    assertEquals(6, params.length);

    
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
