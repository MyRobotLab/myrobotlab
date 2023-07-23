package org.myrobotlab.process;

public class SubprocessException extends RuntimeException {
    public SubprocessException(String message) {
        super(message);
    }

    public SubprocessException(Throwable throwable) {
        super(throwable);
    }

    public SubprocessException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
