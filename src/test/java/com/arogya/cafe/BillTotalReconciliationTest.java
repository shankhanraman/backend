package com.arogya.cafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.arogya.cafe.catalog.dto.CatalogDtos.CategoryRequest;
import com.arogya.cafe.catalog.dto.CatalogDtos.MenuItemRequest;
import com.arogya.cafe.catalog.entity.Category;
import com.arogya.cafe.catalog.entity.MenuItem;
import com.arogya.cafe.catalog.service.CatalogService;
import com.arogya.cafe.common.enums.StaffRole;
import com.arogya.cafe.ordering.dto.OrderingDtos.CreateOrderRequest;
import com.arogya.cafe.ordering.dto.OrderingDtos.CustomerRequest;
import com.arogya.cafe.ordering.dto.OrderingDtos.OrderLineRequest;
import com.arogya.cafe.ordering.dto.OrderingDtos.OrderResponse;
import com.arogya.cafe.ordering.entity.Customer;
import com.arogya.cafe.ordering.service.OrderService;
import com.arogya.cafe.security.entity.Staff;
import com.arogya.cafe.security.repository.StaffRepository;
import com.arogya.cafe.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Billing-accuracy guard: a Bill's total must equal the sum of its order lines' price snapshots
 * times quantities — exactly, to the paisa. Multi-line, multi-quantity, mixed prices.
 */
@SpringBootTest
class BillTotalReconciliationTest extends AbstractIntegrationTest {

    @Autowired
    private CatalogService catalog;

    @Autowired
    private OrderService orders;

    @Autowired
    private StaffRepository staffRepo;

    @Test
    void billTotalEqualsSumOfLinePriceSnapshots() {
        Category category = catalog.createCategory(new CategoryRequest("Beverages"));
        MenuItem chai = catalog.createMenuItem(
                new MenuItemRequest("Masala Chai", "Regular", new BigDecimal("18.50"), category.getId()));
        MenuItem shake = catalog.createMenuItem(
                new MenuItemRequest("Badam Shake", "Regular", new BigDecimal("180.00"), category.getId()));

        Customer customer = orders.createCustomer(new CustomerRequest("Walk-in", "NA"));
        Staff cashier = staffRepo.save(new Staff("cashier", StaffRole.CASHIER, "cashier", "x"));

        // 3 chai @ 18.50 + 2 shakes @ 180.00 = 55.50 + 360.00 = 415.50
        OrderResponse order = orders.createOrder(
                new CreateOrderRequest(
                        customer.getId(),
                        List.of(
                                new OrderLineRequest(chai.getId(), "Regular", 3),
                                new OrderLineRequest(shake.getId(), "Regular", 2)),
                        "cash"),
                cashier);

        BigDecimal expected = new BigDecimal("415.50");
        assertEquals(
                0,
                order.bill().totalAmount().compareTo(expected),
                "bill total must equal Σ(unit price snapshot × quantity)");
    }
}
