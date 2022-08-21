package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockNonSynchronizedService implements StockBusinessInterface {
    private final StockRepository stockRepository;

    public StockNonSynchronizedService(final StockRepository stockRepository) {
            this.stockRepository = stockRepository;
    }

    @Transactional
    public synchronized void decrease(final Long productId, final Long quantity) {
        final Stock stock = stockRepository.getByProductId(productId);
        stock.decrease(quantity);
    }
}
