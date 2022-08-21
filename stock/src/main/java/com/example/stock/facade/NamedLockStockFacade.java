package com.example.stock.facade;

import com.example.stock.repository.NamedLockRepository;
import com.example.stock.service.NamedLockStockService;
import org.springframework.stereotype.Component;

// lettuce lock
@Component
public class NamedLockStockFacade {

    private NamedLockRepository namedLockRepository;

    private NamedLockStockService namedLockStockService;

    public NamedLockStockFacade(final NamedLockRepository namedLockRepository, final NamedLockStockService namedLockStockService) {
        this.namedLockRepository = namedLockRepository;
        this.namedLockStockService = namedLockStockService;
    }

    public void decrease(Long id, Long quantity) {
        try {
            namedLockRepository.getLock(id.toString());
            namedLockStockService.decrease(id, quantity);
        } finally {
            namedLockRepository.releaseLock(id.toString());
        }
    }
}
