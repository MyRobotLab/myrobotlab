package org.myrobotlab.codec.json;

/**
 * Represents a failed JSON serialization operation.
 * This exception is always caused by other exceptions.
 *
 * @author AutonomicPerfectionist
 */
public class JsonSerializationException extends RuntimeException{
    public JsonSerializationException(Throwable cause) {
        super(cause);
    }
}
