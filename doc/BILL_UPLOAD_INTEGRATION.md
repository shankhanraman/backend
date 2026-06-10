# Bill Upload Integration

Integrate your FastAPI Bill Scanner with the cafe backend to automatically process supplier invoices and update inventory stock.

---

## Overview

**Bill Scanner (FastAPI)** → **Extract invoice data** → **Spring Boot backend** → **Create supplier purchases** → **Update inventory**

When you upload a bill (image/PDF), the system:
1. Calls the FastAPI Bill Scanner API to extract: vendor, line items, quantities, prices
2. Finds or creates the supplier
3. Finds or creates ingredients for each line item
4. Records stock purchase transactions
5. Updates inventory quantities

---

## Architecture

```
Bill Upload (image/PDF)
         ↓
POST /bill-upload/process
         ↓
BillProcessingService
         ├─ Call FastAPI /scan
         ├─ Parse response (vendor, items, total)
         ├─ Find/create Supplier
         ├─ For each line item:
         │  ├─ Find/create Ingredient
         │  ├─ Get/create InventoryStock
         │  ├─ Create StockTransaction (PURCHASE)
         │  └─ Update stock qty_on_hand
         └─ Return ProcessedBillResponse
         ↓
Response: supplier, ingredients created, stock transactions recorded
```

---

## Setup

### 1. Start the FastAPI Bill Scanner

From your Bill Scanner project:

```powershell
# Terminal 1: Start GLM-OCR server (optional but recommended)
.\run-glmocr.ps1

# Terminal 2: Start the scanner API
.\run.ps1
```

The API will be available at `http://127.0.0.1:8000`

### 2. Configure the Spring Boot Backend

Set the Bill Scanner URL in `application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:15432/cafe
    username: postgres
    password: ${POSTGRES_PASSWORD}

# Optional: set Bill Scanner URL (defaults to http://127.0.0.1:8000)
bill:
  scanner:
    url: ${BILL_SCANNER_URL:http://127.0.0.1:8000}
```

Or set via environment variable:
```powershell
$env:BILL_SCANNER_URL = "http://127.0.0.1:8000"
```

---

## API Endpoint

### Upload & Process Bill

**Endpoint:** `POST /bill-upload/process`

**Request:**
```powershell
# Using curl
curl -X POST "http://localhost:8080/bill-upload/process" \
  -F "file=@invoice.jpg" \
  -F "engine=auto"

# Or with PowerShell
$form = @{
  file = Get-Item -Path "C:\path\to\bill.jpg"
  engine = "auto"
}
Invoke-WebRequest -Uri "http://localhost:8080/bill-upload/process" `
  -Method Post -Form $form
```

**Parameters:**
- `file` (required): Bill image (JPG/PNG) or PDF
- `engine` (optional): OCR engine to use
  - `auto` (default) — tries GLM-OCR first, falls back to Tesseract
  - `glmocr` — local GLM-OCR model (most accurate, ~40s on CPU)
  - `tesseract` — fast OCR (1-3s, less accurate on handwritten)
  - `gemini` — Google Gemini vision (requires API key)

**Response (201 Created):**
```json
{
  "engine": "glmocr",
  "supplier": {
    "id": 1,
    "name": "Local Dairy Co",
    "contact": "9876543210",
    "createdAt": "2026-06-10T12:00:00Z"
  },
  "billNumber": "INV_14253",
  "billDate": "08/06/26",
  "totalAmount": 5000.00,
  "lineItems": [
    {
      "ingredientName": "Whole Milk",
      "unit": "units",
      "quantity": 10,
      "unitPrice": 400.00,
      "newStockLevel": 110.0,
      "transactionId": 1001
    },
    {
      "ingredientName": "Sugar",
      "unit": "units",
      "quantity": 5,
      "unitPrice": 200.00,
      "newStockLevel": 55.0,
      "transactionId": 1002
    }
  ],
  "warnings": []
}
```

---

### Check Bill Scanner Health

**Endpoint:** `GET /bill-upload/health`

**Response (200 OK):**
```json
{
  "status": true,
  "message": "All systems operational",
  "engines": {
    "glmocr_available": true,
    "tesseract_available": true,
    "gemini_available": false
  }
}
```

---

## Data Flow

### Example: Processing an Invoice

**Bill content:**
```
Local Dairy Co
Phone: 9876543210

