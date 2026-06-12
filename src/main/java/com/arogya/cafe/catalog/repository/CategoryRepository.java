package com.arogya.cafe.catalog.repository;

import com.arogya.cafe.catalog.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {}
