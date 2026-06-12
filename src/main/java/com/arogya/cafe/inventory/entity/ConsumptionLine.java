package com.arogya.cafe.inventory.entity;

/** One serving to consume: which menu item, which size, how many. */
public record ConsumptionLine(Long menuItemId, String sizeVariant, int quantity) {}
