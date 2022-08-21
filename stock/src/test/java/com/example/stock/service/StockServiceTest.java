package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.facade.OptimisticLockStockFacade;
import com.example.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StockServiceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private PessimisticLockStockService pessimisticLockStockService;

    @Autowired private OptimisticLockStockFacade stockOptimisticLockFacade;

    @Autowired private StockRepository stockRepository;

    private final int threadCount = 1000;
    private final long productId = 1000L;
    private final long quantity = 1L;
    private final long initQuantity = 1000L;

    private ExecutorService executorService;
    private CountDownLatch countDownLatch;

    @BeforeEach
    public void beforeEach() {
        stockRepository.save(new Stock(productId, initQuantity));

        executorService = Executors.newFixedThreadPool(threadCount);
        countDownLatch = new CountDownLatch(threadCount);
    }

    @AfterEach
    public void afterEach() {
        stockRepository.deleteAll();
    }

    @DisplayName("SYNCHRONIZED를 사용한 재고 감소 - 동시 1000개 테스트 | 16.994s 소요")
    @Test
    void SYNCHRONIZED를_사용한_재고_감소() throws InterruptedException {
        // given

        // when
        IntStream.range(0, threadCount).forEach(e -> executorService.submit(() -> {
                    try {
                        stockService.decrease(productId, quantity);
                    } finally {
                        countDownLatch.countDown();
                    }
                }
        ));

        countDownLatch.await();

        // then
        final Long afterQuantity = stockRepository.getByProductId(productId).getQuantity();
        System.out.println("### SYNCHRONIZED 동시성 처리 이후 수량 ###" + afterQuantity);
        assertThat(afterQuantity).isZero();
    }

    @DisplayName("pessimistic lock을 사용한 재고 감소 - 동시에 1000개 테스트 | 12.415s 소요")
    @Test
    void PESSIMISTIC_LOCK을_사용한_재고_감소() throws InterruptedException {
        // given

        // when
        IntStream.range(0, threadCount).forEach(e -> executorService.submit(() -> {
                    try {
                        pessimisticLockStockService.decrease(productId, quantity);
                    } finally {
                        countDownLatch.countDown();
                    }
                }
        ));

        countDownLatch.await();

        // then
        final Long afterQuantity = stockRepository.getByProductId(productId).getQuantity();
        System.out.println("### PESSIMISTIC LOCK 동시성 처리 이후 수량 ###" + afterQuantity);
        assertThat(afterQuantity).isZero();
    }

    @DisplayName("optimistic lock을 사용한 재고 감소 - 동시에 1000개 테스트 | 36.494s")
    // 충돌이 빈번하게 일어나지 않을 것이라면 Optimistic Lock을 사용한다.
    @Test
    void OPTIMISTIC_LOCK을_사용한_재고_감소() throws InterruptedException {
        // given

        // when
        IntStream.range(0, threadCount).forEach(e -> executorService.submit(() -> {
                    try {
                        stockOptimisticLockFacade.decrease(productId, quantity);
                    } catch (final InterruptedException ex) {
                        throw new RuntimeException(ex);
                    } finally {
                        countDownLatch.countDown();
                    }
                }
        ));

        countDownLatch.await();

        // then
        final Long afterQuantity = stockRepository.getByProductId(productId).getQuantity();
        System.out.println("### OPTIMISTIC LOCK 동시성 처리 이후 수량 ###" + afterQuantity);
        assertThat(afterQuantity).isZero();
    }
}