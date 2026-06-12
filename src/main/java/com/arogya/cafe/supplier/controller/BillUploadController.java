package com.arogya.cafe.supplier.controller;

import com.arogya.cafe.common.exception.BusinessRuleException;
import com.arogya.cafe.supplier.BillScannerException;
import com.arogya.cafe.supplier.client.*;
import com.arogya.cafe.supplier.service.*;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/bill-upload")
public class BillUploadController {

    private final BillProcessingService billProcessingService;
    private final BillScannerClient billScannerClient;

    public BillUploadController(BillProcessingService billProcessingService, BillScannerClient billScannerClient) {
        this.billProcessingService = billProcessingService;
        this.billScannerClient = billScannerClient;
    }

    /**
     * Upload and process a bill (invoice/receipt) to create supplier stock purchase.
     *
     * @param file Bill image (JPG/PNG) or PDF
     * @param engine OCR engine: "auto" (default), "glmocr", "tesseract", "gemini"
     * @return Processed bill with created supplier, ingredients, and stock transactions
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BillProcessingService.ProcessedBillResponse> processBill(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "engine", required = false, defaultValue = "auto") String engine) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            BillProcessingService.ProcessedBillResponse response = billProcessingService.processBill(file, engine);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BillScannerException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Scan a bill and return the extracted fields for review/editing — does NOT write to the
     * database. The UI shows this, the user corrects it (e.g. adds a missing vendor), then commits.
     */
    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> scanOnly(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "engine", required = false, defaultValue = "auto") String engine) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(billProcessingService.scanPreview(file, engine));
        } catch (BillScannerException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /** Commit the reviewed/edited bill: create the supplier and restock each ingredient. */
    @PostMapping("/commit")
    public ResponseEntity<?> commit(@RequestBody BillProcessingService.CommitRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(billProcessingService.commitBill(req));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Could not commit bill"));
        }
    }

    /**
     * Check health of the bill scanner service.
     *
     * @return Health status and available OCR engines
     */
    @GetMapping("/health")
    public ResponseEntity<BillScannerClient.HealthResponse> checkHealth() {
        try {
            BillScannerClient.HealthResponse health = billScannerClient.checkHealth();
            return ResponseEntity.ok(health);
        } catch (BillScannerException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}
