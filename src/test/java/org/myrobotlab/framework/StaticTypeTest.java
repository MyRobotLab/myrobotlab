package org.myrobotlab.framework;

import org.junit.Test;
import org.myrobotlab.test.AbstractTest;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class StaticTypeTest extends AbstractTest {

    @Test
    @SuppressWarnings("rawtypes")
    public void testRawType() {
        assertThrows(IllegalArgumentException.class, () -> {
            StaticType type = new StaticType() {};
        });
    }

    @Test
    public void testSimpleType() {
        StaticType<String> type = new StaticType<>() {};
        assertEquals(String.class, type.getType());

    }

    @Test
    public void testComplexType() {
        // This is how you need to construct a StaticType.
        // You can't construct it like a normal object because
        // the type information is only reified when a concrete
        // subclass with a concrete type parameter is created
        StaticType<List<String>> type = new StaticType<>() {};
        assertEquals("java.util.List<java.lang.String>", type.getType().getTypeName());
    }

    @Test
    public void testT() {
        assertThrows(IllegalArgumentException.class, () -> {
            StaticType<String> str = staticT();
        });

    }

    @Test
    public void testPartialT() {
        assertThrows(IllegalArgumentException.class, () -> {
            StaticType<List<String>> str = partialStaticT();
        });

    }

    /*
    This method shows that you can't pass generic types
    through method signatures and other boundaries.
    You *MUST* specify the types explicitly when the
    instance is constructed. It will throw IllegalArgumentException
    when you try to call this method
     */
    private <T> StaticType<T> staticT() {
        return new StaticType<>() {
        };
    }

    /*
    Even if you supply some concrete type parameters,
    if there is a type variable anywhere construction will throw
    an IllegalArgumentException
     */
    private <T> StaticType<List<T>> partialStaticT() {
        return new StaticType<>() {
        };
    }

}
