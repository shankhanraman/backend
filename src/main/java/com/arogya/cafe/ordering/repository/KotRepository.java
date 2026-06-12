package com.arogya.cafe.ordering.repository;

import com.arogya.cafe.ordering.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KotRepository extends JpaRepository<Kot, Long> {
    Kot findByOrderId(Long orderId);
}
