package com.arogya.cafe.supplier.client;
import com.arogya.cafe.supplier.BillScannerException;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

@Component
public class BillScannerClient {

    private final RestTemplate restTemplate;
    private final String billScannerUrl;

    public BillScannerClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.billScannerUrl = System.getenv().getOrDefault("BILL_SCANNER_URL", "http://127.0.0.1:8000");
    }

    public ScanResponse scanBill(MultipartFile file, String engine) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());
            if (engine != null && !engine.isBlank()) {
                body.add("engine", engine);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            ScanResponse response = restTemplate.postForObject(
                    billScannerUrl + "/scan",
                    request,
                    ScanResponse.class
            );

            return response;
        } catch (Exception e) {
            throw new BillScannerException("Failed to scan bill: " + e.getMessage(), e);
        }
    }

    public HealthResponse checkHealth() {
        try {
            return restTemplate.getForObject(billScannerUrl + "/health", HealthResponse.class);
        } catch (Exception e) {
            throw new BillScannerException("Bill Scanner service unavailable", e);
        }
    }

    // DTOs for API responses
    public static class ScanResponse {
        public boolean success;
        public String engine_used;
        public BillData data;
        public List<String> warnings;

        public String getEngineUsed() {
            return engine_used;
        }

        public BillData getData() {
            return data;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }

    public static class BillData {
        @JsonProperty("vendor_name")
        public String vendorName;

        @JsonProperty("vendor_contact")
        public String vendorContact;

        @JsonProperty("bill_number")
        public String billNumber;

        @JsonProperty("bill_date")
        public String billDate;

        public String currency;

        @JsonProperty("line_items")
        public List<LineItem> lineItems;

        public BigDecimal subtotal;
        public BigDecimal total;

        @JsonProperty("payment_method")
        public String paymentMethod;

        // Getters
        public String getVendorName() {
            return vendorName;
        }

        public String getVendorContact() {
            return vendorContact;
        }

        public String getBillNumber() {
            return billNumber;
        }

        public String getBillDate() {
            return billDate;
        }

        public List<LineItem> getLineItems() {
            return lineItems;
        }

        public BigDecimal getTotal() {
            return total;
        }
    }

    public static class LineItem {
        public String description;
        public Integer quantity;

        @JsonProperty("unit_price")
        public BigDecimal unitPrice;

        public BigDecimal amount;

        // Getters
        public String getDescription() {
            return description;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }

    public static class HealthResponse {
        public boolean status;
        public String message;
        public EngineStatus engines;

        public static class EngineStatus {
            public boolean glmocr_available;
            public boolean tesseract_available;
            public boolean gemini_available;
        }
    }
}
