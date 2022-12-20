package org.myrobotlab.codec;

import org.junit.Test;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.test.AbstractTest;

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
        if(!CodecUtils.USING_GSON) {
            assertEquals("{\"language\":null,\"displayLanguage\":null,\"country\":null,\"displayCountry\":null,\"tag\":null,\"class\":\"org.myrobotlab.service.data.Locale\"}", json);
        } else {
            assertEquals("{\"class\":\"org.myrobotlab.service.data.Locale\"}", json);
        }

        code = " - ";
        mrlLocale = new Locale(code);
        json = CodecUtils.toJson(mrlLocale);
        if(!CodecUtils.USING_GSON) {
            assertEquals("{\"language\":null,\"displayLanguage\":null,\"country\":null,\"displayCountry\":null,\"tag\":null,\"class\":\"org.myrobotlab.service.data.Locale\"}", json);
        } else {
            assertEquals("{\"class\":\"org.myrobotlab.service.data.Locale\"}", json);
        }

        code = "  ";
        mrlLocale = new Locale(code);
        json = CodecUtils.toJson(mrlLocale);
        if (!CodecUtils.USING_GSON) {
            assertEquals("{\"language\":null,\"displayLanguage\":null,\"country\":null,\"displayCountry\":null,\"tag\":null,\"class\":\"org.myrobotlab.service.data.Locale\"}", json);
        } else {
            assertEquals("{\"class\":\"org.myrobotlab.service.data.Locale\"}", json);
        }

        code = "";
        mrlLocale = new Locale(code);
        json = CodecUtils.toJson(mrlLocale);
        if (!CodecUtils.USING_GSON) {
            assertEquals("{\"language\":null,\"displayLanguage\":null,\"country\":null,\"displayCountry\":null,\"tag\":null,\"class\":\"org.myrobotlab.service.data.Locale\"}", json);
        } else {
            assertEquals("{\"class\":\"org.myrobotlab.service.data.Locale\"}", json);
        }

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
        Message encodedMessage = Message.createMessage(
                "testing-sender",
                "testService",
                "testMethod",
                new Object[]{
                        "\"testParam1\"",
                        "2",
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
                        new MRLListener(
                                "topic",
                                "callbackService",
                                "callbackMethod"
                        )
                }
        );

        assertEquals(decodedMessage, CodecUtils.decodeMessageParams(encodedMessage));
    }

    @Test
    public void testJsonMessageDeserNull() {
        Message deserMessage = CodecUtils.jsonToMessage("null");
        assertNull(deserMessage);

    }
}
