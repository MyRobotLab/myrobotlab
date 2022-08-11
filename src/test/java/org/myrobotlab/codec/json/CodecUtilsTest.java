package org.myrobotlab.codec.json;

import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.test.AbstractTest;

import static org.junit.Assert.assertEquals;

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
        assertEquals("{\"class\":\"org.myrobotlab.service.data.Locale\"}", json);

        code = " - ";
        mrlLocale = new Locale(code);
        json = CodecUtils.toJson(mrlLocale);
        assertEquals("{\"class\":\"org.myrobotlab.service.data.Locale\"}", json);

        code = "  ";
        mrlLocale = new Locale(code);
        json = CodecUtils.toJson(mrlLocale);
        assertEquals("{\"class\":\"org.myrobotlab.service.data.Locale\"}", json);

        code = "";
        mrlLocale = new Locale(code);
        json = CodecUtils.toJson(mrlLocale);
        assertEquals("{\"class\":\"org.myrobotlab.service.data.Locale\"}", json);

        code = "-uS";
        mrlLocale = new Locale(code);
        json = CodecUtils.toJson(mrlLocale);
        assertEquals("{\"language\":\"\",\"displayLanguage\":\"\",\"country\":\"US\",\"displayCountry\":\"United States\",\"tag\":\"-US\",\"class\":\"org.myrobotlab.service.data.Locale\"}", json);

    }
}
