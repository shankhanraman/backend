package com.arogya.cafe.inventory.service;

import com.arogya.cafe.catalog.entity.ItemIngredient;
import com.arogya.cafe.catalog.repository.IngredientRepository;
import com.arogya.cafe.catalog.repository.ItemIngredientRepository;
import com.arogya.cafe.common.enums.StockTransactionType;
import com.arogya.cafe.common.exception.BusinessRuleException;
import com.arogya.cafe.common.exception.InsufficientStockException;
import com.arogya.cafe.common.exception.NotFoundException;
import com.arogya.cafe.inventory.dto.InventoryDtos.CreateStockRequest;
import com.arogya.cafe.inventory.dto.InventoryDtos.RestockRequest;
import com.arogya.cafe.inventory.dto.InventoryDtos.SupplierRequest;
import com.arogya.cafe.inventory.entity.*;
import com.arogya.cafe.inventory.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final InventoryStockRepository stocks;
    private final StockTransactionRepository transactions;
    private final SupplierRepository suppliers;
    private final IngredientRepository ingredients;
    private final ItemIngredientRepository itemIngredients;

    public StockService(
            InventoryStockRepository stocks,
            StockTransactionRepository transactions,
            SupplierRepository suppliers,
            IngredientRepository ingredients,
            ItemIngredientRepository itemIngredients) {
        this.stocks = stocks;
        this.transactions = transactions;
        this.suppliers = suppliers;
        this.ingredients = ingredients;
        this.itemIngredients = itemIngredients;
    }

    // ---- Supplier CRUD ----
    public Supplier createSupplier(SupplierRequest req) {
        return suppliers.save(new Supplier(req.name(), req.contact()));
    }

    @Transactional(readOnly = true)
    public List<Supplier> listSuppliers() {
        return suppliers.findAll();
    }

    @Transactional(readOnly = true)
    public Supplier getSupplier(Long id) {
        return suppliers.findById(id).orElseThrow(() -> new NotFoundException("Supplier " + id + " not found"));
    }

    // ---- Stock CRUD / queries ----
    public InventoryStock createStock(CreateStockRequest req) {
        var ingredient = ingredients
                .findById(req.ingredientId())
                .orElseThrow(() -> new NotFoundException("Ingredient " + req.ingredientId() + " not found"));
        if (stocks.findByIngredientId(req.ingredientId()) != null) {
            throw new BusinessRuleException("Ingredient " + req.ingredientId() + " already has a stock record");
        }
        return stocks.save(new InventoryStock(ingredient, req.qtyOnHand(), req.reorderThreshold()));
    }

    @Transactional(readOnly = true)
    public List<InventoryStock> listStock() {
        return stocks.findAll();
    }

    @Transactional(readOnly = true)
    public InventoryStock getStockByIngredient(Long ingredientId) {
        InventoryStock stock = stocks.findByIngredientId(ingredientId);
        if (stock == null) {
            throw new NotFoundException("No stock record for ingredient " + ingredientId);
        }
        return stock;
    }

    @Transactional(readOnly = true)
    public List<InventoryStock> lowStock() {
        return stocks.findLowStock();
    }

    @Transactional(readOnly = true)
    public List<StockTransaction> transactionsForIngredient(Long ingredientId) {
        InventoryStock stock = getStockByIngredient(ingredientId);
        return transactions.findByInventoryStockIdOrderByCreatedAtDesc(stock.getId());
    }

    /**
     * Restock from a supplier delivery: adds quantity to the ingredient's stock and
     * logs a RESTOCKED transaction.
     */
    public InventoryStock restock(Long ingredientId, RestockRequest req, String triggeredBy) {
        InventoryStock stock = getStockByIngredient(ingredientId);
        Supplier supplier = req.supplierId() != null ? getSupplier(req.supplierId()) : null;
        stock.setQtyOnHand(stock.getQtyOnHand().add(req.quantity()));
        stock.setLastUpdated(Instant.now());
        transactions.save(new StockTransaction(
                stock, StockTransactionType.RESTOCKED, req.quantity(), triggeredBy, supplier, null));
        log.info("Restocked ingredient {} by {} -> {}", ingredientId, req.quantity(), stock.getQtyOnHand());
        return stock;
    }

    /**
     * Auto-deduct every ingredient consumed by the given order lines, logging a CONSUMED
     * transaction per ingredient and raising low-stock alerts. Called when a KOT is prepared.
     */
    public List<InventoryStock> consumeForOrder(Long orderId, List<ConsumptionLine> lines, String triggeredBy) {
        Map<Long, InventoryStock> touched = new LinkedHashMap<>();
        for (ConsumptionLine line : lines) {
            List<ItemIngredient> recipe = itemIngredients.findByMenuItemId(line.menuItemId()).stream()
                    .filter(ii ->
                            ii.getSizeVariant() == null || ii.getSizeVariant().equals(line.sizeVariant()))
                    .toList();
            for (ItemIngredient recipeLine : recipe) {
                BigDecimal amount = recipeLine.getQuantity().multiply(BigDecimal.valueOf(line.quantity()));
                if (amount.signum() == 0) {
                    continue;
                }
                Long ingredientId = recipeLine.getIngredient().getId();
                InventoryStock stock = stocks.findByIngredientId(ingredientId);
                if (stock == null) {
                    throw new NotFoundException("No stock record for ingredient " + ingredientId);
                }
                BigDecimal remaining = stock.getQtyOnHand().subtract(amount);
                if (remaining.signum() < 0) {
                    throw new InsufficientStockException("Not enough "
                            + stock.getIngredient().getName() + ": have " + stock.getQtyOnHand() + ", need " + amount);
                }
                stock.setQtyOnHand(remaining);
                stock.setLastUpdated(Instant.now());
                transactions.save(
                        new StockTransaction(stock, StockTransactionType.CONSUMED, amount, triggeredBy, null, orderId));
                touched.put(ingredientId, stock);
            }
        }
        List<InventoryStock> result = new ArrayList<>(touched.values());
        result.stream()
                .filter(InventoryStock::isLow)
                .forEach(s -> log.warn(
                        "LOW STOCK: {} at {} {} (threshold {})",
                        s.getIngredient().getName(),
                        s.getQtyOnHand(),
                        s.getIngredient().getUnit(),
                        s.getReorderThreshold()));
        return result;
    }
}
