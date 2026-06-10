package com.arogya.cafe.supplier;

public class BillScannerException extends RuntimeException {
    public BillScannerException(String message) {
        super(message);
    }

    public BillScannerException(String message, Throwable cause) {
        super(message, cause);
    }
}
