package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.PessimisticStockRepository;
import com.example.stock.repository.StockRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class PessimisticLockStockService implements StockBusinessInterface {

    private PessimisticStockRepository stockRepository;

    public PessimisticLockStockService(final PessimisticStockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional
    public void decrease(final Long id, final Long quantity) {
        Stock stock = stockRepository.getByProductId(id);

        stock.decrease(quantity);
    }
}
