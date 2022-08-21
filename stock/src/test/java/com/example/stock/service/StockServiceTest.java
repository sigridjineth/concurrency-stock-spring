package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StockServiceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private PessimisticLockStockService pessimisticLockStockService;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    public void before() {
        Stock stock = new Stock(1L, 100L);

        stockRepository.saveAndFlush(stock);
    }

    @AfterEach
    public void after() {
        stockRepository.deleteAll();
    }

    @Test
    public void 재고_수량이_감소한다() {
        // when
        stockService.decrease(1L, 1L);

        // then
        Stock stock = stockRepository.findById(1L).orElseThrow(
                () -> new IllegalArgumentException("Stock not found")
        );

        assertEquals(99L, stock.getQuantity());
    }

    /*
    ## CountDownLatch는 언제 쓸까?
    * 쓰레드를 N개 실행했을 때, 일정 개수의 쓰레드가 모두 끝날 때 까지 기다려야지만 다음으로 진행할 수 있거나
    * 다른 쓰레드를 실행시킬 수 있는 경우 사용한다.
    * 예를 들어 리스트에 어떤 자료구조가 있고, 각 자료구조를 병렬로 처리한 후 배치(batch)로
    * 데이터베이스를 업데이트 한다거나 다른 시스템으로 push하는 경우가 있다.

    ## CountDownLatch의 어떤 점이 이를 가능하게 하는가?
    * CountDownLatch를 초기화 할 때 정수값 count를 넣어준다.
    * 쓰레드는 마지막에서 countDown() 메서드를 불러준다.
    * 그러면 초기화 때 넣어준 정수값이 하나 내려간다.
    * 즉 각 쓰레드는 마지막에서 자신이 실행완료 했음을 countDown 메서드로 알려준다.
    * 이 쓰레드들이 끝나기를 기다리는 쪽 입장에서는 await()메서드를 불러준다.
    * 그러면 현재 메서드가 실행중이 메인쓰레드는 더이 상 진행하지않고 CountDownLatch의 count가 0이 될 때까지 기다린다.
    * 0이라는 정수값이 게이트(Latch)의 역할을 한다.
    * 카운트다운이 되면 게이트(latch)가 열리는 것이다.
     */
    @Test
    public void SYNCHRONIZED_동시에_100개의_쓰레드_요청한다() throws InterruptedException {
        // given
        int threadCount = 100;
        // 멀티 쓰레드 -> ExecutorService
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    stockService.decrease(1L, 1L);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        // https://stackoverflow.com/questions/41866691/what-is-the-purpose-of-await-in-countdownlatch
        // The one who waits for the action to be completed should call await() method.
        // This will wait indefinitely until all threads mark the work as processed,
        // by calling the countDown().
        countDownLatch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow(
                () -> new IllegalArgumentException("Stock not found")
        );

        // then: expected is zero
        assertEquals(0L, stock.getQuantity());
    }

    @Test
    public void PESSIMISTIC_LOCK_동시에_100개의_쓰레드_요청한다() throws InterruptedException {
        // given
        int threadCount = 100;
        // 멀티 쓰레드 -> ExecutorService
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pessimisticLockStockService.decrease(1L, 1L);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        // https://stackoverflow.com/questions/41866691/what-is-the-purpose-of-await-in-countdownlatch
        // The one who waits for the action to be completed should call await() method.
        // This will wait indefinitely until all threads mark the work as processed,
        // by calling the countDown().
        countDownLatch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow(
                () -> new IllegalArgumentException("Stock not found")
        );

        // then: expected is zero
        assertEquals(0L, stock.getQuantity());
    }
}