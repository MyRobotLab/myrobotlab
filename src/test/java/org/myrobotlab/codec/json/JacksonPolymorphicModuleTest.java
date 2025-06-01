package org.myrobotlab.codec.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.noctordeser.NoCtorDeserModule;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

import java.util.Arrays;

public class JacksonPolymorphicModuleTest extends AbstractTest {
    public final static Logger log = LoggerFactory.getLogger(JacksonPolymorphicModuleTest.class);

    private static ObjectMapper polymorphicMapper;

    private static ObjectMapper regularMapper;

    @BeforeClass
    public static void setup() {
        polymorphicMapper = new ObjectMapper();
        polymorphicMapper.registerModule(new NoCtorDeserModule());
        polymorphicMapper.registerModule(JacksonPolymorphicModule.getPolymorphicModule());

        //Disables Jackson's automatic property detection
        polymorphicMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        polymorphicMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        polymorphicMapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.ANY);

        //Make jackson behave like gson in that unknown properties are ignored
        polymorphicMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        regularMapper = new ObjectMapper();
        regularMapper.registerModule(new NoCtorDeserModule());

        //Disables Jackson's automatic property detection
        regularMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        regularMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        regularMapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.ANY);

        //Make jackson behave like gson in that unknown properties are ignored
        regularMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    public void testStringSer() throws JsonProcessingException {
        String testString = "this is a test with spaces and $pecial characters!";
        String jsonString = polymorphicMapper.writeValueAsString(testString);
        log.debug("Encoded test string: " + jsonString);
        String decodedString = regularMapper.readValue(jsonString, String.class);
        log.debug("Decoded test string: " + decodedString);
        Assert.assertEquals("String encoding not correct", testString, decodedString);
    }

    @Test
    public void testStringArraySer() throws JsonProcessingException {
        String[] testStrings = new String[] {"This", "is", "a", "test", "array"};
        String jsonString = polymorphicMapper.writeValueAsString(testStrings);
        log.debug("Encoded test string: " + jsonString);
        String[] decodedStrings = regularMapper.readValue(jsonString, String[].class);
        log.debug("Decoded test strings: " + Arrays.toString(decodedStrings));
        Assert.assertArrayEquals("String array encoding incorrect", testStrings, decodedStrings);
    }

    @Test
    public void testNumberSer() throws JsonProcessingException {
        int testInt = 42;
        String jsonString = polymorphicMapper.writeValueAsString(testInt);
        log.debug("Encoded test string: " + jsonString);
        int decodedInt = regularMapper.readValue(jsonString, Integer.class);
        log.debug("Decoded test int: " + decodedInt);
        Assert.assertEquals("Int encoding not correct", testInt, decodedInt);
    }

    @Test
    public void testBoolSer() throws JsonProcessingException {
        boolean testBoolean = false;
        String jsonString = polymorphicMapper.writeValueAsString(testBoolean);
        log.debug("Encoded test string: " + jsonString);
        boolean decodedBoolean = regularMapper.readValue(jsonString, Boolean.class);
        log.debug("Decoded test string: " + decodedBoolean);
        Assert.assertEquals("Boolean encoding not correct", testBoolean, decodedBoolean);
    }


    @Test
    public void testStringDeser() throws JsonProcessingException {
        String testString = "this is a test with spaces and $pecial characters!";
        String jsonString = regularMapper.writeValueAsString(testString);
        log.debug("Encoded test string: " + jsonString);
        String decodedString = polymorphicMapper.readValue(jsonString, String.class);
        log.debug("Decoded test string: " + decodedString);
        Assert.assertEquals("String decoding not correct", testString, decodedString);
    }

    @Test
    public void testStringArrayDeser() throws JsonProcessingException {
        String[] testStrings = new String[] {"This", "is", "a", "test", "array"};
        String jsonString = regularMapper.writeValueAsString(testStrings);
        log.debug("Encoded test string: " + jsonString);
        String[] decodedStrings = polymorphicMapper.readValue(jsonString, String[].class);
        log.debug("Decoded test strings: " + Arrays.toString(decodedStrings));
        Assert.assertArrayEquals("String array decoding incorrect", testStrings, decodedStrings);
    }

    @Test
    public void testNumberDeser() throws JsonProcessingException {
        int testInt = 42;
        String jsonString = regularMapper.writeValueAsString(testInt);
        log.debug("Encoded test string: " + jsonString);
        int decodedInt = polymorphicMapper.readValue(jsonString, Integer.class);
        log.debug("Decoded test int: " + decodedInt);
        Assert.assertEquals("Int decoding not correct", testInt, decodedInt);
    }

    @Test
    public void testBoolDeser() throws JsonProcessingException {
        boolean testBoolean = false;
        String jsonString = regularMapper.writeValueAsString(testBoolean);
        log.debug("Encoded test string: " + jsonString);
        boolean decodedBoolean = polymorphicMapper.readValue(jsonString, Boolean.class);
        log.debug("Decoded test string: " + decodedBoolean);
        Assert.assertEquals("Boolean decoding not correct", testBoolean, decodedBoolean);
    }

    @Test
    public void testApiDescriptionDeser() throws JsonProcessingException {
        CodecUtils.ApiDescription description = new CodecUtils.ApiDescription("key",
                "/path/", "{exampleURI}", "This is a description");
        String jsonString = regularMapper.writeValueAsString(description);
        log.debug("Encoded test string: " + jsonString);
        CodecUtils.ApiDescription decodedDescription = polymorphicMapper.readValue(jsonString, CodecUtils.ApiDescription.class);
        log.debug("Decoded test string: " + decodedDescription);
        Assert.assertEquals("ApiDescription decoding not correct", description, decodedDescription);
    }

    @Test
    public void testMessagePolymorphic() throws JsonProcessingException {
        Message message = Message.createMessage("runtime", "runtime", "register",
                new Registration("runtime", "obsidian", "org.myrobotlab.service.Runtime"));
        String jsonString = polymorphicMapper.writeValueAsString(message);
        log.debug("Encoded test string: " + jsonString);
        Message decodedMessage = (Message) polymorphicMapper.readValue(jsonString, Object.class);
        log.debug("Decoded test string: " + decodedMessage);
        Assert.assertEquals("Message polymorphism not correct", message, decodedMessage);
    }





}
