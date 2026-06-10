package com.arogya.cafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arogya.cafe.catalog.dto.CatalogDtos.CategoryRequest;
import com.arogya.cafe.catalog.dto.CatalogDtos.IngredientRequest;
import com.arogya.cafe.catalog.dto.CatalogDtos.ItemIngredientRequest;
import com.arogya.cafe.catalog.dto.CatalogDtos.MenuItemRequest;
import com.arogya.cafe.catalog.entity.Category;
import com.arogya.cafe.catalog.entity.Ingredient;
import com.arogya.cafe.catalog.entity.MenuItem;
import com.arogya.cafe.catalog.service.CatalogService;
import com.arogya.cafe.common.enums.KotStatus;
import com.arogya.cafe.common.enums.OrderStatus;
import com.arogya.cafe.common.enums.PaymentStatus;
import com.arogya.cafe.common.enums.StaffRole;
import com.arogya.cafe.common.enums.StockTransactionType;
import com.arogya.cafe.common.exception.BusinessRuleException;
import com.arogya.cafe.inventory.dto.InventoryDtos.CreateStockRequest;
import com.arogya.cafe.inventory.dto.InventoryDtos.RestockRequest;
import com.arogya.cafe.inventory.entity.StockTransaction;
import com.arogya.cafe.inventory.repository.InventoryStockRepository;
import com.arogya.cafe.inventory.repository.StockTransactionRepository;
import com.arogya.cafe.inventory.service.StockService;
import com.arogya.cafe.ordering.dto.OrderingDtos.BillResponse;
import com.arogya.cafe.ordering.dto.OrderingDtos.CreateOrderRequest;
import com.arogya.cafe.ordering.dto.OrderingDtos.CustomerRequest;
import com.arogya.cafe.ordering.dto.OrderingDtos.KotResponse;
import com.arogya.cafe.ordering.dto.OrderingDtos.OrderLineRequest;
import com.arogya.cafe.ordering.dto.OrderingDtos.OrderResponse;
import com.arogya.cafe.ordering.entity.Customer;
import com.arogya.cafe.ordering.service.BillService;
import com.arogya.cafe.ordering.service.KotService;
import com.arogya.cafe.ordering.service.OrderService;
import com.arogya.cafe.security.entity.Staff;
import com.arogya.cafe.security.repository.StaffRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Reproduces the quick-ref worked example end-to-end through the service layer on H2. */
@SpringBootTest
@ActiveProfiles("test")
class OrderWorkflowIntegrationTest {

    @Autowired
    private CatalogService catalog;
    @Autowired
    private StockService stock;
    @Autowired
    private OrderService orders;
    @Autowired
    private KotService kots;
    @Autowired
    private BillService bills;
    @Autowired
    private StaffRepository staffRepo;
    @Autowired
    private InventoryStockRepository stockRepo;
    @Autowired
    private StockTransactionRepository txnRepo;

    private Staff staff(StaffRole role, String username) {
        return staffRepo.save(new Staff(username, role, username, "x"));
    }