Item              Qty    Price    Total
─────────────────────────────────────
Whole Milk        10     400      4,000
Sugar             5      200      1,000
─────────────────────────────────────
Total:                           5,000
```

**Step-by-step processing:**

1. **Upload bill image**
   ```powershell
   POST /bill-upload/process with invoice.jpg
   ```

2. **Bill Scanner extracts:**
   ```json
   {
     "vendor_name": "Local Dairy Co",
     "vendor_contact": "9876543210",
     "bill_number": null,
     "line_items": [
       {"description": "Whole Milk", "quantity": 10, "unit_price": 400},
       {"description": "Sugar", "quantity": 5, "unit_price": 200}
     ],
     "total": 5000
   }
   ```

3. **Backend processes:**
   - **Find/create Supplier:** "Local Dairy Co" with contact "9876543210"
   - **For "Whole Milk":**
     - Find/create Ingredient "Whole Milk" (unit: "units")
     - Get InventoryStock for Whole Milk
     - Create StockTransaction: RESTOCKED, quantity=10, supplier=Local Dairy Co
     - Update stock: qty_on_hand += 10
   - **For "Sugar":**
     - Find/create Ingredient "Sugar" (unit: "units")
     - Get InventoryStock for Sugar
     - Create StockTransaction: PURCHASE, quantity=5, supplier=Local Dairy Co
     - Update stock: qty_on_hand += 5

4. **Response returns:**
   - Supplier created/found
   - 2 ingredients created/found
   - 2 stock transactions recorded
   - Stock levels updated

---

## Classes & Components

### BillScannerClient
HTTP client to communicate with FastAPI Bill Scanner API.

**Methods:**
- `scanBill(MultipartFile, String engine)` — Upload bill and get extracted data
- `checkHealth()` — Check scanner service status

**DTOs:**
- `ScanResponse` — API response wrapper
- `BillData` — Extracted bill information
- `LineItem` — Individual invoice line item
- `HealthResponse` — Service health status

### BillProcessingService
Business logic for converting bill data into supplier purchases.

**Methods:**
- `processBill(MultipartFile, String engine)` — Main entry point
- `findOrCreateSupplier(BillData)` — Get or create supplier
- `processLineItem(LineItem, Supplier)` — Create ingredient & stock transaction
- `findOrCreateIngredient(String)` — Get or create ingredient

**Returns:**
- `ProcessedBillResponse` — Summary of created objects and transactions

### BillUploadController
REST endpoints for bill upload operations.

**Endpoints:**
- `POST /bill-upload/process` — Upload and process bill
- `GET /bill-upload/health` — Check scanner health

---

## Error Handling

### Bill Scanner Unavailable
```
Status: 503 Service Unavailable
Message: "Bill Scanner service unavailable"
```

**Solution:**
1. Check FastAPI process is running: `.\run.ps1`
2. Verify Bill Scanner is accessible: `GET /bill-upload/health`
3. Check `BILL_SCANNER_URL` environment variable

### Missing Vendor Name
```
Status: 400 Bad Request
Message: "Bill must have a vendor name to create supplier"
```

**Solution:**
- Ensure the bill clearly shows the vendor/supplier name
- Try different OCR engine: `engine=glmocr` (most accurate)

### Invalid Quantity
```
Status: 400 Bad Request
Message: "Line item quantity must be greater than 0"
```

**Solution:**
- Bill scanning error; check bill image quality
- Manual correction may be needed

---

## Workflow in Cafe Operations

### Receiving Stock from Supplier

**Scenario:** You receive a physical invoice from Local Dairy Co with milk and supplies.

1. **Take a photo** of the invoice with your phone
2. **Upload via API:**
   ```powershell
   curl -X POST "http://localhost:8080/bill-upload/process" \
     -F "file=@invoice.jpg"
   ```
3. **System automatically:**
   - Extracts vendor, items, quantities, prices
   - Updates supplier contact info
   - Records stock purchase in inventory
   - Updates `inventory_stock.qty_on_hand`
4. **Staff verifies** the processed bill response
5. **Inventory is ready** to use in orders

### Benefits

- **Speed:** No manual data entry
- **Accuracy:** OCR + LLM extraction vs. human typing
- **Auditability:** Every transaction logged in `stock_transaction` table
- **Offline-capable:** GLM-OCR works without internet

---

## Configuration

### `.env` (Bill Scanner project)

```
BILL_SCANNER_URL=http://127.0.0.1:8000
TESSERACT_CMD=C:\Program Files\Tesseract-OCR\tesseract.exe
GLM_OCR_MAX_DIM=1024
LLM_API_KEY=your-api-key-optional
LLM_MODEL=gpt-3.5-turbo
```

### `application-dev.yml` (Spring Boot)

```yaml
bill:
  scanner:
    url: ${BILL_SCANNER_URL:http://127.0.0.1:8000}
    timeout-seconds: 120  # OCR can take 40s on CPU
```

---

## Testing

### Health Check

```powershell
curl http://localhost:8080/bill-upload/health
```

Expected: `{"status": true, "engines": {...}}`

### Process a Sample Bill

```powershell
$form = @{
  file = Get-Item -Path "sample_bill.jpg"
  engine = "tesseract"  # Fast, for testing
}
Invoke-WebRequest -Uri "http://localhost:8080/bill-upload/process" `
  -Method Post -Form $form -OutFile response.json
Get-Content response.json | ConvertFrom-Json | Format-Table
```

---

## Troubleshooting

| Problem | Cause | Solution |
|---------|-------|----------|
| 503 Service Unavailable | Bill Scanner not running | Start `.\run.ps1` |
| 400 Bad Request (vendor) | OCR couldn't read vendor | Use `engine=glmocr`, better lighting |
| Empty line items | Poor bill image quality | Retake photo, higher contrast |
| Timeout | Scanner is slow on CPU | Use `engine=tesseract` for speed |
| Ingredient auto-creation issues | Name conflicts | Add manual ingredient mapping later |

---

## Future Enhancements

1. **Ingredient Mapping:** Map OCR'd item names to existing ingredients (fuzzy matching)
2. **Supplier Matching:** Find suppliers by name; avoid duplicates
3. **Bill History:** Store processed bills with OCR confidence scores
4. **Batch Upload:** Process multiple bills at once
5. **Manual Review:** Preview extracted data before committing to inventory
6. **Unit Inference:** Auto-detect units (kg, litres, units) from context

