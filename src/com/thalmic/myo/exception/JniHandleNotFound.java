package com.thalmic.myo.exception;

public class JniHandleNotFound extends Exception {
    private static final long serialVersionUID = -7498575829001820184L;

    public JniHandleNotFound() {
	super();
    }

    public JniHandleNotFound(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
	super(message, cause, enableSuppression, writableStackTrace);
    }

    public JniHandleNotFound(String message, Throwable cause) {
	super(message, cause);
    }

    public JniHandleNotFound(String message) {
	super(message);
    }

    public JniHandleNotFound(Throwable cause) {
	super(cause);
    }

}
