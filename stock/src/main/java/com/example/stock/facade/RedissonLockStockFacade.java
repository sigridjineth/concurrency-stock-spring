package com.example.stock.facade;

import com.example.stock.service.StockNonSynchronizedService;
import com.example.stock.service.StockService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedissonLockStockFacade {

    private final RedissonClient redissonClient;

    private final StockNonSynchronizedService stockService;

    public RedissonLockStockFacade(final RedissonClient redissonClient, final StockNonSynchronizedService stockService) {
        this.redissonClient = redissonClient;
        this.stockService = stockService;
    }

    public void decrease(final Long productId, final Long quantity) throws InterruptedException {
        final RLock lock = redissonClient.getLock(productId.toString());

        try {
//            boolean isAvailable = lock.tryLock(10, 1, TimeUnit.SECONDS);

            if (!lock.tryLock(30, 1, TimeUnit.SECONDS)) {
                System.out.println("redisson getLock timeout");
                return;
            }

            stockService.decrease(productId, quantity);

        } finally {
            // unlock the lock object
            lock.unlock();
        }
    }
}
