package com.arogya.cafe.supplier.service;
import com.arogya.cafe.supplier.*;
import com.arogya.cafe.supplier.client.*;

import com.arogya.cafe.catalog.entity.Ingredient;
import com.arogya.cafe.catalog.repository.IngredientRepository;
import com.arogya.cafe.inventory.entity.InventoryStock;
import com.arogya.cafe.inventory.repository.InventoryStockRepository;
import com.arogya.cafe.inventory.entity.StockTransaction;
import com.arogya.cafe.inventory.repository.StockTransactionRepository;
import com.arogya.cafe.inventory.entity.Supplier;
import com.arogya.cafe.inventory.repository.SupplierRepository;
import com.arogya.cafe.common.enums.StockTransactionType;
import com.arogya.cafe.common.exception.NotFoundException;
import com.arogya.cafe.common.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class BillProcessingService {

    private final BillScannerClient billScannerClient;
    private final SupplierRepository supplierRepository;
    private final IngredientRepository ingredientRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final StockTransactionRepository stockTransactionRepository;

    public BillProcessingService(
            BillScannerClient billScannerClient,
            SupplierRepository supplierRepository,
            IngredientRepository ingredientRepository,
            InventoryStockRepository inventoryStockRepository,
            StockTransactionRepository stockTransactionRepository) {
        this.billScannerClient = billScannerClient;
        this.supplierRepository = supplierRepository;
        this.ingredientRepository = ingredientRepository;
        this.inventoryStockRepository = inventoryStockRepository;
        this.stockTransactionRepository = stockTransactionRepository;
    }

    /**
     * Scan a bill image and process it into supplier purchases.
     *
     * @param billFile the bill image/PDF
     * @param engine OCR engine: "auto", "glmocr", "tesseract", "gemini"
     * @return ProcessedBillResponse with supplier, items, and transactions created
     */
    public ProcessedBillResponse processBill(org.springframework.web.multipart.MultipartFile billFile, String engine) {
        // Step 1: Scan the bill
        BillScannerClient.ScanResponse scanResponse = billScannerClient.scanBill(billFile, engine);

        if (!scanResponse.success) {
            throw new BillScannerException("Bill scanning failed");
        }

        BillScannerClient.BillData billData = scanResponse.data;

        // Step 2: Find or create supplier
        Supplier supplier = findOrCreateSupplier(billData);

        // Step 3: Process line items and create stock transactions
        List<ProcessedLineItem> processedItems = new ArrayList<>();
        for (BillScannerClient.LineItem item : billData.getLineItems()) {
            ProcessedLineItem processed = processLineItem(item, supplier);
            processedItems.add(processed);
        }

        return new ProcessedBillResponse(
                scanResponse.engine_used,
                supplier,
                billData.getBillNumber(),
                billData.getBillDate(),
                billData.getTotal(),
                processedItems,
                scanResponse.warnings
        );
    }

    private Supplier findOrCreateSupplier(BillScannerClient.BillData billData) {
        String vendorName = billData.getVendorName();
        String vendorContact = billData.getVendorContact();

        if (vendorName == null || vendorName.isBlank()) {
            throw new BusinessRuleException("Bill must have a vendor name to create supplier");
        }

        // Try to find by name
        // (assuming SupplierRepository has a findByName method; if not, add it)
        // For now, we'll create a new one or update if needed
        Supplier supplier = new Supplier(vendorName, vendorContact != null ? vendorContact : "");
        return supplierRepository.save(supplier);
    }

    private ProcessedLineItem processLineItem(BillScannerClient.LineItem billItem, Supplier supplier) {
        String itemDescription = billItem.getDescription();
        Integer quantity = billItem.getQuantity();
        BigDecimal unitPrice = billItem.getUnitPrice();

        if (itemDescription == null || itemDescription.isBlank()) {
            throw new BusinessRuleException("Line item must have a description");
        }

        if (quantity == null || quantity <= 0) {
            throw new BusinessRuleException("Line item quantity must be greater than 0");
        }

        // Step 1: Find or create ingredient by name
        Ingredient ingredient = findOrCreateIngredient(itemDescription);

        // Step 2: Get or create inventory stock
        InventoryStock stock = inventoryStockRepository.findByIngredientId(ingredient.getId());
        if (stock == null) {
            stock = new InventoryStock(
                    ingredient,
                    BigDecimal.ZERO,
                    BigDecimal.TEN  // Default reorder threshold
            );
            stock = inventoryStockRepository.save(stock);
        }

        // Step 3: Create stock transaction (RESTOCKED from supplier)
        StockTransaction transaction = new StockTransaction(
                stock,
                StockTransactionType.RESTOCKED,
                new BigDecimal(quantity),
                "bill_upload",
                supplier,
                null  // No order associated with bill upload
        );
        transaction = stockTransactionRepository.save(transaction);

        // Step 4: Update inventory stock quantity
        stock.setQtyOnHand(stock.getQtyOnHand().add(new BigDecimal(quantity)));
        stock.setLastUpdated(Instant.now());
        inventoryStockRepository.save(stock);

        return new ProcessedLineItem(
                ingredient.getName(),
                ingredient.getUnit(),
                quantity,
                unitPrice,
                stock.getQtyOnHand(),
                transaction.getId()
        );
    }

    private Ingredient findOrCreateIngredient(String name) {
        // Try to find existing ingredient
        // For now, assume we need to create new ones from bills
        // In production, you might want to fuzzy-match or require manual mapping
        Ingredient ingredient = new Ingredient(name, "units");  // Default unit; could improve this
        return ingredientRepository.save(ingredient);
    }

    // DTOs for API response
    public static class ProcessedBillResponse {
        public String engine;
        public Supplier supplier;
        public String billNumber;
        public String billDate;
        public BigDecimal totalAmount;
        public List<ProcessedLineItem> lineItems;
        public List<String> warnings;

        public ProcessedBillResponse(String engine, Supplier supplier, String billNumber, String billDate,
                                    BigDecimal totalAmount, List<ProcessedLineItem> lineItems, List<String> warnings) {
            this.engine = engine;
            this.supplier = supplier;
            this.billNumber = billNumber;
            this.billDate = billDate;
            this.totalAmount = totalAmount;
            this.lineItems = lineItems;
            this.warnings = warnings;
        }
    }

    public static class ProcessedLineItem {
        public String ingredientName;
        public String unit;
        public Integer quantity;
        public BigDecimal unitPrice;
        public BigDecimal newStockLevel;
        public Long transactionId;

        public ProcessedLineItem(String ingredientName, String unit, Integer quantity,
                               BigDecimal unitPrice, BigDecimal newStockLevel, Long transactionId) {
            this.ingredientName = ingredientName;
            this.unit = unit;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.newStockLevel = newStockLevel;
            this.transactionId = transactionId;
        }
    }
}
