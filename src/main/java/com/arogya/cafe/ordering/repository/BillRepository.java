package com.arogya.cafe.ordering.repository;
import com.arogya.cafe.ordering.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, Long> {
    Bill findByOrderId(Long orderId);
}
