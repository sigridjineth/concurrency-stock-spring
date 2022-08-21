package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.OptimisticStockRepository;
import com.example.stock.repository.StockRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class OptimisticLockStockService implements StockBusinessInterface {

    private final OptimisticStockRepository stockRepository;

    public OptimisticLockStockService(final OptimisticStockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional
    public void decrease(final Long id, final Long quantity) {
        final Stock stock = stockRepository.getByProductId(id);
        stock.decrease(quantity);
    }
}
