package org.myrobotlab.framework;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;

/**
 * This class acts as a container for generic
 * type information that is retained at runtime.
 * At first glance that statement would seem to
 * violate the most important element of Java
 * generics: type erasure. However, Java does
 * in fact reify generic types in extremely
 * specific circumstances. One of those circumstances
 * is when a subclass is created that statically
 * provides concrete type parameters. For example:
 * <p></p>
 * {@code
 * new StaticType<String>(){}
 * }
 * <p></p>
 * The following does not work because it does not provide
 * a static concrete type parameter:
 * <p></p>
 * {@code
 * new StaticType<T>(){}
 * }
 * <p>
 *     The above will throw an {@link IllegalArgumentException}.
 * </p>
 * <p>
 *  This concept comes from a 2006 blog post from Neal Gafter.
 *  <a href="http://gafter.blogspot.com/2006/12/super-type-tokens.html?showComment=1171980720000#c2954512480577635806">
 *  A comment</a> on that blog post suggested implementing a generic interface with
 *  a method signature containing the generic type parameter, such as
 *  {@link Comparable}. This should have made it a syntax error
 *  to use raw types, but I was unable to cause such an error to occur.
 *  Thus, this class instead checks for raw types in the constructor
 *  by checking whether {@link Class#getGenericSuperclass()} returns
 *  an instance of {@link ParameterizedType} or not. If not,
 *  {@link IllegalArgumentException} is thrown.
 * </p>
 *
 * @param <T> The concrete type that this StaticType contains
 * @see <a href="http://gafter.blogspot.com/2006/12/super-type-tokens.html">Super Type Tokens</a>
 * @author AutonomicPerfectionist
 */
public abstract class StaticType<T> {
    private final Type storedType;

    /**
     * Constructs this StaticType object
     * and ensures the generic type parameters
     * were provided and are concrete.
     *
     * @throws IllegalArgumentException if the generic
     *          type parameters were not provided or were not concrete.
     */
    protected StaticType() {
        // This gets the type passed into the T
        // just like in the source code. This means that
        // if you do new StaticType<T>(){}
        // this genericType *will not* have the type of T
        Type genericType = getClass().getGenericSuperclass();
        if (!(genericType instanceof ParameterizedType))
            throw new IllegalArgumentException("StaticType must not be a raw type");
        storedType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        validateType(storedType);

    }

    private StaticType(Type storedType) {
        this.storedType = storedType;
        validateType(storedType);
    }

    /**
     * Gets the stored {@link Type}
     * instance. This type should contain the type of
     * {@link T}, including concrete generic type
     * parameters if T is a generic type itself.
     * @return The stored type
     */
    public Type getType() {
        return storedType;
    }

    /**
     * Get the stored type as a Class object
     * that can be used for checking cast
     * compatibility. Note that the resulting
     * Class object will not check for generic
     * compatibility. If the stored type
     * is not a Class type, then this method
     * throws {@link IllegalStateException}.
     *
     * @return The internal stored type cast to a Class object
     * @throws IllegalStateException if the stored type is not a Class.
     */
    @SuppressWarnings("unchecked")
    public Class<T> asClass() {
        if (storedType instanceof Class) {
            return (Class<T>) storedType;
        } else {
            throw new IllegalStateException("Stored type " + storedType + " is not a Class.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StaticType)) return false;

        StaticType<?> that = (StaticType<?>) o;

        return Objects.equals(storedType, that.storedType);
    }

    @Override
    public int hashCode() {
        return storedType != null ? storedType.hashCode() : 0;
    }


    @Override
    public String toString() {
        return storedType.toString();
    }
    /**
     * Function to recursively validate type parameters
     * to ensure they are all concrete so no type variables sneak in.
     * @param type The type to check
     */
    private static void validateType(Type type) {
        if (type instanceof ParameterizedType) {
            for (Type param : ((ParameterizedType) type).getActualTypeArguments()) {
                validateType(param);
            }
        } else if (type instanceof TypeVariable) {
            throw new IllegalArgumentException("Cannot construct a StaticType with any non-concrete type variables");
        }

    }

    public static <T> StaticType<T> fromJavaType(Type type) {
        return new StaticType<>(type){};
    }
}
