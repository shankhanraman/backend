package com.arogya.cafe.common.exception;

/** A domain/workflow invariant was violated (e.g. serving before KOT prepared). Maps to HTTP 409. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
