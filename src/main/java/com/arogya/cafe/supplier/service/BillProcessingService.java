package com.arogya.cafe.supplier.service;

import com.arogya.cafe.catalog.entity.Ingredient;
import com.arogya.cafe.catalog.repository.IngredientRepository;
import com.arogya.cafe.common.enums.StockTransactionType;
import com.arogya.cafe.common.exception.BusinessRuleException;
import com.arogya.cafe.inventory.entity.InventoryStock;
import com.arogya.cafe.inventory.entity.StockTransaction;
import com.arogya.cafe.inventory.entity.Supplier;
import com.arogya.cafe.inventory.repository.InventoryStockRepository;
import com.arogya.cafe.inventory.repository.StockTransactionRepository;
import com.arogya.cafe.inventory.repository.SupplierRepository;
import com.arogya.cafe.supplier.*;
import com.arogya.cafe.supplier.client.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                scanResponse.warnings);
    }

    /**
     * Scan a bill and return the extracted fields WITHOUT touching the database. Lets the UI show
     * the OCR result for review/correction (add a missing vendor, fix items) before committing.
     */
    public ScanPreview scanPreview(org.springframework.web.multipart.MultipartFile billFile, String engine) {
        BillScannerClient.ScanResponse scan = billScannerClient.scanBill(billFile, engine);
        if (!scan.success) {
            throw new BillScannerException("Bill scanning failed");
        }
        BillScannerClient.BillData d = scan.data;
        List<PreviewLine> lines = new ArrayList<>();
        if (d.getLineItems() != null) {
            for (BillScannerClient.LineItem li : d.getLineItems()) {
                lines.add(new PreviewLine(li.getDescription(), li.getQuantity(), li.getUnitPrice()));
            }
        }
        return new ScanPreview(
                scan.engine_used,
                d.getVendorName(),
                d.getVendorContact(),
                d.getBillNumber(),
                d.getBillDate(),
                d.getTotal(),
                lines,
                scan.warnings != null ? scan.warnings : List.of());
    }

    /**
     * Commit a reviewed/edited bill: create the supplier, then find-or-create each ingredient by
     * name and restock it. No OCR here — the data comes from the (corrected) UI form.
     */
    public ProcessedBillResponse commitBill(CommitRequest req) {
        if (req.vendorName() == null || req.vendorName().isBlank()) {
            throw new BusinessRuleException("Vendor name is required");
        }
        if (req.lineItems() == null || req.lineItems().isEmpty()) {
            throw new BusinessRuleException("Add at least one line item");
        }
        Supplier supplier = supplierRepository.save(new Supplier(
                req.vendorName().trim(),
                req.vendorContact() != null ? req.vendorContact().trim() : ""));

        List<ProcessedLineItem> processed = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CommitLine l : req.lineItems()) {
            if (l.ingredientName() == null || l.ingredientName().isBlank()) {
                continue;
            }
            if (l.quantity() == null || l.quantity() <= 0) {
                throw new BusinessRuleException("Quantity must be greater than 0 for " + l.ingredientName());
            }
            String name = l.ingredientName().trim();
            String unit = l.unit() != null && !l.unit().isBlank() ? l.unit().trim() : "units";
            Ingredient ingredient = ingredientRepository
                    .findByNameIgnoreCase(name)
                    .orElseGet(() -> ingredientRepository.save(new Ingredient(name, unit)));

            InventoryStock stock = inventoryStockRepository.findByIngredientId(ingredient.getId());
            if (stock == null) {
                stock = inventoryStockRepository.save(new InventoryStock(ingredient, BigDecimal.ZERO, BigDecimal.TEN));
            }
            BigDecimal qty = new BigDecimal(l.quantity());
            StockTransaction txn = stockTransactionRepository.save(new StockTransaction(
                    stock, StockTransactionType.RESTOCKED, qty, "Bill upload: " + supplier.getName(), supplier, null));
            stock.setQtyOnHand(stock.getQtyOnHand().add(qty));
            stock.setLastUpdated(Instant.now());
            inventoryStockRepository.save(stock);

            if (l.unitPrice() != null) {
                total = total.add(l.unitPrice().multiply(qty));
            }
            processed.add(new ProcessedLineItem(
                    ingredient.getName(),
                    ingredient.getUnit(),
                    l.quantity(),
                    l.unitPrice(),
                    stock.getQtyOnHand(),
                    txn.getId()));
        }
        return new ProcessedBillResponse(
                "reviewed", supplier, req.billNumber(), req.billDate(), total, processed, List.of());
    }

    // ---- Review/commit DTOs ----
    public record PreviewLine(String description, Integer quantity, BigDecimal unitPrice) {}

    public record ScanPreview(
            String engine,
            String vendorName,
            String vendorContact,
            String billNumber,
            String billDate,
            BigDecimal totalAmount,
            List<PreviewLine> lineItems,
            List<String> warnings) {}

    public record CommitLine(String ingredientName, String unit, Integer quantity, BigDecimal unitPrice) {}

    public record CommitRequest(
            String vendorName, String vendorContact, String billNumber, String billDate, List<CommitLine> lineItems) {}

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
                    ingredient, BigDecimal.ZERO, BigDecimal.TEN // Default reorder threshold
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
                null // No order associated with bill upload
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
                transaction.getId());
    }

    private Ingredient findOrCreateIngredient(String name) {
        // Try to find existing ingredient
        // For now, assume we need to create new ones from bills
        // In production, you might want to fuzzy-match or require manual mapping
        Ingredient ingredient = new Ingredient(name, "units"); // Default unit; could improve this
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

        public ProcessedBillResponse(
                String engine,
                Supplier supplier,
                String billNumber,
                String billDate,
                BigDecimal totalAmount,
                List<ProcessedLineItem> lineItems,
                List<String> warnings) {
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

        public ProcessedLineItem(
                String ingredientName,
                String unit,
                Integer quantity,
                BigDecimal unitPrice,
                BigDecimal newStockLevel,
                Long transactionId) {
            this.ingredientName = ingredientName;
            this.unit = unit;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.newStockLevel = newStockLevel;
            this.transactionId = transactionId;
        }
    }
}
