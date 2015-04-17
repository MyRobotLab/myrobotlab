package com.thalmic.myo.exception;

public class HubNotFoundException extends Exception {
    private static final long serialVersionUID = -7798407988091106844L;

    public HubNotFoundException() {
	super();
    }

    public HubNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
	super(message, cause, enableSuppression, writableStackTrace);
    }

    public HubNotFoundException(String message, Throwable cause) {
	super(message, cause);
    }

    public HubNotFoundException(String message) {
	super(message);
    }

    public HubNotFoundException(Throwable cause) {
	super(cause);
    }
}