package com.example.stock.facade;

import com.example.stock.repository.RedisRepository;
import com.example.stock.service.StockService;
import org.springframework.stereotype.Component;

@Component
public class LettuceLockStockFacade {

    private final RedisRepository redisRepository;

    private final StockService stockService;

    public LettuceLockStockFacade(final RedisRepository redisRepository, final StockService stockService) {
        this.redisRepository = redisRepository;
        this.stockService = stockService;
    }

    public void decrease(final Long productId, final Long quantity) throws InterruptedException {
        while (!redisRepository.lock(productId)) {
            Thread.sleep(100); // 부하를 줄여줘본다.
        }

        try {
            stockService.decrease(productId, quantity);
        } finally {
            redisRepository.unlock(productId);
        }
    }
}
