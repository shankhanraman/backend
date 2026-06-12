package com.arogya.cafe.inventory.repository;

import com.arogya.cafe.inventory.entity.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    List<StockTransaction> findByInventoryStockIdOrderByCreatedAtDesc(Long inventoryStockId);

    List<StockTransaction> findByOrderId(Long orderId);
}
