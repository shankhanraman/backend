package com.arogya.cafe.ordering.controller;
import com.arogya.cafe.security.entity.*;
import com.arogya.cafe.ordering.service.*;

import com.arogya.cafe.ordering.dto.OrderingDtos.KotResponse;
import com.arogya.cafe.security.provider.CurrentStaffProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kots")
public class KotController {

    private final KotService kotService;
    private final CurrentStaffProvider currentStaff;

    public KotController(KotService kotService, CurrentStaffProvider currentStaff) {
        this.kotService = kotService;
        this.currentStaff = currentStaff;
    }

    @GetMapping("/{id}")
    public KotResponse get(@PathVariable Long id) {
        return KotResponse.from(kotService.getKot(id));
    }

    /** Only a chef/manager marks a KOT prepared (which triggers stock deduction). Role gated in SecurityConfig. */
    @PostMapping("/{id}/prepare")
    public KotResponse prepare(@PathVariable Long id) {
        return kotService.markPrepared(id, currentStaff.require());
    }
}
