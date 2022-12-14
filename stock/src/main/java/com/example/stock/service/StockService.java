package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class StockService implements StockBusinessInterface {

    private StockRepository stockRepository;

    public StockService(final StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    /*
    만약 스프링의 @Transactional 어노테이션을 사용한다면?

    // TransactionStockService와 같이 실행할 때 wrapping해서 새로 실행한다.
    // 트랜잭션 종료 시점에 commit하는데 여기서 발생한다.
    // decrease method 담긴 thread가 트랜잭션 종료 전에 DB에 접근하여 race condition이 일어날 수 있다.

    // 그렇다면 왜 @Transactional 어노테이션을 사용하는가?
    // https://okky.kr/article/437870
    // @Transactional을 꼭 사용할 필요는 없습니다.
    // 물론 직접 if문 내지는 try catch문 사용해서 관리해도 된다.
    // catch(Exception e) 잡아서 그냥 직접 rollback하면 된다.
    // 그런데 프로젝트가 커지고 DAO, Service가 많으면 많아질수록 중복되는 if, try catch 코드가 점점 많아지겠다.
    // 그래서 중복되는 부분은 한군데서 관리하고싶다는 요구사항이 발생했을 것이다.
    // 이 때 @Transactional 어노테이션을 붙이면 그 try catch 코드들을 직접 짤 필요 없이 알아서 자동으로 붙여주기 때문에
    // 중복되는 코드가 줄어들고 보기 쉬워지죠. 이러한 이유에서 @Transactional을 사용한다.
    */
    //@Transactional
    public synchronized void decrease(final Long id, final Long quantity) {
        // 1. get stock
        // 2. decrease stock
        // 3. save stock

        final Stock stock = stockRepository.getByProductId(id);
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }
}