    @Test
    void oneRegularBadamShakeWalksTheFullWorkflowAndDeductsStock() {
        // ---- Arrange: catalog, recipe, stock, people ----
        Category category = catalog.createCategory(new CategoryRequest("Milk & Shakes"));
        Ingredient premix = catalog.createIngredient(new IngredientRequest("Premix Sachet", "pcs"));
        Ingredient milk = catalog.createIngredient(new IngredientRequest("Milk", "ml"));

        MenuItem badam = catalog.createMenuItem(
                new MenuItemRequest("Badam Shake", "Regular", BigDecimal.valueOf(180), category.getId()));
        catalog.addRecipeLine(badam.getId(),
                new ItemIngredientRequest(premix.getId(), BigDecimal.ONE, "pcs", "Regular"));
        catalog.addRecipeLine(badam.getId(),
                new ItemIngredientRequest(milk.getId(), BigDecimal.valueOf(180), "ml", "Regular"));

        stock.createStock(new CreateStockRequest(premix.getId(), BigDecimal.valueOf(50), BigDecimal.valueOf(10)));
        stock.createStock(new CreateStockRequest(milk.getId(), BigDecimal.valueOf(20000), BigDecimal.valueOf(5000)));

        Customer customer = orders.createCustomer(new CustomerRequest("Walk-in", "NA"));
        Staff cashier = staff(StaffRole.CASHIER, "cashier");
        Staff chef = staff(StaffRole.CHEF, "chef");
        Staff server = staff(StaffRole.SERVER, "server");

        // ---- 1: Cashier creates the order; Bill + KOT auto-generated ----
        OrderResponse order = orders.createOrder(
                new CreateOrderRequest(customer.getId(), List.of(new OrderLineRequest(badam.getId(), "Regular", 1)), "cash"),
                cashier);
        assertEquals(OrderStatus.CREATED, order.status());
        assertEquals(KotStatus.PENDING, order.kot().status());
        assertEquals(PaymentStatus.UNPAID, order.bill().paymentStatus());
        assertEquals(0, order.bill().totalAmount().compareTo(BigDecimal.valueOf(180)), "bill total should be Rs 180");

        // ---- 2: Chef prepares the KOT -> auto stock deduction ----
        KotResponse kot = kots.markPrepared(order.kot().id(), chef);
        assertEquals(KotStatus.PREPARED, kot.status());

        assertEquals(0, stockRepo.findByIngredientId(premix.getId()).getQtyOnHand().compareTo(BigDecimal.valueOf(49)),
                "premix 50 -> 49");
        assertEquals(0, stockRepo.findByIngredientId(milk.getId()).getQtyOnHand().compareTo(BigDecimal.valueOf(19820)),
                "milk 20000 -> 19820");

        List<StockTransaction> consumed = txnRepo.findByOrderId(order.id()).stream()
                .filter(t -> t.getType() == StockTransactionType.CONSUMED).toList();
        assertEquals(2, consumed.size(), "two CONSUMED transactions logged");

        // ---- 3: Server serves (allowed because KOT is prepared) ----
        OrderResponse served = orders.markServed(order.id(), server);
        assertEquals(OrderStatus.SERVED, served.status());

        // ---- 4: Cashier takes payment -> order completed ----
        BillResponse paid = bills.pay(order.bill().id(), cashier);
        assertEquals(PaymentStatus.PAID, paid.paymentStatus());
        assertEquals(OrderStatus.COMPLETED, orders.getOrder(order.id()).status());

        // ---- 5: Next-day restock: Milk 19,820 -> 29,820 with a RESTOCKED txn ----
        var restocked = stock.restock(milk.getId(), new RestockRequest(BigDecimal.valueOf(10000), null),
                "test restock");
        assertEquals(0, restocked.getQtyOnHand().compareTo(BigDecimal.valueOf(29820)));

        boolean hasRestock = stock.transactionsForIngredient(milk.getId()).stream()
                .anyMatch(t -> t.getType() == StockTransactionType.RESTOCKED);
        assertTrue(hasRestock, "restock transaction logged");
    }

    @Test
    void servingBeforeKotPreparedIsRejected() {
        Category category = catalog.createCategory(new CategoryRequest("Cat2"));
        MenuItem item = catalog.createMenuItem(
                new MenuItemRequest("Tea", "Regular", BigDecimal.valueOf(20), category.getId()));
        Customer customer = orders.createCustomer(new CustomerRequest("Walk-in 2", "NA"));
        Staff cashier = staff(StaffRole.CASHIER, "cashier2");
        Staff server = staff(StaffRole.SERVER, "server2");

        OrderResponse order = orders.createOrder(
                new CreateOrderRequest(customer.getId(), List.of(new OrderLineRequest(item.getId(), "Regular", 1)), "cash"),
                cashier);

        Throwable ex = null;
        try {
            orders.markServed(order.id(), server);
        } catch (Throwable t) {
            ex = t;
        }
        assertTrue(ex instanceof BusinessRuleException, "should refuse to serve un-prepared order");
    }
}
