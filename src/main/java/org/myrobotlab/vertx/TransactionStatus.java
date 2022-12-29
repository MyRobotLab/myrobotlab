package org.myrobotlab.vertx;

public enum TransactionStatus {
    PENDING("PENDING"),
    INITIATED("INITIATED"),
    RUNNING("RUNNING"),
    FINALIZING("FINALIZING"),
    COMPLETE("COMPLETE");

    private String value;
    TransactionStatus(final String statusStr) {
        this.value = statusStr;
    }

    public String value() { return value; }
  }
