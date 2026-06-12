package com.arogya.cafe;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.arogya.cafe.catalog.dto.CatalogDtos.IngredientRequest;
import com.arogya.cafe.catalog.entity.Ingredient;
import com.arogya.cafe.catalog.service.CatalogService;
import com.arogya.cafe.inventory.dto.InventoryDtos.CreateStockRequest;
import com.arogya.cafe.inventory.entity.InventoryStock;
import com.arogya.cafe.inventory.repository.InventoryStockRepository;
import com.arogya.cafe.inventory.service.StockService;
import com.arogya.cafe.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * Inventory-correctness guard under concurrency. Two transactions read the same stock row, both
 * decrement it, and both try to commit. With the {@code @Version} optimistic lock the second
 * commit must fail rather than silently overwrite the first — which would lose a deduction and let
 * stock drift above what was really consumed. The test is deterministic (no thread timing): it
 * loads two detached copies, commits one, then commits the now-stale other.
 */
@SpringBootTest
class StockOptimisticLockTest extends AbstractIntegrationTest {

    @Autowired
    private CatalogService catalog;

    @Autowired
    private StockService stock;

    @Autowired
    private InventoryStockRepository stockRepo;

    @Test
    void staleConcurrentStockUpdateIsRejected() {
        Ingredient sugar = catalog.createIngredient(new IngredientRequest("Sugar", "g"));
        stock.createStock(new CreateStockRequest(sugar.getId(), BigDecimal.valueOf(1000), BigDecimal.valueOf(100)));

        // Two independent reads (each in its own transaction) → two detached copies at version 0.
        InventoryStock first = stockRepo.findByIngredientId(sugar.getId());
        InventoryStock stale = stockRepo.findByIngredientId(sugar.getId());

        // First writer commits: row goes to version 1.
        first.setQtyOnHand(first.getQtyOnHand().subtract(BigDecimal.valueOf(200)));
        stockRepo.saveAndFlush(first);

        // Second writer holds version 0 → optimistic lock check fails instead of clobbering the first.
        stale.setQtyOnHand(stale.getQtyOnHand().subtract(BigDecimal.valueOf(300)));
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> stockRepo.saveAndFlush(stale));
    }
}
