package com.arogya.cafe.common.enums;

/** Lifecycle of an order: created at the counter, served after prep, completed once paid. */
public enum OrderStatus {
    CREATED,
    SERVED,
    COMPLETED
}
