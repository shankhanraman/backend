package com.arogya.cafe.ordering.repository;

import com.arogya.cafe.ordering.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {}
