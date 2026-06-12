package com.arogya.cafe.common.exception;

import java.time.Instant;
import java.util.Map;

/** Consistent error envelope returned for every handled exception. */
public record ApiError(
        Instant timestamp, int status, String error, String message, String path, Map<String, String> fieldErrors) {}
