package org.myrobotlab.codec;

import static org.junit.Assert.*;
import org.junit.Test;
import org.myrobotlab.test.AbstractTest;

public class ForeignProcessUtilsTest extends AbstractTest {

    @Test
    public void testValidJavaClassName() {
        assertTrue("False negative when checking valid class name",
                ForeignProcessUtils.isValidJavaClassName("ExampleClass"));

        assertTrue("False negative when checking valid class name",
                ForeignProcessUtils.isValidJavaClassName("com.example.ExampleClass"));
    }

    @Test
    public void testInvalidJavaClassName() {
        assertFalse("False positive when checking empty class name",
                ForeignProcessUtils.isValidJavaClassName(""));

        assertFalse("False positive when checking valid class name",
                ForeignProcessUtils.isValidJavaClassName("^ExampleClass"));

        assertFalse("False positive when checking valid class name",
                ForeignProcessUtils.isValidJavaClassName("py:com.example.ExampleClass"));
    }

    @Test
    public void testValidTypeKey() {
        assertTrue("False negative when checking valid type key",
                ForeignProcessUtils.isValidTypeKey("ExampleClass"));

        assertTrue("False negative when checking valid type key",
                ForeignProcessUtils.isValidTypeKey("com.example.ExampleClass"));

        assertTrue("False negative when checking valid type key",
                ForeignProcessUtils.isValidTypeKey("py:com.example.ExampleClass"));
    }

    @Test
    public void testInvalidTypeKey() {
        assertFalse("False positive when checking empty type key",
                ForeignProcessUtils.isValidTypeKey(""));

        assertFalse("False positive when checking invalid type key",
                ForeignProcessUtils.isValidTypeKey("^com.example.ExampleClass"));

        assertFalse("False positive when checking invalid type key",
                ForeignProcessUtils.isValidTypeKey(":py:com.example.ExampleClass"));
    }

    @Test
    public void testGetLanguageId() {
        assertThrows(IllegalArgumentException.class, () -> ForeignProcessUtils.getLanguageId(":bad_id"));
        String languageId = "py";
        String type_key = languageId + ":exampleService";
        assertEquals("Incorrect language id", languageId, ForeignProcessUtils.getLanguageId(type_key));

        languageId = "rust";
        type_key = languageId + ":mrl::exampleService";
        assertEquals("Incorrect language id", languageId, ForeignProcessUtils.getLanguageId(type_key));
    }

    @Test
    public void testGetLanguageSpecificTypeKey() {
        assertThrows(IllegalArgumentException.class, () -> ForeignProcessUtils.getLanguageSpecificTypeKey(":bad_id"));
        String langTypeKey = "exampleService";
        String typeKey = "py:" + langTypeKey;
        assertEquals("Incorrect language-specific type key", langTypeKey,
                ForeignProcessUtils.getLanguageSpecificTypeKey(typeKey));

        langTypeKey = "mrl::exampleService";
        typeKey = "rust:" + langTypeKey;
        assertEquals("Incorrect language-specific type key", langTypeKey,
                ForeignProcessUtils.getLanguageSpecificTypeKey(typeKey));
    }

}
