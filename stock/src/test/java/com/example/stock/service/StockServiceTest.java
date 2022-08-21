package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.facade.LettuceLockStockFacade;
import com.example.stock.facade.NamedLockStockFacade;
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

    @Autowired private NamedLockStockFacade namedLockStockFacade;

    @Autowired private LettuceLockStockFacade lettuceLockStockFacade;

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

    @DisplayName("optimistic lock을 사용한 재고 감소 - 동시에 1000개 테스트 | 36.494s 소요")
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

    @DisplayName("named lock 을 사용한 재고 감소 - 동시에 1000개 테스트 | 21.857s 소요")
    // 데이터 소스를 분리하지 않고 하나로 사용할 경우 커넥션 풀이 부족해질 수 있으므로 분리하는 것을 추천한다.
    @Test
    void NAMED_LOCK을_사용한_재고_감소() throws InterruptedException {
        // given

        // when
        IntStream.range(0, threadCount).forEach(e -> executorService.submit(() -> {
                    try {
                        namedLockStockFacade.decrease(productId, quantity);
                    } finally {
                        countDownLatch.countDown();
                    }
                }
        ));

        countDownLatch.await();

        // then
        final Long afterQuantity = stockRepository.getByProductId(productId).getQuantity();
        System.out.println("### NAMED LOCK 동시성 처리 이후 수량 ###" + afterQuantity);
        assertThat(afterQuantity).isZero();
    }

    @DisplayName("redis lettuce lock 을 사용한 재고 감소 - 동시에 1000개 테스트 | 49.581s")
    // Redis를 사용하면 트랜잭션에 따라 대응되는 현재 트랜잭션 풀 세션 관리를 하지 않아도 되므로 구현이 편리하다.
    // Spin Lock 방식이므로 부하를 줄 수 있어서 thread busy waiting을 통하여 요청 간의 시간을 주어야 한다.
    @Test
    void LETTUCE_LOCK을_사용한_재고_감소() throws InterruptedException {
        // given

        // when
        IntStream.range(0, threadCount).forEach(e -> executorService.submit(() -> {
                    try {
                        try {
                            lettuceLockStockFacade.decrease(productId, quantity);
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                }
        ));

        countDownLatch.await();

        // then
        final Long afterQuantity = stockRepository.getByProductId(productId).getQuantity();
        System.out.println("### LETTUCE LOCK 동시성 처리 이후 수량 ###" + afterQuantity);
        assertThat(afterQuantity).isZero();
    }
}