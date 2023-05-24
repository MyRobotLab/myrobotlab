package org.myrobotlab.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.codec.json.JsonDeserializationException;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.StaticType;
import org.myrobotlab.framework.TimeoutException;
import org.myrobotlab.net.Http;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.test.AbstractTest;
import org.myrobotlab.utils.ObjectTypePair;

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
  public void returnMessageTestSimpleTypes() {
    String retVal = "retVal";
    // Put directly in blocking list because sendBlocking() won't use it for local services
    Runtime.getInstance().getInbox().blockingList.put(
            "runtime.onBlocking",
            new ObjectTypePair<>(null, new StaticType<String>(){})
    );

    Message returnMsg = Message.createMessage("test", "runtime", "onBlocking", new Object[]{CodecUtils.toJson(retVal)});
    returnMsg.msgType = Message.MSG_TYPE_RETURN;
    returnMsg.encoding = "json";
    Message deserMessage = CodecUtils.decodeMessageParams(new Message(returnMsg));

    assertEquals(retVal, deserMessage.data[0]);

    // This should now throw, notice the Integer type instead of String
    Runtime.getInstance().getInbox().blockingList.put(
            "runtime.onBlocking",
            new ObjectTypePair<>(null, new StaticType<Integer>(){})
    );
    returnMsg = Message.createMessage("test", "runtime", "onBlocking", new Object[]{CodecUtils.toJson(retVal)});
    returnMsg.msgType = Message.MSG_TYPE_RETURN;
    returnMsg.encoding = "json";
    Message finalReturnMsg = returnMsg;
    assertThrows(JsonDeserializationException.class, () -> CodecUtils.decodeMessageParams(finalReturnMsg));

    Runtime.getInstance().getInbox().blockingList.put(
            "runtime.onBlocking",
            new ObjectTypePair<>(null, new StaticType<Integer>(){})
    );
    returnMsg = Message.createMessage("test", "runtime", "onBlocking", new Object[]{CodecUtils.toJson(retVal)});
    // Setting msgType to null makes CodecUtils ignore blockingList
    returnMsg.msgType = null;
    returnMsg.encoding = "json";
    deserMessage = CodecUtils.decodeMessageParams(new Message(returnMsg));

    assertEquals(retVal, deserMessage.data[0]);


  }

}
