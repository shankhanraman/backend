package com.arogya.cafe.ordering.controller;
import com.arogya.cafe.security.entity.*;
import com.arogya.cafe.ordering.service.*;

import com.arogya.cafe.ordering.dto.OrderingDtos.BillResponse;
import com.arogya.cafe.security.provider.CurrentStaffProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;
    private final CurrentStaffProvider currentStaff;

    public BillController(BillService billService, CurrentStaffProvider currentStaff) {
        this.billService = billService;
        this.currentStaff = currentStaff;
    }

    @GetMapping("/{id}")
    public BillResponse get(@PathVariable Long id) {
        return BillResponse.from(billService.getBill(id));
    }

    /** Only the cashier/manager records payment (which completes the order). Role gated in SecurityConfig. */
    @PostMapping("/{id}/pay")
    public BillResponse pay(@PathVariable Long id) {
        return billService.pay(id, currentStaff.require());
    }
}
