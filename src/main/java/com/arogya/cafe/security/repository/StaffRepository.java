package com.arogya.cafe.security.repository;
import com.arogya.cafe.security.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Staff findByUsername(String username);

    boolean existsByUsername(String username);
}
