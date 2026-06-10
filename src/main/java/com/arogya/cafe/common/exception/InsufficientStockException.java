package com.arogya.cafe.common.exception;

/** Stock would go negative on a deduction. Maps to HTTP 409. */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
