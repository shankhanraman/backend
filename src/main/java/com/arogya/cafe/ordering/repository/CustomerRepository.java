package com.arogya.cafe.ordering.repository;

import com.arogya.cafe.ordering.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {}
