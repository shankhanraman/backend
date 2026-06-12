package com.arogya.cafe.ordering.service;

import com.arogya.cafe.catalog.entity.*;
import com.arogya.cafe.catalog.entity.MenuItem;
import com.arogya.cafe.catalog.repository.MenuItemRepository;
import com.arogya.cafe.common.enums.KotStatus;
import com.arogya.cafe.common.enums.OrderStatus;
import com.arogya.cafe.common.enums.PaymentStatus;
import com.arogya.cafe.common.exception.BusinessRuleException;
import com.arogya.cafe.common.exception.NotFoundException;
import com.arogya.cafe.ordering.dto.OrderingDtos.CreateOrderRequest;
import com.arogya.cafe.ordering.dto.OrderingDtos.CustomerRequest;
import com.arogya.cafe.ordering.dto.OrderingDtos.OrderResponse;
import com.arogya.cafe.ordering.entity.*;
import com.arogya.cafe.ordering.repository.*;
import com.arogya.cafe.security.entity.*;
import com.arogya.cafe.security.entity.Staff;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

    private final CustomerRepository customers;
    private final OrderRepository orders;
    private final OrderLineRepository orderLines;
    private final KotRepository kots;
    private final BillRepository bills;
    private final MenuItemRepository menuItems;

    public OrderService(
            CustomerRepository customers,
            OrderRepository orders,
            OrderLineRepository orderLines,
            KotRepository kots,
            BillRepository bills,
            MenuItemRepository menuItems) {
        this.customers = customers;
        this.orders = orders;
        this.orderLines = orderLines;
        this.kots = kots;
        this.bills = bills;
        this.menuItems = menuItems;
    }

    // ---- Customer CRUD ----
    public Customer createCustomer(CustomerRequest req) {
        return customers.save(new Customer(req.name(), req.contact()));
    }

    @Transactional(readOnly = true)
    public List<Customer> listCustomers() {
        return customers.findAll();
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(Long id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer " + id + " not found"));
    }

    // ---- Order workflow ----

    /**
     * Create an order with its lines, then auto-generate Bill and KOT.
     * Payment is processed immediately (UNPAID → PAID).
     * KOT is sent to kitchen only after successful payment.
     * Enforces the "at least one order line" rule. Performed by a cashier.
     */
    public OrderResponse createOrder(CreateOrderRequest req, Staff cashier) {
        if (req.lines().isEmpty()) {
            throw new BusinessRuleException("An order must have at least one line");
        }
        Customer customer = getCustomer(req.customerId());

        Order order = new Order(customer);
        order.getHandledBy().add(cashier);
        order = orders.save(order);

        List<OrderLine> lines = new ArrayList<>();
        for (var lineReq : req.lines()) {
            MenuItem menuItem = menuItems
                    .findById(lineReq.menuItemId())
                    .orElseThrow(() -> new NotFoundException("MenuItem " + lineReq.menuItemId() + " not found"));
            lines.add(orderLines.save(
                    new OrderLine(order, menuItem, lineReq.sizeVariant(), lineReq.quantity(), menuItem.getPrice())));
        }

        BigDecimal total = lines.stream().map(OrderLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Bill bill = bills.save(new Bill(order, total));

        // Step 1: Payment is processed immediately
        bill.setPaymentStatus(PaymentStatus.PAID);
        bill = bills.save(bill);

        // Step 2: KOT is sent to kitchen only after payment succeeds
        Kot kot = kots.save(new Kot(order));

        return OrderResponse.build(order, lines, kot, bill);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        Order order = orders.findById(id).orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
        return assemble(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders() {
        return orders.findAll().stream().map(this::assemble).toList();
    }

    /**
     * Mark an order served. Allowed only once the KOT is PREPARED. Performed by a server.
     */
    public OrderResponse markServed(Long orderId, Staff server) {
        Order order =
                orders.findById(orderId).orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
        Kot kot = kots.findByOrderId(orderId);
        if (kot == null) {
            throw new NotFoundException("No KOT for order " + orderId);
        }
        if (kot.getStatus() != KotStatus.PREPARED) {
            throw new BusinessRuleException("Order " + orderId + " cannot be served until its KOT is prepared");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessRuleException("Order " + orderId + " is already completed");
        }
        order.setStatus(OrderStatus.SERVED);
        order.getHandledBy().add(server);
        return assemble(order);
    }

    private OrderResponse assemble(Order order) {
        Long orderId = order.getId();
        List<OrderLine> lines = orderLines.findByOrderId(orderId);
        Kot kot = kots.findByOrderId(orderId);
        Bill bill = bills.findByOrderId(orderId);
        if (kot == null) {
            throw new NotFoundException("No KOT for order " + orderId);
        }
        if (bill == null) {
            throw new NotFoundException("No bill for order " + orderId);
        }
        return OrderResponse.build(order, lines, kot, bill);
    }
}
