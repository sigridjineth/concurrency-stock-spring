package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    private StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public void decrease(Long id, Long quantity) {
        // 1. get stock
        // 2. decrease stock
        // 3. save stock

        Stock stock = stockRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Stock not found")
        );

        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);
    }
}
