package org.myrobotlab.codec.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.Arrays;

public class GsonPolymorphicTest extends AbstractTest {
    public final static Logger log = LoggerFactory.getLogger(GsonPolymorphicTest.class);

    private static Gson polymorphicGson;

    private static Gson regularGson;

    @BeforeClass
    public static void setup() {
        polymorphicGson = new GsonBuilder().registerTypeAdapterFactory(new GsonPolymorphicTypeAdapterFactory())
                .setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").disableHtmlEscaping().create();
        

        regularGson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").disableHtmlEscaping().create();
    }

    @Test
    public void testStringSer() {
        String testString = "this is a test with spaces and $pecial characters!";
        String jsonString = polymorphicGson.toJson(testString);
        log.debug("Encoded test string: " + jsonString);
        String decodedString = regularGson.fromJson(jsonString, String.class);
        log.debug("Decoded test string: " + decodedString);
        Assert.assertEquals("String encoding not correct", testString, decodedString);
    }

    @Test
    public void testStringArraySer() {
        String[] testStrings = new String[] {"This", "is", "a", "test", "array"};
        String jsonString = polymorphicGson.toJson(testStrings);
        log.debug("Encoded test string: " + jsonString);
        String[] decodedStrings = regularGson.fromJson(jsonString, String[].class);
        log.debug("Decoded test strings: " + Arrays.toString(decodedStrings));
        Assert.assertArrayEquals("String array encoding incorrect", testStrings, decodedStrings);
    }

    @Test
    public void testNumberSer() {
        int testInt = 42;
        String jsonString = polymorphicGson.toJson(testInt);
        log.debug("Encoded test string: " + jsonString);
        int decodedInt = regularGson.fromJson(jsonString, Integer.class);
        log.debug("Decoded test int: " + decodedInt);
        Assert.assertEquals("Int encoding not correct", testInt, decodedInt);
    }

    @Test
    public void testBoolSer() {
        boolean testBoolean = false;
        String jsonString = polymorphicGson.toJson(testBoolean);
        log.debug("Encoded test string: " + jsonString);
        boolean decodedBoolean = regularGson.fromJson(jsonString, Boolean.class);
        log.debug("Decoded test string: " + decodedBoolean);
        Assert.assertEquals("Boolean encoding not correct", testBoolean, decodedBoolean);
    }


    @Test
    public void testStringDeser() {
        String testString = "this is a test with spaces and $pecial characters!";
        String jsonString = regularGson.toJson(testString);
        log.debug("Encoded test string: " + jsonString);
        String decodedString = polymorphicGson.fromJson(jsonString, String.class);
        log.debug("Decoded test string: " + decodedString);
        Assert.assertEquals("String decoding not correct", testString, decodedString);
    }

    @Test
    public void testStringArrayDeser() {
        String[] testStrings = new String[] {"This", "is", "a", "test", "array"};
        String jsonString = regularGson.toJson(testStrings);
        log.debug("Encoded test string: " + jsonString);
        String[] decodedStrings = polymorphicGson.fromJson(jsonString, String[].class);
        log.debug("Decoded test strings: " + Arrays.toString(decodedStrings));
        Assert.assertArrayEquals("String array decoding incorrect", testStrings, decodedStrings);
    }

    @Test
    public void testNumberDeser() {
        int testInt = 42;
        String jsonString = regularGson.toJson(testInt);
        log.debug("Encoded test string: " + jsonString);
        int decodedInt = polymorphicGson.fromJson(jsonString, Integer.class);
        log.debug("Decoded test int: " + decodedInt);
        Assert.assertEquals("Int decoding not correct", testInt, decodedInt);
    }

    @Test
    public void testBoolDeser() {
        boolean testBoolean = false;
        String jsonString = regularGson.toJson(testBoolean);
        log.debug("Encoded test string: " + jsonString);
        
        boolean decodedBoolean = polymorphicGson.fromJson(jsonString, Boolean.class);
        log.debug("Decoded test string: " + decodedBoolean);
        Assert.assertEquals("Boolean decoding not correct", testBoolean, decodedBoolean);
    }

    @Test
    public void testApiDescriptionDeser() {
        CodecUtils.ApiDescription description = new CodecUtils.ApiDescription("key",
                "/path/", "{exampleURI}", "This is a description");
        String jsonString = regularGson.toJson(description);
        log.debug("Encoded test string: " + jsonString);
        CodecUtils.ApiDescription decodedDescription = polymorphicGson.fromJson(jsonString, CodecUtils.ApiDescription.class);
        log.debug("Decoded test string: " + decodedDescription);
        Assert.assertEquals("ApiDescription decoding not correct", description, decodedDescription);
    }

}
