package org.myrobotlab.codec.json;

/**
 * Represents a failed Json deserialization operation.
 * This exception is always caused by other exceptions.
 *
 * @author AutonomicPerfectionist
 */
public class JsonDeserializationException extends RuntimeException{
    public JsonDeserializationException(Throwable cause) {
        super(cause);
    }
}
