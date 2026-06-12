package com.arogya.cafe.ordering.service;

import com.arogya.cafe.catalog.entity.*;
import com.arogya.cafe.common.enums.OrderStatus;
import com.arogya.cafe.common.enums.PaymentStatus;
import com.arogya.cafe.common.exception.BusinessRuleException;
import com.arogya.cafe.common.exception.NotFoundException;
import com.arogya.cafe.ordering.dto.OrderingDtos.BillResponse;
import com.arogya.cafe.ordering.entity.*;
import com.arogya.cafe.ordering.repository.*;
import com.arogya.cafe.security.entity.*;
import com.arogya.cafe.security.entity.Staff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BillService {

    private final BillRepository bills;

    public BillService(BillRepository bills) {
        this.bills = bills;
    }

    @Transactional(readOnly = true)
    public Bill getBill(Long id) {
        return bills.findById(id).orElseThrow(() -> new NotFoundException("Bill " + id + " not found"));
    }

    /**
     * Cashier records payment. Marks the bill PAID and completes the order.
     */
    public BillResponse pay(Long billId, Staff cashier) {
        Bill bill = getBill(billId);
        if (bill.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessRuleException("Bill " + billId + " is already paid");
        }
        bill.setPaymentStatus(PaymentStatus.PAID);
        bill.getOrder().setStatus(OrderStatus.COMPLETED);
        bill.getOrder().getHandledBy().add(cashier);
        return BillResponse.from(bill);
    }
}
