package com.arogya.cafe.ordering.repository;
import com.arogya.cafe.ordering.entity.*;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
    List<OrderLine> findByOrderId(Long orderId);
}
