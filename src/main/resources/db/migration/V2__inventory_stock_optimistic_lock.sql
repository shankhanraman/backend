-- Adds the optimistic-lock column for InventoryStock (@Version).
-- Additive + backward-compatible: NOT NULL with DEFAULT 0 so existing rows in a live database
-- get a valid version without a separate backfill, and Hibernate `validate` matches the entity.
ALTER TABLE inventory_stock
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
