package com.arogya.cafe.ordering.service;
import com.arogya.cafe.security.entity.*;
import com.arogya.cafe.catalog.entity.*;
import com.arogya.cafe.ordering.repository.*;
import com.arogya.cafe.ordering.entity.*;

import com.arogya.cafe.common.enums.KotStatus;
import com.arogya.cafe.common.exception.BusinessRuleException;
import com.arogya.cafe.common.exception.NotFoundException;
import com.arogya.cafe.inventory.entity.ConsumptionLine;
import com.arogya.cafe.inventory.service.StockService;
import com.arogya.cafe.ordering.dto.OrderingDtos.KotResponse;
import com.arogya.cafe.security.entity.Staff;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class KotService {

    private final KotRepository kots;
    private final OrderLineRepository orderLines;
    private final StockService stockService;

    public KotService(KotRepository kots, OrderLineRepository orderLines, StockService stockService) {
        this.kots = kots;
        this.orderLines = orderLines;
        this.stockService = stockService;
    }

    @Transactional(readOnly = true)
    public Kot getKot(Long id) {
        return kots.findById(id).orElseThrow(() -> new NotFoundException("KOT " + id + " not found"));
    }

    /**
     * Chef marks a KOT prepared. This is the trigger that auto-deducts ingredient stock:
     * every order line's recipe is consumed and logged as CONSUMED stock transactions.
     */
    public KotResponse markPrepared(Long kotId, Staff chef) {
        Kot kot = getKot(kotId);
        if (kot.getStatus() == KotStatus.PREPARED) {
            throw new BusinessRuleException("KOT " + kotId + " is already prepared");
        }
        Long orderId = kot.getOrder().getId();
        List<OrderLine> lines = orderLines.findByOrderId(orderId);
        if (lines.isEmpty()) {
            throw new BusinessRuleException("KOT " + kotId + " has no order lines to prepare");
        }

        kot.setStatus(KotStatus.PREPARED);
        kot.getFulfilledBy().add(chef);

        List<ConsumptionLine> consumption = lines.stream()
                .map(l -> new ConsumptionLine(l.getMenuItem().getId(), l.getSizeVariant(), l.getQuantity()))
                .toList();
        stockService.consumeForOrder(orderId, consumption, "ORDER #" + orderId + " prepared by " + chef.getName());

        return KotResponse.from(kot);
    }
}
