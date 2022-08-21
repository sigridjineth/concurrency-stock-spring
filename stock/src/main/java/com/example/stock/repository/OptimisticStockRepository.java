package com.example.stock.repository;

import com.example.stock.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import javax.persistence.LockModeType;

public interface OptimisticStockRepository extends JpaRepository<Stock, Long> {
    @Lock(LockModeType.OPTIMISTIC)
    Stock getByProductId(Long productId);
}
