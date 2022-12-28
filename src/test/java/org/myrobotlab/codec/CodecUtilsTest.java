package org.myrobotlab.codec;

import org.junit.Test;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.test.AbstractTest;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CodecUtilsTest extends AbstractTest {

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
        assertEquals("{\"language\":\"\",\"displayLanguage\":\"\",\"country\":\"US\",\"displayCountry\":\"United States\",\"tag\":\"-US\",\"class\":\"org.myrobotlab.service.data.Locale\"}", json);

    }

    @Test
    public void testJsonMessageSerDeser() {
        Message message = Message.createMessage(
                "testing-sender",
                "testService",
                "testMethod",
                        new Object[]{
                                "testParam1",
                                2,
                                new MRLListener(
                                        "topic",
                                        "callbackService",
                                        "callbackMethod"
                                )
                        }
                );
        String json = CodecUtils.toJsonMsg(message);
        Message deserMessage = CodecUtils.jsonToMessage(json);
        assertEquals(message, deserMessage);
    }

    @Test
    public void testDecodeMessageParams() {
        // Tests decoding message params without access
        // to the method cache. There are caveats in doing this!
        // First, GSON has edge cases for arrays of objects, since
        // we don't know what type is contained within the array we are forced
        // to deserialize it to an array of Objects, but GSON skips our custom
        // deserializer for the Object type. So an array of objects that are not
        // primitives nor Strings *will* deserialize incorrectly unless
        // we are using Jackson.
        // Second, without the type information from the method cache we have
        // no way of knowing whether to interpret an array as an array of Objects
        // or as a List (or even what implementor of List to use)

        Message encodedMessage = Message.createMessage(
                "testing-sender",
                "testService",
                "testMethod",
                new Object[]{
                        "\"testParam1\"",
                        "2",
                        "false",
                        "1.5",
                        "null",
                        CodecUtils.toJson(List.of("string1", "string2")),
                        List.of(List.of("string3", "string4")),
                        CodecUtils.toJson(new MRLListener(
                                "topic",
                                "callbackService",
                                "callbackMethod"
                        ))
                }
        );

        Message decodedMessage = Message.createMessage(
                "testing-sender",
                "testService",
                "testMethod",
                new Object[]{
                        "testParam1",
                        2,
                        false,
                        1.5,
                        null,
                        List.of("string1", "string2"),
                        List.of(List.of("string3", "string4")),
                        new MRLListener(
                                "topic",
                                "callbackService",
                                "callbackMethod"
                        )
                }
        );

        decodedMessage.msgId = encodedMessage.msgId;

        assertEquals(decodedMessage, CodecUtils.decodeMessageParams(encodedMessage));
    }

    @Test
    public void testJsonMessageDeserNull() {
        Message deserMessage = CodecUtils.jsonToMessage("null");
        assertNull(deserMessage);

    }
}
