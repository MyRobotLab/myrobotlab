package org.myrobotlab.codec;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class contains many utility methods related to foreign
 * processes that are written in other programming languages.
 * <p>
 * This class defines a format for describing "classes"
 * or types that originate from unknown programming languages.
 * <p>
 * The format consists of two simple parts: the language ID, that
 * describes what language the process is using, and the
 * language-specific type key. It is expected that each
 * language-specific type key maps to a set of runtime-static procedures,
 * so in the case of languages where a type's applicable procedures
 * can change during runtime, as in Python, it is recommended to
 * generate a new type key for every change in the procedure list.
 * This is because the set of known procedures is cached and is not regenerated
 * to improve performance.
 * <p>
 * Currently, this information is only used to determine when
 * to generate a {@link java.lang.reflect.Proxy}, but it would
 * enable foreign processes in the future to instantiate the
 * correct proxy when dynamic proxies aren't possible such as
 * in fully compiled languages.
 *
 * @author AutonomicPerfectionist
 */
public class ForeignProcessUtils {


    /**
     * The string used to separate the two parts of the
     * foreign process type specifier. The language ID
     * may not contain this string, but the language-specific
     * type key may. It is used in a regular expression
     * so escaping might be required.
     */
    public static final String LANGUAGE_ID_SEPARATOR = ":";

    /**
     * A pattern that both tests whether a string is a valid foreign
     * type key and splits the key on the language id separator.
     * <p>
     * For example, if the separator is a single colon ({@code ':'}),
     * then "py:exampleService" would match and the two capture groups would be
     * "py" and "exampleService."
     * <p>
     * This pattern does not allow the separator in the language ID at all,
     * but does allow it in the language-specific type key (the second capture group).
     * <p>
     * This enables languages that use double-colons for package or module definition
     * to work seamlessly.
     */
    public static final Pattern FOREIGN_TYPE_KEY_PATTERN = Pattern.compile(
            String.format("^([^%s]+)%s(.+)$", LANGUAGE_ID_SEPARATOR, LANGUAGE_ID_SEPARATOR));


    /**
     * Java identifier pattern, using builtin Java regex "macros."
     * This is a string regex pattern that identifies a valid
     * Java identifier.
     */
    private static final String JAVA_ID_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";

    /**
     * Java fully-qualified class name pattern. This pattern is used
     * to determine if a type key is a valid Java class. If it is not
     * and the type key also does not match the {@link #FOREIGN_TYPE_KEY_PATTERN},
     * then the type key is malformed and should be rejected.
     */
    private static final Pattern JAVA_FQCN_PATTERN = Pattern.compile(JAVA_ID_PATTERN + "(\\." + JAVA_ID_PATTERN + ")*");


    /**
     * Checks whether the given string is a valid
     * fully-qualified class name.
     *
     * @param name The string to be checked
     * @return Whether name is a valid FQCN
     */
    public static boolean isValidJavaClassName(String name) {
        return JAVA_FQCN_PATTERN.matcher(name).matches();
    }


    /**
     * Checks whether a string is a valid Java class name
     * or a valid foreign type key.
     * @param typeKey The string to be checked for validity
     * @return Whether the string is a valid type keu
     */
    public static boolean isValidTypeKey(String typeKey) {
        return typeKey != null && (
                FOREIGN_TYPE_KEY_PATTERN.matcher(typeKey).matches()
                || isValidJavaClassName(typeKey)
        );
    }

    /**
     * Checks whether a type key is a Java type key or a foreign
     * key.
     * @param type The type key to check
     * @return Whether the string is a foreign key. If false, then it is a Java type key
     * @throws IllegalArgumentException if the string is an invalid type key
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isForeignTypeKey(String type) {
        if (!isValidTypeKey(type))
            throw new IllegalArgumentException("Invalid type key: " + type);
        return FOREIGN_TYPE_KEY_PATTERN.matcher(type).matches();
    }

    /**
     * Gets the language id of a foreign type key. The language ID
     * is the first part of the foreign type key, before the {@link #LANGUAGE_ID_SEPARATOR},
     *
     * @param typeKey The foreign type key to split
     * @return The language ID of the foreign key
     * @throws IllegalArgumentException if the string is not a foreign type key
     */
    public static String getLanguageId(String typeKey) {
        if(!isForeignTypeKey(typeKey))
            throw new IllegalArgumentException("Type key " + typeKey + " is not a foreign key");
        Matcher matcher =  FOREIGN_TYPE_KEY_PATTERN.matcher(typeKey);
        if (matcher.matches())
            return matcher.group(1);
        throw new IllegalStateException("Invalid type key: " + typeKey);
    }


    /**
     * Gets the language-specific type key from a foreign type key.
     * The language-specific type key is the second part of the foreign type key.
     *
     * @param typeKey The foreign type key to split
     * @return The language-specific type key contained in the foreign type key
     * @throws IllegalArgumentException if the string is not a foreign type key
     */
    public static String getLanguageSpecificTypeKey(String typeKey) {
        if(!isForeignTypeKey(typeKey))
            throw new IllegalArgumentException("Type key " + typeKey + " is not a foreign key");
        Matcher matcher =  FOREIGN_TYPE_KEY_PATTERN.matcher(typeKey);
        if (matcher.matches())
            return matcher.group(2);
        throw new IllegalStateException("Invalid type key: " + typeKey);
    }

    public static void main(String[] args) {
        String foreignTypeKey = "py:exampleService";
        System.out.println("isValid: " + isValidTypeKey(foreignTypeKey));
        System.out.println("isValidJava: " + isValidJavaClassName(foreignTypeKey));
        System.out.println("isForeign: " + isForeignTypeKey(foreignTypeKey));
        System.out.println("languageId: " + getLanguageId(foreignTypeKey));
        System.out.println("languageSpecificTypeKey: " + getLanguageSpecificTypeKey(foreignTypeKey));

        String invalidKey = "^abcde";

        System.out.println("isValid (no): " + isValidTypeKey(invalidKey));
    }

}
